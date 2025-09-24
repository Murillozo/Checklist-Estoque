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
import subprocess
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


def test_merge_checklists_accepts_montador_key() -> None:
    sup = {
        "obra": "OBRA1",
        "ano": "2024",
        "suprimento": "Victor",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "respostas": {"suprimento": ["C"]},
            }
        ],
    }
    prod = {
        "obra": "OBRA1",
        "ano": "2024",
        "montador": "Joao",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "respostas": {"montador": ["C"]},
            }
        ],
    }

    merged = merge.merge_checklists(sup, prod)

    assert merged["respondentes"]["produção"] == "Joao"
    assert merged["respondentes"]["suprimento"] == "Victor"
    assert merged["itens"][0]["respostas"] == {
        "suprimento": ["C", "Victor"],
        "montador": ["C", "Joao"],
    }




def test_merge_checklists_handles_multiple_montadores() -> None:
    sup = {
        "obra": "OBRA1",
        "ano": "2024",
        "suprimento": "Victor",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "respostas": {"suprimento": ["C"]},
            }
        ],
    }
    prod = {
        "obra": "OBRA1",
        "ano": "2024",
        "montador": "Joao",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "montador": "Joao",
                "respostas": {"montador": ["C"]},
            },
            {
                "numero": 2,
                "pergunta": "Outra",
                "montador": "Maria",
                "respostas": {"montador": ["C"]},
            },
        ],
    }
    merged = merge.merge_checklists(sup, prod)
    assert merged["respondentes"]["produção"] == "Joao"
    assert merged["itens"][0]["respostas"] == {
        "suprimento": ["C", "Victor"],
        "montador": ["C", "Joao"],
    }


def test_merge_checklists_handles_conflicting_numbers_and_missing_montador() -> None:
    sup = {
        "obra": "OBRA1",
        "ano": "2024",
        "suprimento": "Victor",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta A",
                "respostas": {"suprimento": ["C"]},
            },
            {
                "numero": 75,
                "pergunta": "Pergunta B",
                "respostas": {"suprimento": ["C"]},
            },
        ],
    }
    prod = {
        "obra": "OBRA1",
        "ano": "2024",
        "origem": "AppOficina",
        "itens": [
            {
                "numero": 55,
                "pergunta": "Pergunta A",
                "respostas": {"producao": ["C", "Carlos"]},
            },
            {
                "numero": 75,
                "pergunta": "Pergunta C",
                "respostas": {"producao": ["C", "Carlos"]},
            },
            {
                "numero": 109,
                "pergunta": "Pergunta B",
                "respostas": {"producao": ["C", "Carlos"]},
            },
        ],
    }

    merged = merge.merge_checklists(sup, prod)

    perguntas = {item["pergunta"]: item for item in merged["itens"]}
    assert perguntas["Pergunta B"]["numero"] == [75, 109]
    assert perguntas["Pergunta B"]["respostas"] == {
        "suprimento": ["C", "Victor"],
        "producao": ["C", "Carlos"],
    }
    assert perguntas["Pergunta C"]["numero"] == [75]
    assert merged["respondentes"]["produção"] == "Carlos"


def test_merge_checklists_maps_appestoque_aliases_to_suprimento() -> None:
    pergunta = "1.15 - COMPONENTES: Identificação do projeto"
    sup = {
        "obra": "MERGEKRAI",
        "ano": "2025",
        "suprimento": "victorr",
        "itens": [
            {
                "numero": 75,
                "pergunta": pergunta,
                "respostas": {"producao": ["C"]},
            }
        ],
    }
    prod = {
        "obra": "MERGEKRAI",
        "ano": "2025",
        "origem": "AppOficina",
        "itens": [
            {
                "numero": 109,
                "pergunta": pergunta,
                "respostas": {"producao": ["C", "Carlos"]},
            }
        ],
    }

    merged = merge.merge_checklists(sup, prod)

    item = next(entry for entry in merged["itens"] if entry["pergunta"] == pergunta)
    assert item["numero"] == [75, 109]
    assert item["respostas"] == {
        "suprimento": ["C", "victorr"],
        "producao": ["C", "Carlos"],
    }


def test_merge_checklists_accepts_list_numbers_from_previous_merges() -> None:
    pergunta = "1.1 - INVÓLUCRO"
    sup = {
        "obra": "OBRA123",
        "ano": "2025",
        "suprimento": "Ana",
        "itens": [
            {
                "numero": [55, 56],
                "pergunta": pergunta,
                "respostas": {"suprimento": ["NC"]},
            }
        ],
    }
    prod = {
        "obra": "OBRA123",
        "ano": "2025",
        "origem": "AppOficina",
        "itens": [
            {
                "numero": 128,
                "pergunta": pergunta,
                "respostas": {"producao": ["NA", "Carlos"]},
            }
        ],
    }

    merged = merge.merge_checklists(sup, prod)

    item = next(entry for entry in merged["itens"] if entry["pergunta"] == pergunta)
    assert item["numero"] == [55, 56, 128]
    assert item["respostas"] == {
        "suprimento": ["NC", "Ana"],
        "producao": ["NA", "Carlos"],
    }


