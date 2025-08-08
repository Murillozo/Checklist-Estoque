from flask import Blueprint, request, jsonify
import os
import json
from datetime import datetime

bp = Blueprint('json_api', __name__)

BASE_DIR = os.path.dirname(__file__)


@bp.route('/checklist', methods=['POST'])
def salvar_checklist():
    """Save a checklist payload to a timestamped JSON file."""
    data = request.get_json() or {}
    obra = data.get('obra', 'desconhecida')

    os.makedirs(BASE_DIR, exist_ok=True)
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
    safe_obra = "".join(c for c in obra if c.isalnum() or c in ('-','_')) or 'obra'
    filename = f"checklist_{safe_obra}_{timestamp}.json"
    file_path = os.path.join(BASE_DIR, filename)

    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    return jsonify({'caminho': file_path})


@bp.route('/projects', methods=['GET'])
def listar_projetos():
    """Return names and items of all checklist JSON files."""
    files = [f for f in os.listdir(BASE_DIR) if f.endswith('.json')]
    projetos = {}
    for nome in files:
        caminho = os.path.join(BASE_DIR, nome)
        try:
            with open(caminho, 'r', encoding='utf-8') as f:
                data = json.load(f)
                itens = data.get('items', [])
            projetos[nome] = itens
        except Exception:
            continue
    return jsonify({'projects': projetos})

