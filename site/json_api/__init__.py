from flask import Blueprint, request, jsonify
import os
import json
from datetime import datetime
from os import path

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
    merge_directory(BASE_DIR)
    move_matching_checklists(BASE_DIR)

    return jsonify({'caminho': file_path})


@bp.route('/projects', methods=['GET'])
def listar_projetos():
    """Return obra/ano info for each checklist JSON file."""
    arquivos = [f for f in os.listdir(BASE_DIR) if f.endswith('.json')]
    projetos = []
    for nome in sorted(arquivos):
        caminho = path.join(BASE_DIR, nome)
        try:
            with open(caminho, 'r', encoding='utf-8') as f:
                data = json.load(f)
            projetos.append({
                'arquivo': nome,
                'obra': data.get('obra', path.splitext(nome)[0]),
                'ano': data.get('ano', '')
            })
        except Exception:
            continue
    return jsonify({'projetos': projetos})


@bp.route('/posto02/projects', methods=['GET'])
def listar_posto02_projetos():
    """Return obra/ano info for each checklist JSON file in Posto02_Oficina."""
    dir_path = os.path.join(BASE_DIR, 'Posto02_Oficina')
    if not os.path.isdir(dir_path):
        return jsonify({'projetos': []})
    arquivos = [f for f in os.listdir(dir_path) if f.endswith('.json')]
    projetos = []
    for nome in sorted(arquivos):
        caminho = path.join(dir_path, nome)
        try:
            with open(caminho, 'r', encoding='utf-8') as f:
                data = json.load(f)
            projetos.append({
                'arquivo': nome,
                'obra': data.get('obra', path.splitext(nome)[0]),
                'ano': data.get('ano', ''),
            })
        except Exception:
            continue
    return jsonify({'projetos': projetos})


@bp.route('/revisao', methods=['GET'])
def listar_revisao():
    """Return checklists whose answers diverge between departments."""
    dir_path = os.path.join(BASE_DIR, 'Posto01_Oficina')
    if not os.path.isdir(dir_path):
        return jsonify([])
    dados = find_mismatches(dir_path)
    return jsonify(dados)


@bp.route('/revisao/reenviar', methods=['POST'])
def reenviar_checklist():
    """Recreate a suprimento checklist from a merged file and remove the old merge."""
    data = request.get_json() or {}
    obra = data.get('obra')
    ano = data.get('ano', '')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    merged_path = os.path.join(BASE_DIR, 'Posto01_Oficina', f'checklist_{obra}.json')
    if not os.path.exists(merged_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(merged_path, 'r', encoding='utf-8') as f:
        merged = json.load(f)

    sup_nome = merged.get('respondentes', {}).get('suprimento')
    sup_itens = []
    for item in merged.get('itens', []):
        sup_resp = item.get('respostas', {}).get('suprimento')
        if sup_resp is not None:
            sup_itens.append({
                'numero': item.get('numero'),
                'pergunta': item.get('pergunta'),
                'resposta': sup_resp,
            })

    novo = {
        'obra': merged.get('obra', obra),
        'ano': merged.get('ano', ano),
        'suprimento': sup_nome,
        'itens': sup_itens,
        'materiais': merged.get('materiais', []),
    }

    ts = datetime.now().strftime('%Y%m%d%H%M%S')
    out_path = os.path.join(BASE_DIR, f'checklist_{obra}_{ts}.json')
    with open(out_path, 'w', encoding='utf-8') as f:
        json.dump(novo, f, ensure_ascii=False, indent=2)

    try:
        os.remove(merged_path)
    except OSError:
        pass

    return jsonify({'caminho': out_path})

# legacy alias
bp.add_url_rule('/upload', view_func=salvar_checklist, methods=['POST'])

# utilidades de mesclagem
from .merge_checklists import merge_checklists, merge_directory, find_mismatches
from .merge_checklists import move_matching_checklists