def test_merge_directory_preserves_suprimento_answers_for_component_block(
    tmp_path: pathlib.Path,
) -> None:
    """Regression for AppEstoque respostas registradas como produção."""

    pergunta_tpl = "1.{idx} - TESTE"
    sup = {
        "obra": "OBRA-SUP",
        "ano": "2026",
        "suprimento": "Victor",
        "itens": [
            {
                "numero": 74 + idx,
                "pergunta": pergunta_tpl.format(idx=idx),
                "respostas": {"producao": ["C"]},
            }
            for idx in range(15, 20)
        ],
    }
    prod = {
        "obra": "OBRA-SUP",
        "ano": "2026",
        "origem": "AppOficina",
        "itens": [
            {
                "numero": 108 + idx,
                "pergunta": pergunta_tpl.format(idx=idx),
                "respostas": {"producao": ["C", "Carlos"]},
            }
            for idx in range(15, 20)
        ],
    }

    sup_path = tmp_path / "sup.json"
    prod_path = tmp_path / "prod.json"
    sup_path.write_text(json.dumps(sup), encoding="utf-8")
    prod_path.write_text(json.dumps(prod), encoding="utf-8")

    merged = merge.merge_directory(str(tmp_path))
    assert merged, "Merge should generate checklist"

    perguntas = {item["pergunta"]: item for item in merged[0]["itens"]}
    for idx in range(15, 20):
        pergunta = pergunta_tpl.format(idx=idx)
        respostas = perguntas[pergunta]["respostas"]
        assert "suprimento" in respostas
        assert respostas["suprimento"][0] == "C"
        assert respostas["producao"][0] == "C"


def test_merge_directory_reuses_previous_suprimento_answers(
    tmp_path: pathlib.Path,
) -> None:
    """Production uploads should reuse the last known suprimento answers."""

    posto02 = tmp_path / "Posto02_Oficina"
    posto02.mkdir()

    merged_payload = {
        "obra": "OBRA1",
        "ano": "2024",
        "respondentes": {"suprimento": "Victor", "produção": "Carlos"},
        "itens": [
            {
                "numero": [1],
                "pergunta": "Pergunta",
                "respostas": {
                    "suprimento": ["C", "Victor"],
                    "producao": ["C", "Carlos"],
                },
            }
        ],
        "materiais": [],
    }
    (posto02 / "checklist_OBRA1.json").write_text(
        json.dumps(merged_payload), encoding="utf-8"
    )

    novo_prod = {
        "obra": "OBRA1",
        "ano": "2024",
        "origem": "AppOficina",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "respostas": {"producao": ["NC", "Carlos"]},
            }
        ],
    }
    raw_path = tmp_path / "checklist_OBRA1_20240101.json"
    raw_path.write_text(json.dumps(novo_prod), encoding="utf-8")

    merged = merge.merge_directory(str(tmp_path))

    assert merged, "Expected merge to reuse suprimento payload"
    respostas = merged[0]["itens"][0]["respostas"]
    assert respostas["suprimento"][0] == "C"
    assert respostas["producao"][0] == "NC"

    destino = tmp_path / "Posto01_Oficina" / "checklist_OBRA1.json"
    assert destino.exists(), "Merged checklist should be stored for revisão"
    assert not raw_path.exists(), "Raw production upload must be removed"


def test_find_mismatches_ignores_additional_production_annotations(tmp_path: pathlib.Path) -> None:
    path = tmp_path / "checklist_OBRA1.json"
    _write_checklist(path, ["C", "Joao"], ["C", "Joao", "Maria"])

    assert find_mismatches(str(tmp_path)) == []


def test_find_mismatches_reports_nc_answers(tmp_path: pathlib.Path) -> None:
    path = tmp_path / "checklist_OBRA1.json"
    _write_checklist(path, ["NC", "Carlos"], ["C", "Joao"])

    resultado = find_mismatches(str(tmp_path))
    assert len(resultado) == 1
    divergencia = resultado[0]["divergencias"][0]
    assert divergencia["suprimento"] == ["NC", "Carlos"]
    assert divergencia["produção"] == ["C", "Joao"]
    assert divergencia["respostas"]["suprimento"] == ["NC", "Carlos"]


def test_find_mismatches_reports_na_answers(tmp_path: pathlib.Path) -> None:
    path = tmp_path / "checklist_OBRA1.json"
    _write_checklist(path, ["C", "Carlos"], ["NA", "Joao"])

    resultado = find_mismatches(str(tmp_path))
    assert len(resultado) == 1
    divergencia = resultado[0]["divergencias"][0]
    assert divergencia["suprimento"] == ["C", "Carlos"]
    assert divergencia["produção"] == ["NA", "Joao"]


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

    assert data["itens"][0]["respostas"] == {
        "suprimento": ["C", "Joao"],
        "produção": ["C", "Joao", "Maria"],
    }
    assert find_mismatches(str(tmp_path / "Posto02_Oficina")) == []


