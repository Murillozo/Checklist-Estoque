import json
import pathlib
import importlib
import sys
from flask import Flask

SITE_DIR = pathlib.Path(__file__).resolve().parents[1] / "site"
sys.path.insert(0, str(SITE_DIR))
api = importlib.import_module("json_api")


def _client(tmp_path: pathlib.Path):
    api.BASE_DIR = str(tmp_path)
    app = Flask(__name__)
    app.register_blueprint(api.bp)
    return app.test_client()


def test_upload_and_inspection_without_divergencias(tmp_path):
    # Prepare base file expected by upload endpoint
    src_dir = tmp_path / "Posto02_Oficina"
    src_dir.mkdir()
    with open(src_dir / "checklist_OBRA1.json", "w", encoding="utf-8") as f:
        json.dump({"obra": "OBRA1"}, f)

    client = _client(tmp_path)

    upload_payload = {
        "obra": "OBRA1",
        "montador": "Joao",
        "itens": [
            {"numero": 1, "pergunta": "Pergunta1", "respostas": {"montador": ["C"]}},
            {"numero": 2, "pergunta": "Pergunta2", "resposta": ["C"]},
        ],
    }
    res = client.post("/posto02/upload", json=upload_payload)
    assert res.status_code == 200

    insp_payload = {
        "obra": "OBRA1",
        "inspetor": "Maria",
        "itens": [
            {"numero": 1, "pergunta": "Pergunta1", "resposta": ["C"]},
            {"numero": 2, "pergunta": "Pergunta2", "resposta": ["C"]},
        ],
    }
    res_insp = client.post("/posto02/insp/upload", json=insp_payload)
    assert res_insp.status_code == 200
    assert res_insp.get_json()["divergencias"] == []
