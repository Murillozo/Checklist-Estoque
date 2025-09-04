#!/usr/bin/env python3
"""Monitor and clean checklist JSON files for Posto02_Oficina.

The script watches ``site/json_api/Posto02_Oficina`` for JSON files and writes
``*_clean.json`` and ``*_resumo.csv`` files back into the same directory.
Run with ``python clean_watch_posto02.py`` to watch continuously or
``python clean_watch_posto02.py --once`` to process the current backlog and
exit.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import logging
import time
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Any, Dict, List, Optional

# ---------------------------------------------------------------------------
# Optional watchdog import.  If unavailable we will fallback to simple polling.
try:  # pragma: no cover - optional dependency
    from watchdog.events import FileSystemEventHandler
    from watchdog.observers import Observer

    WATCHDOG_AVAILABLE = True
except Exception:  # pragma: no cover - watchdog may be absent
    WATCHDOG_AVAILABLE = False


BASE_DIR = Path(__file__).resolve().parent
WATCH_DIR = BASE_DIR / "site" / "json_api" / "Posto02_Oficina"
OUT_DIR = WATCH_DIR
LOG_FILE = OUT_DIR / "clean_watch.log"
FUNCS = ("suprimento", "produção")
BASE_CHECKLIST = WATCH_DIR.parent / "checklist_PRO1000.json"
ANCHOR_PERGUNTA = "1.1 - INVÓLUCRO - CAIXA: Identificação do projeto"

# Map of numero -> pergunta from the base checklist.  Populated at runtime
BASE_QUESTIONS: Dict[int, str] = {}

logger = logging.getLogger("clean_watch")


# ---------------------------------------------------------------------------
# Utility helpers

def setup_logging() -> None:
    """Configure logging to console and rotating file."""

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    logger.setLevel(logging.INFO)
    fmt = logging.Formatter("%(asctime)s %(levelname)s %(message)s")

    ch = logging.StreamHandler()
    ch.setFormatter(fmt)
    logger.addHandler(ch)

    fh = RotatingFileHandler(LOG_FILE, maxBytes=1_000_000, backupCount=3, encoding="utf-8")
    fh.setFormatter(fmt)
    logger.addHandler(fh)


def normalize_responses(value: Any) -> List[str]:
    """Normalize a response field into a list of cleaned strings, preserving order."""

    items: List[str] = []
    if value is None:
        items = []
    elif isinstance(value, str):
        items = [value]
    elif isinstance(value, list):
        items = [v for v in value if isinstance(v, str)]
    cleaned: List[str] = []
    for v in items:
        s = v.strip()
        if s and s.upper() not in {"NA", "NULL"}:
            cleaned.append(s)
    return cleaned


# ---------------------------------------------------------------------------
# Core pure functions

def load_json(path: Path) -> Optional[Dict[str, Any]]:
    """Load a JSON file using UTF-8 encoding.

    Returns ``None`` and logs an error if the file cannot be parsed.
    """

    try:
        with path.open("r", encoding="utf-8") as fh:
            return json.load(fh)
    except Exception as exc:  # pragma: no cover - I/O errors
        logger.error("erro ao ler JSON inválido %s: %s", path, exc)
        return None


def load_base_questions() -> None:
    """Populate ``BASE_QUESTIONS`` from ``checklist_PRO1000.json`` if present."""

    global BASE_QUESTIONS
    data = load_json(BASE_CHECKLIST)
    mapping: Dict[int, str] = {}
    if data:
        for item in data.get("itens", []):
            try:
                numero = int(item.get("numero"))
                pergunta = str(item.get("pergunta", "")).strip()
            except Exception:
                continue
            if pergunta:
                mapping[numero] = pergunta
    if not mapping:
        logger.warning("base checklist não encontrada ou inválida: %s", BASE_CHECKLIST)
    BASE_QUESTIONS = mapping


def clean_item(raw_item: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """Normalise an individual item from the checklist.

    Responses for each function are converted to lists where the last element
    represents the most recent answer for that function.
    """

    try:
        numero = int(raw_item.get("numero"))
        pergunta_raw = str(raw_item.get("pergunta", "")).strip()
        respostas = raw_item.get("respostas", {})
    except Exception:
        return None

    pergunta = BASE_QUESTIONS.get(numero, pergunta_raw)
    if not pergunta:
        return None
    if not isinstance(respostas, dict):
        respostas = {}

    norm: Dict[str, List[str]] = {}
    for key, value in respostas.items():
        lower = key.lower()
        if lower.startswith("sup"):
            norm_key = "suprimento"
        elif lower.startswith("prod"):
            norm_key = "produção"
        else:
            continue
        norm[norm_key] = normalize_responses(value)

    for func in FUNCS:
        norm.setdefault(func, [])

    return {"numero": numero, "pergunta": pergunta, "respostas": norm}


def merge_duplicates(items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Merge items that have the same ``numero``.

    Histories from duplicated questions are concatenated in the order of
    appearance.  The most recent answer for each function is therefore the
    last element of the combined history list.
    """

    grouped: Dict[int, Dict[str, Any]] = {}
    order: List[int] = []
    for item in items:
        n = item["numero"]
        if n not in grouped:
            grouped[n] = {
                "numero": n,
                "pergunta": item["pergunta"],
                "respostas": {f: list(item["respostas"][f]) for f in FUNCS},
            }
            order.append(n)
        else:
            g = grouped[n]
            for f in FUNCS:
                g["respostas"][f].extend(item["respostas"][f])

    result: List[Dict[str, Any]] = []
    for n in order:
        result.append(grouped[n])
    return result