def test_move_matching_removes_raw_uploads(tmp_path: pathlib.Path) -> None:
    src_dir = tmp_path / "Posto01_Oficina"
    src_dir.mkdir()

    checklist_path = src_dir / "checklist_OBRA1.json"
    data = {
        "obra": "OBRA1",
        "ano": "2024",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "respostas": {"suprimento": ["C"], "produção": ["C"]},
            }
        ],
    }
    with open(checklist_path, "w", encoding="utf-8") as fp:
        json.dump(data, fp, ensure_ascii=False)

    leftover = tmp_path / "checklist_OBRA1_20240101000000.json"
    with open(leftover, "w", encoding="utf-8") as fp:
        json.dump({"obra": "OBRA1", "itens": []}, fp, ensure_ascii=False)

    other = tmp_path / "checklist_OBRA2_20240101000000.json"
    with open(other, "w", encoding="utf-8") as fp:
        json.dump({"obra": "OBRA2", "itens": []}, fp, ensure_ascii=False)

    moved = move_matching_checklists(str(tmp_path))
    assert moved == ["checklist_OBRA1.json"]

    assert not leftover.exists()
    assert other.exists()


def test_cli_merges_and_moves_to_posto02(tmp_path: pathlib.Path) -> None:
    sup = {
        "obra": "OBRA1",
        "ano": "2024",
        "suprimento": "Carlos",
        "itens": [
            {"numero": 1, "pergunta": "Pergunta", "respostas": {"suprimento": ["C"]}}
        ],
    }
    prod = {
        "obra": "OBRA1",
        "ano": "2024",
        "montador": "Joao",
        "itens": [
            {"numero": 1, "pergunta": "Pergunta", "respostas": {"montador": ["C"]}}
        ],
    }
    with open(tmp_path / "sup.json", "w", encoding="utf-8") as fp:
        json.dump(sup, fp, ensure_ascii=False)
    with open(tmp_path / "prod.json", "w", encoding="utf-8") as fp:
        json.dump(prod, fp, ensure_ascii=False)

    subprocess.run([sys.executable, str(MODULE_PATH), str(tmp_path)], check=True)

    dest_path = tmp_path / "Posto02_Oficina" / "checklist_OBRA1.json"
    with open(dest_path, "r", encoding="utf-8") as fp:
        data = json.load(fp)

    assert data["itens"][0]["respostas"] == {
        "suprimento": ["C", "Carlos"],
        "montador": ["C", "Joao"],
    }


def test_merge_directory_detects_production_in_item_respostas(tmp_path: pathlib.Path) -> None:
    sup = {
        "obra": "OBRA1",
        "ano": "2024",
        "suprimento": "Carlos",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "respostas": {"suprimento": ["C"]},
            }
        ],
    }
    prod = {
        "obra": "OBRA1",
        "ano": "2024",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "respostas": {"montador": ["J"]},
            }
        ],
    }
    with open(tmp_path / "sup.json", "w", encoding="utf-8") as fp:
        json.dump(sup, fp, ensure_ascii=False)
    with open(tmp_path / "prod.json", "w", encoding="utf-8") as fp:
        json.dump(prod, fp, ensure_ascii=False)

    merged = merge.merge_directory(str(tmp_path))
    assert len(merged) == 1

    out_path = tmp_path / "Posto01_Oficina" / "checklist_OBRA1.json"
    with open(out_path, "r", encoding="utf-8") as fp:
        data = json.load(fp)

    assert data["itens"][0]["respostas"] == {
        "suprimento": ["C", "Carlos"],
        "montador": ["J"],
    }


def test_merge_directory_handles_appoficina_origin(tmp_path: pathlib.Path) -> None:
    sup = {
        "obra": "OBRA1",
        "ano": "2024",
        "suprimento": "Carlos",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "respostas": {"suprimento": ["C"]},
            }
        ],
    }
    prod = {
        "obra": "OBRA1",
        "ano": "2024",
        "origem": "AppOficina",
        "itens": [
            {
                "numero": 1,
                "pergunta": "Pergunta",
                "resposta": ["OK"],
            }
        ],
    }

    sup_path = tmp_path / "sup_OBRA1.json"
    prod_path = tmp_path / "20240102T120000_OBRA1.json"
    with open(sup_path, "w", encoding="utf-8") as fp:
        json.dump(sup, fp, ensure_ascii=False)
    with open(prod_path, "w", encoding="utf-8") as fp:
        json.dump(prod, fp, ensure_ascii=False)

    merged = merge.merge_directory(str(tmp_path))
    assert len(merged) == 1

    out_path = tmp_path / "Posto01_Oficina" / "checklist_OBRA1.json"
    with open(out_path, "r", encoding="utf-8") as fp:
        data = json.load(fp)

    assert data["itens"][0]["respostas"] == {
        "suprimento": ["C", "Carlos"],
        "montador": ["OK"],
    }
    assert not sup_path.exists()
    assert not prod_path.exists()


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
    assert item["respostas"]["montador"] == ["C", "Joao"]
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