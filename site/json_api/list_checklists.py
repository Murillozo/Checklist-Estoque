import os
import re
import json
from typing import Dict, List, Any

BASE_DIR = os.path.dirname(__file__)
FOLDERS = [
    "Posto01_Oficina",
    "Posto02_Oficina",
    "Posto02_Oficina_Inspetor",
    "Posto03_Pre_montagem_01",
    "Posto03_Pre_montagem_01_Inspetor",
    "POSTO_04_BARRAMENTO",
    "POSTO_04_BARRAMENTO_Inspetor",
    "Posto05_cablagem_01",
    "Posto05_cablagem_01_inspetor",
    "Posto06_Pre_montagem_02",
    "Posto06_Pre_montagem_02_inspetor",
    "POSTO06_1_06Cablagem02",
    "POSTO06_1_06Cablagem02_inspetor",
    "posto08_IQM",
    "posto08_IQE",
    "POSTO08_TESTE",
    "EXPEDICAO",
    "CHECKLIST_FINAL",
]


def humanize_folder(name: str) -> str:
    """Return a human friendly representation of ``name``.

    Examples
    --------
    >>> humanize_folder("Posto01_Oficina")
    'Posto 01 Oficina'
    >>> humanize_folder("POSTO08_TESTE")
    'Posto 08 Teste'
    """
    text = name.replace("_", " ")

    def repl(match: re.Match) -> str:
        return f"Posto {int(match.group(1)):02d}"

    text = re.sub(r"posto\s*(\d+)", repl, text, flags=re.I)
    text = text.title()
    # Preserve acronyms in upper-case
    for acr in ("IQM", "IQE"):
        text = text.replace(acr.title(), acr)
    return text

def collect_checklists() -> Dict[str, List[Dict[str, Any]]]:
    """Return a mapping of folder names to their JSON checklist contents.

    Any directory that does not exist is skipped. Each entry contains the
    filename and the parsed JSON data. Files that cannot be decoded as JSON are
    ignored. The function is useful for debugging or quick inspection of the
    pipeline.
    """
    result: Dict[str, List[Dict[str, Any]]] = {}
    for folder in FOLDERS:
        path = os.path.join(BASE_DIR, folder)
        if not os.path.isdir(path):
            continue
        entries: List[Dict[str, Any]] = []
        for fname in os.listdir(path):
            if not fname.endswith(".json"):
                continue
            fpath = os.path.join(path, fname)
            try:
                with open(fpath, "r", encoding="utf-8") as fp:
                    data = json.load(fp)
            except Exception:
                continue
            entries.append({"file": fname, "data": data})
        result[folder] = entries
    return result


def main() -> None:
    """Command line helper that prints discovered checklists."""
    import argparse
    parser = argparse.ArgumentParser(description="List checklist JSON files")
    parser.add_argument(
        "--show", action="store_true", help="Print JSON content instead of just filenames"
    )
    args = parser.parse_args()
    data = collect_checklists()
    for folder, items in data.items():
        print(folder + ":")
        for item in items:
            if args.show:
                print(json.dumps(item["data"], ensure_ascii=False, indent=2))
            else:
                print(f"  - {item['file']}")


if __name__ == "__main__":
    main()
