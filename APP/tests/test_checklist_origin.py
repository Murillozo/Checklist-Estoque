from __future__ import annotations

import json
import os
import pathlib
import time
from flask import Flask

import importlib
import sys

SITE_DIR = pathlib.Path(__file__).resolve().parents[1] / "site"
sys.path.insert(0, str(SITE_DIR))
api = importlib.import_module("json_api")


def _client(tmp_path: pathlib.Path):
    api.BASE_DIR = str(tmp_path)
    app = Flask(__name__)
    app.register_blueprint(api.bp)
    return app.test_client()


def test_salvar_checklist_appestoque_does_not_merge(tmp_path: pathlib.Path, monkeypatch):
    called = {"merge": 0, "move": 0}

    def fake_merge(directory: str) -> None:
        called["merge"] += 1

    def fake_move(directory: str) -> None:
        called["move"] += 1

    monkeypatch.setattr(api, "merge_directory", fake_merge)
    monkeypatch.setattr(api, "move_matching_checklists", fake_move)

    client = _client(tmp_path)
    payload = {"obra": "OBRA1", "origem": "AppEstoque"}
    res = client.post("/checklist", json=payload)
    assert res.status_code == 200
    assert called == {"merge": 0, "move": 0}


def test_salvar_checklist_appoficina_triggers_merge(tmp_path: pathlib.Path, monkeypatch):
    called = {"merge": 0, "move": 0}

    def fake_merge(directory: str) -> None:
        called["merge"] += 1

    def fake_move(directory: str) -> None:
        called["move"] += 1

    monkeypatch.setattr(api, "merge_directory", fake_merge)
    monkeypatch.setattr(api, "move_matching_checklists", fake_move)

    client = _client(tmp_path)
    payload = {"obra": "OBRA1", "origem": "AppOficina"}
    res = client.post("/checklist", json=payload)
    assert res.status_code == 200
    assert called["merge"] == 1
    assert called["move"] == 1


def test_salvar_checklist_multiple_uploads_unique_names(tmp_path: pathlib.Path, monkeypatch):
    called = {"merge": 0, "move": 0}

    def fake_merge(directory: str) -> None:
        called["merge"] += 1

    def fake_move(directory: str) -> None:
        called["move"] += 1

    monkeypatch.setattr(api, "merge_directory", fake_merge)
    monkeypatch.setattr(api, "move_matching_checklists", fake_move)

    client = _client(tmp_path)

    estoque_payload = {"obra": "OBRA1", "origem": "AppEstoque"}
    oficina_payload = {"obra": "OBRA1", "origem": "AppOficina"}

    res1 = client.post("/checklist", json=estoque_payload)
    res2 = client.post("/checklist", json=oficina_payload)

    assert res1.status_code == 200
    assert res2.status_code == 200

    arquivos = sorted(p.name for p in tmp_path.glob("*.json"))
    assert len(arquivos) == 2
    assert arquivos[0] != arquivos[1]

    assert called["merge"] == 1
    assert called["move"] == 1


def test_obter_checklist_requires_obra(tmp_path: pathlib.Path):
    client = _client(tmp_path)

    res = client.get("/checklist")
    assert res.status_code == 400
    assert res.get_json()["erro"] == "obra obrigatória"


def test_obter_checklist_inexistente(tmp_path: pathlib.Path):
    client = _client(tmp_path)

    res = client.get("/checklist", query_string={"obra": "OBRA-XYZ"})
    assert res.status_code == 404


def test_obter_checklist_retorna_arquivo_mais_recente(tmp_path: pathlib.Path):
    client = _client(tmp_path)

    antigo = tmp_path / "checklist_OBRA1_20220101.json"
    antigo.write_text(
        json.dumps(
            {
                "obra": "OBRA1",
                "itens": [
                    {"pergunta": "Antiga", "respostas": {"suprimento": ["NC"]}},
                ],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    timestamp_antigo = time.time() - 3600
    os.utime(antigo, (timestamp_antigo, timestamp_antigo))

    destino = tmp_path / "Posto01_Oficina"
    destino.mkdir()
    recente = destino / "checklist_OBRA1.json"
    recente.write_text(
        json.dumps(
            {
                "obra": "OBRA1",
                "itens": [
                    {
                        "pergunta": "Atual",
                        "respostas": {"suprimento": ["C", "Maria"]},
                    },
                ],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )

    res = client.get("/checklist", query_string={"obra": "OBRA1"})
    assert res.status_code == 200
    dados = res.get_json()
    assert dados["checklist"]["itens"][0]["pergunta"] == "Atual"
    assert dados["checklist"]["itens"][0]["respostas"]["suprimento"][0] == "C"