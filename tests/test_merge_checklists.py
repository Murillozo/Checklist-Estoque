"""Tests for merge_checklists helpers.

These tests exercise the behavior of ``find_mismatches`` and
``move_matching_checklists`` when the production checklist contains
additional annotations beyond those found in the supply checklist.
"""

from __future__ import annotations

import importlib.util
import json
import pathlib


MODULE_PATH = pathlib.Path(__file__).resolve().parents[1] / "site" / "json_api" / "merge_checklists.py"
spec = importlib.util.spec_from_file_location("merge_checklists", MODULE_PATH)
merge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(merge)  # type: ignore[misc]

find_mismatches = merge.find_mismatches
move_matching_checklists = merge.move_matching_checklists


def _write_checklist(path: pathlib.Path, sup: list[str], prod: list[str]) -> None:
    data = {
        "obra": "OBRA1",
        "ano": "2024",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "respostas": {"suprimento": sup, "produção": prod},
            }
        ],
    }
    with open(path, "w", encoding="utf-8") as fp:
        json.dump(data, fp, ensure_ascii=False)


def test_find_mismatches_ignores_additional_production_annotations(tmp_path: pathlib.Path) -> None:
    path = tmp_path / "checklist_OBRA1.json"
    _write_checklist(path, ["C", "Joao"], ["C", "Joao", "Maria"])

    assert find_mismatches(str(tmp_path)) == []


def test_move_matching_preserves_extra_annotations(tmp_path: pathlib.Path) -> None:
    src_dir = tmp_path / "Posto01_Oficina"
    src_dir.mkdir()
    checklist_path = src_dir / "checklist_OBRA1.json"
    _write_checklist(checklist_path, ["C", "Joao"], ["C", "Joao", "Maria"])

    moved = move_matching_checklists(str(tmp_path))
    assert moved == ["checklist_OBRA1.json"]

    dest_path = tmp_path / "Posto02_Oficina" / "checklist_OBRA1.json"
    with open(dest_path, "r", encoding="utf-8") as fp:
        data = json.load(fp)

    assert data["itens"][0]["respostas"]["produção"] == ["C", "Joao", "Maria"]
    assert find_mismatches(str(tmp_path / "Posto02_Oficina")) == []

