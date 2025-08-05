from flask import Blueprint, request, jsonify
import os
import json
from datetime import datetime

bp = Blueprint('json_api', __name__)

BASE_DIR = os.path.dirname(__file__)

@bp.route('/checklist', methods=['POST'])
def salvar_checklist():
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
