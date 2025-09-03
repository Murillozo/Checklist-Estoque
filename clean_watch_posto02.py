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
import re
import time
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional

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
ORDER = "prefix"

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


def dedupe(seq: Iterable[str]) -> List[str]:
    """Remove duplicates preserving order."""

    seen = set()
    out: List[str] = []
    for item in seq:
        if item not in seen:
            seen.add(item)
            out.append(item)
    return out


PREFIX_RE = re.compile(r"^(\d+(?:\.\d+)+)\s*-")

# ordem de subtópicos dentro do mesmo prefixo (quanto menor, mais alto)
SUBORDER = [
    "identificação do projeto",
    "separação - posto -",
    "referências x projeto",
    "material em bom estado",
]


def parse_prefix_tuple(pergunta: str) -> tuple:
    """Extrai prefixo numérico e converte para tupla de inteiros."""

    m = PREFIX_RE.match(pergunta.strip().lower())
    if not m:
        return tuple()
    parts = m.group(1).split(".")
    try:
        return tuple(int(p) for p in parts)
    except Exception:
        # fallback robusto
        return tuple()


def subtopic_rank(pergunta: str) -> int:
    """Prioridade do sufixo textual da pergunta dentro do prefixo."""

    text = pergunta.strip().lower()
    for idx, token in enumerate(SUBORDER):
        if token in text:
            return idx
    return 9


def normalize_responses(value: Any) -> List[str]:
    """Normalize a response field into a list of cleaned strings."""

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
        if s and s.upper() != "NA":
            cleaned.append(s)
    return dedupe(cleaned)


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


def clean_item(raw_item: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """Normalise an individual item from the checklist.

    Responses for each function are converted to lists where the last element
    represents the most recent answer for that function.
    """

    try:
        numero = int(raw_item.get("numero"))
        pergunta = str(raw_item.get("pergunta", "")).strip()
        respostas = raw_item.get("respostas", {})
    except Exception:
        return None

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
    """Merge items that have the same question.

    Histories from duplicated questions are concatenated in the order of
    appearance, removing duplicates while preserving chronology.  The most
    recent answer for each function is therefore the last element of the
    combined history list.
    """

    grouped: Dict[str, Dict[str, Any]] = {}
    order: List[str] = []
    for item in items:
        q = item["pergunta"]
        if q not in grouped:
            grouped[q] = {
                "numero": item["numero"],
                "pergunta": q,
                "respostas": {f: list(item["respostas"][f]) for f in FUNCS},
            }
            order.append(q)
        else:
            g = grouped[q]
            g["numero"] = max(g["numero"], item["numero"])
            for f in FUNCS:
                g["respostas"][f].extend(item["respostas"][f])

    result: List[Dict[str, Any]] = []
    for q in order:
        g = grouped[q]
        for f in FUNCS:
            g["respostas"][f] = dedupe(g["respostas"][f])
        result.append(g)
    return result


def build_output(raw: Dict[str, Any], items: List[Dict[str, Any]], order: str = "prefix") -> Dict[str, Any]:
    """Build the final JSON structure.

    Each item consolidates duplicate questions and exposes the full history of
    respostas for both ``suprimento`` and ``produção`` as lists.
    """

    resp_raw = raw.get("respondentes") or {}
    respondentes: Dict[str, str] = {}
    if isinstance(resp_raw, dict):
        for k, v in resp_raw.items():
            respondentes[str(k)] = str(v or "")

    mat_raw = raw.get("materiais") or []
    materiais: List[Any] = []
    if isinstance(mat_raw, list):
        for m in mat_raw:
            if m is not None:
                materiais.append(m)

    out: Dict[str, Any] = {
        "obra": str(raw.get("obra") or ""),
        "ano": str(raw.get("ano") or ""),
        "respondentes": respondentes,
        "materiais": materiais,
        "itens": [],
    }

    cleaned_items: List[Dict[str, Any]] = []
    for item in items:
        sup_hist = item["respostas"].get("suprimento") or []
        prod_hist = item["respostas"].get("produção") or []
        res_out: Dict[str, Any] = {
            "suprimento": sup_hist,
            "produção": prod_hist,
        }
        cleaned_items.append(
            {
                "numero": item["numero"],
                "pergunta": item["pergunta"],
                "respostas": res_out,
            }
        )

    def sort_key_prefix(it: Dict[str, Any]):
        prefix_tuple = parse_prefix_tuple(it["pergunta"])
        sub_rank = subtopic_rank(it["pergunta"])
        return (prefix_tuple or (9999,), sub_rank, it["pergunta"].lower())

    def sort_key_recency(it: Dict[str, Any]):
        has_atual = bool(it["respostas"]["suprimento"] or it["respostas"]["produção"])
        if has_atual:
            return (0, -it["numero"])
        return (1, it["pergunta"])

    sort_key = sort_key_prefix if order == "prefix" else sort_key_recency
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
    """Write a summary CSV exposing the latest responses.

    The last element of each list is treated as the "atual" value for that
    função.
    """

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["numero", "pergunta", "suprimento_atual", "producao_atual"])
    for item in data["itens"]:
        sup_hist = item["respostas"]["suprimento"]
        prod_hist = item["respostas"]["produção"]
        writer.writerow([
            item["numero"],
            item["pergunta"],
            sup_hist[-1] if sup_hist else "",
            prod_hist[-1] if prod_hist else "",
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
        or name.endswith("_resumo.csv")
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
    final = build_output(raw, merged, ORDER)

    json_payload = json.dumps(final, ensure_ascii=False, indent=2).encode("utf-8")
    json_changed = write_if_changed(OUT_DIR / f"{path.stem}_clean.json", json_payload)
    csv_changed = write_summary_csv(final, OUT_DIR / f"{path.stem}_resumo.csv")

    if json_changed or csv_changed:
        logger.info("limpo com sucesso: %s", path.name)
    else:
        logger.info("sem alterações: %s", path.name)


def process_existing_files() -> None:
    """Process all JSON files already present in ``WATCH_DIR``."""

    for path in WATCH_DIR.glob("*.json"):
        if is_temp_file(path):
            continue
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
            if path.parent != WATCH_DIR or path.suffix.lower() != ".json" or is_temp_file(path):
                return
            try:
                process_file(path)
            except Exception as exc:
                logger.error("erro ao processar %s: %s", path, exc)

    observer = Observer()
    observer.schedule(Handler(), str(WATCH_DIR), recursive=False)
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
            for path in WATCH_DIR.glob("*.json"):
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

def run(once: bool = False, order: str = "prefix") -> None:
    """Execute the cleaning workflow.

    Parameters
    ----------
    once:
        When ``True`` the current backlog of files is processed and the
        function returns immediately.  Otherwise a file watcher is started and
        this call blocks until interrupted.
    order:
        Sorting strategy for items: ``"prefix"`` (default) or ``"recency"``.
    """

    global ORDER
    ORDER = order

    setup_logging()
    WATCH_DIR.mkdir(parents=True, exist_ok=True)
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
    parser.add_argument("--order", choices=["prefix", "recency"], default="prefix", help="ordenação dos itens")
    args = parser.parse_args()
    run(once=args.once, order=args.order)


if __name__ == "__main__":
    main()
