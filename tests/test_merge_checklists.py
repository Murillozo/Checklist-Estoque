"""Tests for merge_checklists helpers.

These tests exercise the behavior of ``find_mismatches`` and
``move_matching_checklists`` when the production checklist contains
additional annotations beyond those found in the supply checklist.
"""

from __future__ import annotations

import importlib
import importlib.util
import json
import pathlib
import sys
from flask import Flask


MODULE_PATH = pathlib.Path(__file__).resolve().parents[1] / "site" / "json_api" / "merge_checklists.py"
spec = importlib.util.spec_from_file_location("merge_checklists", MODULE_PATH)
merge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(merge)  # type: ignore[misc]

find_mismatches = merge.find_mismatches
move_matching_checklists = merge.move_matching_checklists

# Load API module for posto inspector uploads
SITE_DIR = pathlib.Path(__file__).resolve().parents[1] / "site"
sys.path.insert(0, str(SITE_DIR))
api = importlib.import_module("json_api")


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


def test_posto02_inspector_allows_extra_annotations(tmp_path: pathlib.Path) -> None:
    api.BASE_DIR = str(tmp_path)
    insp_dir = tmp_path / "Posto02_Oficina" / "Posto02_Oficina_Inspetor"
    insp_dir.mkdir(parents=True)

    base_data = {
        "obra": "OBRA1",
        "ano": "2024",
        "posto02": {
            "itens": [
                {
                    "numero": 1,
                    "pergunta": "Pergunta",
                    "respostas": {"produção": ["C", "Joao"]},
                }
            ]
        },
    }
    with open(insp_dir / "checklist_OBRA1.json", "w", encoding="utf-8") as fp:
        json.dump(base_data, fp, ensure_ascii=False)

    app = Flask(__name__)
    app.register_blueprint(api.bp)

    payload = {
        "obra": "OBRA1",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "resposta": ["C", "Joao", "Maria"],
            }
        ],
        "inspetor": "Inspector",
    }
    with app.test_client() as client:
        res = client.post("/posto02/insp/upload", json=payload)
        assert res.status_code == 200
        body = res.get_json()

    assert body["divergencias"] == []
    dest = pathlib.Path(body["caminho"])
    assert dest.parent.name == "Posto03_Pre_montagem_01"
    with open(dest, "r", encoding="utf-8") as fp:
        saved = json.load(fp)
    item = saved["posto02"]["itens"][0]
    assert item["respostas"]["produção"] == ["C", "Joao"]
    assert item["respostas"]["inspetor"] == ["C", "Joao", "Maria"]


def test_posto02_inspector_can_skip_names(tmp_path: pathlib.Path) -> None:
    api.BASE_DIR = str(tmp_path)
    insp_dir = tmp_path / "Posto02_Oficina" / "Posto02_Oficina_Inspetor"
    insp_dir.mkdir(parents=True)

    base_data = {
        "obra": "OBRA1",
        "ano": "2024",
        "posto02": {
            "itens": [
                {
                    "numero": 1,
                    "pergunta": "Pergunta",
                    "respostas": {"produção": ["C", "Joao"]},
                }
            ]
        },
    }
    with open(insp_dir / "checklist_OBRA1.json", "w", encoding="utf-8") as fp:
        json.dump(base_data, fp, ensure_ascii=False)

    app = Flask(__name__)
    app.register_blueprint(api.bp)

    payload = {
        "obra": "OBRA1",
        "itens": [
            {"numero": 1, "pergunta": "Pergunta", "resposta": ["C"]}
        ],
        "inspetor": "Inspector",
    }
    with app.test_client() as client:
        res = client.post("/posto02/insp/upload", json=payload)
        assert res.status_code == 200
        body = res.get_json()

    assert body["divergencias"] == []
    dest = pathlib.Path(body["caminho"])
    assert dest.parent.name == "Posto03_Pre_montagem_01"
    with open(dest, "r", encoding="utf-8") as fp:
        saved = json.load(fp)
    item = saved["posto02"]["itens"][0]
    assert item["respostas"]["inspetor"] == ["C"]


def test_posto03_inspector_ignores_name_differences(tmp_path: pathlib.Path) -> None:
    api.BASE_DIR = str(tmp_path)
    insp_dir = (
        tmp_path / "Posto03_Pre_montagem_01" / "Posto03_Pre_montagem_01_Inspetor"
    )
    insp_dir.mkdir(parents=True)

    base_data = {
        "obra": "OBRA1",
        "ano": "2024",
        "posto03_pre_montagem_01": {
            "itens": [
                {
                    "numero": 1,
                    "pergunta": "Pergunta",
                    "respostas": {"montador": ["C", "Joao"]},
                }
            ]
        },
    }
    with open(insp_dir / "checklist_OBRA1.json", "w", encoding="utf-8") as fp:
        json.dump(base_data, fp, ensure_ascii=False)

    app = Flask(__name__)
    app.register_blueprint(api.bp)

    payload = {
        "obra": "OBRA1",
        "itens": [
            {"numero": 1, "pergunta": "Pergunta", "resposta": ["C", "Maria"]}
        ],
        "inspetor": "Inspector",
    }
    with app.test_client() as client:
        res = client.post("/posto03_pre/insp/upload", json=payload)
        assert res.status_code == 200
        body = res.get_json()

    assert body["divergencias"] == []
    dest = pathlib.Path(body["caminho"])
    assert dest.parent.name == "POSTO_04_BARRAMENTO"
    with open(dest, "r", encoding="utf-8") as fp:
        saved = json.load(fp)
    item = saved["posto03_pre_montagem_01"]["itens"][0]
    assert item["respostas"]["montador"] == ["C", "Joao"]
    assert item["respostas"]["inspetor"] == ["C", "Maria"]