def slice_from_anchor(items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    idx = next((i for i, it in enumerate(items) if it.get("pergunta") == ANCHOR_PERGUNTA), 0)
    return items[idx:]


def build_output(raw: Dict[str, Any], items: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Build the final JSON structure.

    Each item consolidates duplicate questions and retains only the most
    recent resposta for cada função; when ausente, a string vazia é usada.
    """

    out: Dict[str, Any] = {
        "obra": str(raw.get("obra", "")),
        "ano": str(raw.get("ano", "")),
        "respondentes": raw.get("respondentes", {}),
        "materiais": raw.get("materiais", []),
        "itens": [],
    }

    cleaned_items: List[Dict[str, Any]] = []
    for item in items:
        sup_hist = item["respostas"]["suprimento"]
        prod_hist = item["respostas"]["produção"]

        respostas = {
            "suprimento": sup_hist[-1] if sup_hist else "",
            "produção": prod_hist[-1] if prod_hist else "",
        }

        cleaned_items.append(
            {
                "numero": item["numero"],
                "pergunta": item["pergunta"],
                "respostas": respostas,
            }
        )

    def sort_key(it: Dict[str, Any]):
        res = it["respostas"]
        has_atual = bool(res.get("suprimento") or res.get("produção"))
        if has_atual:
            return (0, -it["numero"])
        return (1, it["pergunta"])

    cleaned_items.sort(key=sort_key)
    out["itens"] = cleaned_items
    return out


def write_if_changed(path: Path, payload: bytes) -> bool:
    """Write ``payload`` to ``path`` if contents differ.

    Comparison is done via SHA-256 hash.  Returns ``True`` when the file was
    written, ``False`` if the existing file already contained the same data.
    """

    if path.exists():
        current = path.read_bytes()
        if hashlib.sha256(current).digest() == hashlib.sha256(payload).digest():
            return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(payload)
    return True


def write_summary_csv(data: Dict[str, Any], path: Path) -> bool:
    """Write the CSV summary and return ``True`` if the file changed."""

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["numero", "pergunta", "suprimento_atual", "producao_atual"])
    for item in data["itens"]:
        respostas = item["respostas"]
        writer.writerow([
            item["numero"],
            item["pergunta"],
            respostas.get("suprimento", ""),
            respostas.get("produção", ""),
        ])
    return write_if_changed(path, output.getvalue().encode("utf-8"))


# ---------------------------------------------------------------------------
# File processing

def wait_for_stable_size(path: Path, timeout: float = 5.0) -> bool:
    """Wait until ``path`` size is stable for 500ms.

    Returns ``True`` if stable, ``False`` if the file disappears or the timeout
    is reached.
    """

    deadline = time.time() + timeout
    try:
        last = path.stat().st_size
    except FileNotFoundError:
        return False
    while time.time() < deadline:
        time.sleep(0.5)
        try:
            size = path.stat().st_size
        except FileNotFoundError:
            return False
        if size == last:
            return True
        last = size
    return False


def is_temp_file(path: Path) -> bool:
    """Return ``True`` for temporary or partial files."""

    name = path.name
    return (
        name.startswith("~$")
        or name.endswith(".~")
        or name.endswith("_clean.json")
        or name.endswith("_resumo.json")
    )


def process_file(path: Path) -> None:
    """Clean and consolidate ``path`` producing JSON and CSV outputs."""

    if not wait_for_stable_size(path):
        return
    logger.info("arquivo detectado: %s", path.name)
    raw = load_json(path)
    if raw is None:
        return
    itens_raw = raw.get("itens")
    if not isinstance(itens_raw, list):
        logger.error("JSON inválido (itens) %s", path)
        return

    cleaned: List[Dict[str, Any]] = []
    for r in itens_raw:
        if isinstance(r, dict):
            item = clean_item(r)
            if item:
                cleaned.append(item)

    merged = merge_duplicates(cleaned)
    merged = slice_from_anchor(merged)
    final = build_output(raw, merged)

    json_payload = json.dumps(final, ensure_ascii=False, indent=2).encode("utf-8")
    json_changed = write_if_changed(OUT_DIR / f"{path.stem}_clean.json", json_payload)
    csv_changed = write_summary_csv(final, OUT_DIR / f"{path.stem}_resumo.csv")

    if json_changed or csv_changed:
        logger.info("limpo com sucesso: %s", path.name)
    else:
        logger.info("sem alterações: %s", path.name)


def process_existing_files() -> None:
    """Process all JSON files already present in ``WATCH_DIR``."""

    for path in WATCH_DIR.rglob("*.json"):
        if path.is_file() and not is_temp_file(path):
            try:
                process_file(path)
            except Exception as exc:  # pragma: no cover - robust against errors
                logger.error("erro ao processar %s: %s", path, exc)


# ---------------------------------------------------------------------------
# Watch implementations

def start_watchdog() -> None:  # pragma: no cover - requires watchdog
    class Handler(FileSystemEventHandler):
        def on_created(self, event):
            self.handle(event)

        def on_modified(self, event):
            self.handle(event)

        def handle(self, event):
            if event.is_directory:
                return
            path = Path(event.src_path)
            if path.suffix.lower() != ".json" or is_temp_file(path):
                return
            try:
                process_file(path)
            except Exception as exc:
                logger.error("erro ao processar %s: %s", path, exc)

    observer = Observer()
    observer.schedule(Handler(), str(WATCH_DIR), recursive=True)
    observer.start()
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:  # pragma: no cover - interactive exit
        observer.stop()
    observer.join()


def start_polling() -> None:
    """Fallback watcher based on polling every 5 seconds."""

    logger.warning("watchdog indisponível, usando polling")
    known_mtimes: Dict[Path, float] = {}
    try:
        while True:
            for path in WATCH_DIR.rglob("*.json"):
                if is_temp_file(path):
                    continue
                try:
                    mtime = path.stat().st_mtime
                except FileNotFoundError:
                    continue
                last = known_mtimes.get(path)
                if last is None or mtime != last:
                    known_mtimes[path] = mtime
                    try:
                        process_file(path)
                    except Exception as exc:  # pragma: no cover
                        logger.error("erro ao processar %s: %s", path, exc)
            time.sleep(5)
    except KeyboardInterrupt:  # pragma: no cover
        pass


# ---------------------------------------------------------------------------
# Public helpers

def run(once: bool = False) -> None:
    """Execute the cleaning workflow.

    Parameters
    ----------
    once:
        When ``True`` the current backlog of files is processed and the
        function returns immediately.  Otherwise a file watcher is started and
        this call blocks until interrupted.
    """

    setup_logging()
    WATCH_DIR.mkdir(parents=True, exist_ok=True)
    load_base_questions()
    process_existing_files()
    if once:
        return

    if WATCHDOG_AVAILABLE:
        start_watchdog()
    else:
        start_polling()


# ---------------------------------------------------------------------------
# CLI entry point

def main() -> None:
    parser = argparse.ArgumentParser(description="Limpa e observa checklist JSONs")
    parser.add_argument("--once", action="store_true", help="processa backlog e sai")
    args = parser.parse_args()
    run(once=args.once)


if __name__ == "__main__":
    main()
