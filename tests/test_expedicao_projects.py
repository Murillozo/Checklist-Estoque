import json
import importlib
import pathlib
import sys
from flask import Flask

SITE_DIR = pathlib.Path(__file__).resolve().parents[1] / "site"
sys.path.insert(0, str(SITE_DIR))
api = importlib.import_module("json_api")


def make_client(tmp_path, monkeypatch):
    monkeypatch.setattr(api, "BASE_DIR", tmp_path)
    app = Flask(__name__)
    app.register_blueprint(api.bp, url_prefix="/json_api")
    return app.test_client()


def test_listar_expedicao_projetos(tmp_path, monkeypatch):
    exp_dir = tmp_path / 'EXPEDICAO'
    exp_dir.mkdir()
    with open(exp_dir / 'checklist_OBRA.json', 'w', encoding='utf-8') as f:
        json.dump({'ano': '2024'}, f)
    client = make_client(tmp_path, monkeypatch)
    res = client.get('/json_api/expedicao/projects')
    assert res.status_code == 200
    assert res.get_json() == {
        'projetos': [
            {'arquivo': 'checklist_OBRA.json', 'obra': 'OBRA', 'ano': '2024'}
        ]
    }


def test_obter_expedicao_checklist(tmp_path, monkeypatch):
    exp_dir = tmp_path / 'EXPEDICAO'
    exp_dir.mkdir()
    data = {'obra': 'OBRA', 'ano': '2024'}
    with open(exp_dir / 'checklist_OBRA.json', 'w', encoding='utf-8') as f:
        json.dump(data, f)
    client = make_client(tmp_path, monkeypatch)
    res = client.get('/json_api/expedicao/checklist', query_string={'obra': 'OBRA'})
    assert res.status_code == 200
    payload = res.get_json()
    assert payload['obra'] == 'OBRA'
    assert payload['ano'] == '2024'
    assert len(payload['itens']) == 11
