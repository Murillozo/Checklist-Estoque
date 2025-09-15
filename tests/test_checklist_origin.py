from __future__ import annotations

import json
import pathlib
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
