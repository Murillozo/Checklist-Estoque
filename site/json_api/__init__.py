from flask import Blueprint, request, jsonify
import os
import json
from datetime import datetime
from os import path

bp = Blueprint('json_api', __name__)

BASE_DIR = os.path.dirname(__file__)

def _collect_nc_items(data):
    """Return list of items with at least one "NC" answer."""
    nc_itens = []

    def walk(obj):
        if isinstance(obj, dict):
            itens = obj.get("itens")
            if isinstance(itens, list):
                for item in itens:
                    respostas = item.get("respostas", {})
                    nc_respostas = {}
                    for papel, resp in respostas.items():
                        if isinstance(resp, list):
                            for r in resp:
                                if r.replace(".", "").strip().upper() == "NC":
                                    nc_respostas[papel] = r
                                    break
                    if nc_respostas:
                        nc_itens.append(
                            {
                                "numero": item.get("numero"),
                                "pergunta": item.get("pergunta"),
                                "respostas": nc_respostas,
                            }
                        )
            for value in obj.values():
                walk(value)
        elif isinstance(obj, list):
            for value in obj:
                walk(value)

    walk(data)
    return nc_itens



def _collect_double_nc(data):
    """Return list of answer blocks where both roles answered 'NC'."""
    resultados = []

    def walk(obj):
        if isinstance(obj, dict):
            keys = set(obj.keys())
            if keys in ({"montador", "inspetor"}, {"suprimento", "produção"}):
                valores = []
                for v in obj.values():
                    if isinstance(v, list) and len(v) == 1:
                        valores.append(v[0])
                if len(valores) == 2 and all(v == "NC" for v in valores):
                    resultados.append(obj)
            for v in obj.values():
                walk(v)
        elif isinstance(obj, list):
            for item in obj:
                walk(item)

    walk(data)
    return resultados



def _ensure_nc_preview(file_path: str) -> None:
    """Append preview of NC answers to ``file_path`` in-place."""
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception:
        return

    data["pre_visualizacao"] = _collect_nc_items(data)
    data["respostas_duplas_NC"] = _collect_double_nc(data)

    with open(file_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def _collect_nc_items(data):
    """Return list of items with at least one "NC" answer."""
    nc_itens = []

    def walk(obj):
        if isinstance(obj, dict):
            itens = obj.get("itens")
            if isinstance(itens, list):
                for item in itens:
                    respostas = item.get("respostas", {})
                    nc_respostas = {}
                    for papel, resp in respostas.items():
                        if isinstance(resp, list):
                            for r in resp:
                                if r.replace(".", "").strip().upper() == "NC":
                                    nc_respostas[papel] = r
                                    break
                    if nc_respostas:
                        nc_itens.append(
                            {
                                "numero": item.get("numero"),
                                "pergunta": item.get("pergunta"),
                                "respostas": nc_respostas,
                            }
                        )
            for value in obj.values():
                walk(value)
        elif isinstance(obj, list):
            for value in obj:
                walk(value)

    walk(data)
    return nc_itens


def _collect_double_nc(data):
    """Return list of answer blocks where both roles answered 'NC'."""
    resultados = []

    def walk(obj):
        if isinstance(obj, dict):
            keys = set(obj.keys())
            if keys in ({"montador", "inspetor"}, {"suprimento", "produção"}):
                valores = []
                for v in obj.values():
                    if isinstance(v, list) and len(v) == 1:
                        valores.append(v[0])
                if len(valores) == 2 and all(v == "NC" for v in valores):
                    resultados.append(obj)
            for v in obj.values():
                walk(v)
        elif isinstance(obj, list):
            for item in obj:
                walk(item)

    walk(data)
    return resultados


def _ensure_nc_preview(file_path: str) -> None:
    """Append preview of NC answers to ``file_path`` in-place."""
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception:
        return

    data["pre_visualizacao"] = _collect_nc_items(data)
    data["respostas_duplas_NC"] = _collect_double_nc(data)

    with open(file_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def _collect_nc_items(data):
    """Return list of items with at least one "NC" answer."""
    nc_itens = []

    def walk(obj):
        if isinstance(obj, dict):
            itens = obj.get("itens")
            if isinstance(itens, list):
                for item in itens:
                    respostas = item.get("respostas", {})
                    nc_respostas = {}
                    for papel, resp in respostas.items():
                        if isinstance(resp, list):
                            for r in resp:
                                if r.replace(".", "").strip().upper() == "NC":
                                    nc_respostas[papel] = r
                                    break
                    if nc_respostas:
                        nc_itens.append(
                            {
                                "numero": item.get("numero"),
                                "pergunta": item.get("pergunta"),
                                "respostas": nc_respostas,
                            }
                        )
            for value in obj.values():
                walk(value)
        elif isinstance(obj, list):
            for value in obj:
                walk(value)

    walk(data)
    return nc_itens


def _collect_double_nc(data):
    """Return list of answer blocks where both roles answered 'NC'."""
    resultados = []

    def walk(obj):
        if isinstance(obj, dict):
            keys = set(obj.keys())
            if keys in ({"montador", "inspetor"}, {"suprimento", "produção"}):
                valores = []
                for v in obj.values():
                    if isinstance(v, list) and len(v) == 1:
                        valores.append(v[0])
                if len(valores) == 2 and all(v == "NC" for v in valores):
                    resultados.append(obj)
            for v in obj.values():
                walk(v)
        elif isinstance(obj, list):
            for item in obj:
                walk(item)

    walk(data)
    return resultados


def _ensure_nc_preview(file_path: str) -> None:
    """Append preview of NC answers to ``file_path`` in-place."""
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception:
        return

    data["pre_visualizacao"] = _collect_nc_items(data)
    data["respostas_duplas_NC"] = _collect_double_nc(data)

    with open(file_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


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


@bp.route('/expedicao/projects', methods=['GET'])
def listar_expedicao_projetos():
    """List projects awaiting logística checklist."""
    dir_path = os.path.join(BASE_DIR, 'EXPEDICAO')
    if not os.path.isdir(dir_path):
        return jsonify({'projetos': []})
    arquivos = [f for f in os.listdir(dir_path) if f.endswith('.json')]
    projetos = []
    for nome in sorted(arquivos):
        caminho = path.join(dir_path, nome)
        try:
            with open(caminho, 'r', encoding='utf-8') as f:
                data = json.load(f)
            projetos.append(
                {
                    'arquivo': nome,
                    'obra': data.get('obra', path.splitext(nome)[0]),
                    'ano': data.get('ano', ''),
                }
            )
        except Exception:
            continue
    return jsonify({'projetos': projetos})


@bp.route('/expedicao/checklist', methods=['GET'])
def obter_expedicao_checklist():
    """Return checklist data for a given obra in EXPEDICAO."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    file_path = os.path.join(BASE_DIR, 'EXPEDICAO', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    itens = [
        {'numero': 1, 'pergunta': 'Comunicado à transportadora'},
        {'numero': 2, 'pergunta': 'Comunicado ao cliente'},
        {'numero': 3, 'pergunta': 'Nota fiscal'},
        {'numero': 4, 'pergunta': 'As Built'},
        {'numero': 5, 'pergunta': 'Limpeza'},
        {'numero': 6, 'pergunta': 'Montagem de anteparos'},
        {'numero': 7, 'pergunta': 'Fotos Sem embalagem'},
        {'numero': 8, 'pergunta': 'Romaneio'},
        {'numero': 9, 'pergunta': 'Chaves das portas'},
        {'numero': 10, 'pergunta': 'Embalagem'},
        {'numero': 11, 'pergunta': 'Fotos Com embalagem'},
    ]

    return jsonify({'obra': data.get('obra', obra), 'ano': data.get('ano', ''), 'itens': itens})


@bp.route('/expedicao/upload', methods=['POST'])
def expedicao_upload():
    """Append logística answers and move checklist to final directory."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(BASE_DIR, 'EXPEDICAO', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    itens = []
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        itens.append({'numero': numero, 'pergunta': pergunta, 'respostas': {'logistica': resposta}})

    base['expedicao'] = {'itens': itens}

    dest_dir = os.path.join(BASE_DIR, 'CHECKLIST_FINAL')
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path})


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

@bp.route('/posto02/checklist', methods=['GET'])
def obter_posto02_checklist():
    """Return full checklist data for a given obra in Posto02_Oficina."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    file_path = os.path.join(BASE_DIR, 'Posto02_Oficina', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    return jsonify(data)


@bp.route('/posto08_iqm/projects', methods=['GET'])
def listar_posto08_iqm_projetos():
    """List available IQM checklists."""
    dir_path = os.path.join(BASE_DIR, 'posto08_IQM')
    if not os.path.isdir(dir_path):
        return jsonify({'projetos': []})

    arquivos = [f for f in os.listdir(dir_path) if f.endswith('.json')]
    projetos = []
    for nome in sorted(arquivos):
        caminho = path.join(dir_path, nome)
        try:
            _ensure_nc_preview(caminho)
            with open(caminho, 'r', encoding='utf-8') as f:
                data = json.load(f)
            projetos.append(
                {
                    'arquivo': nome,
                    'obra': data.get('obra', path.splitext(nome)[0]),
                    'ano': data.get('ano', ''),
                }
            )
        except Exception:
            continue
    return jsonify({'projetos': projetos})


@bp.route('/posto08_iqm/checklist', methods=['GET'])
def obter_posto08_iqm_checklist():
    """Return full IQM checklist for a given obra."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    file_path = os.path.join(BASE_DIR, 'posto08_IQM', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    _ensure_nc_preview(file_path)
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    return jsonify(data)



@bp.route('/posto08_iqm/update', methods=['POST'])
def atualizar_posto08_iqm():
    """Append IQM inspector data and move checklist for IQE."""
    data = request.get_json() or {}
    obra = data.get('obra')
    ano = data.get('ano')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    dir_path = os.path.join(BASE_DIR, 'posto08_IQM')
    os.makedirs(dir_path, exist_ok=True)
    src_path = os.path.join(dir_path, f'checklist_{obra}.json')

    base = {}
    if os.path.exists(src_path):
        try:
            with open(src_path, 'r', encoding='utf-8') as f:
                base = json.load(f)
        except Exception:
            base = {}

    if 'obra' not in base:
        base['obra'] = obra
    if ano:
        base['ano'] = ano

    base['posto08_iqm'] = data.get('posto08_iqm', {})

    with open(src_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)

    dest_dir = os.path.join(BASE_DIR, 'posto08_IQE')
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    try:
        os.replace(src_path, dest_path)
    except OSError:
        pass

    _ensure_nc_preview(dest_path)
    return jsonify({'caminho': dest_path})


@bp.route('/posto08_iqe/projects', methods=['GET'])
def listar_posto08_iqe_projetos():
    """List available IQE checklists."""
    dir_path = os.path.join(BASE_DIR, 'posto08_IQE')
    if not os.path.isdir(dir_path):
        return jsonify({'projetos': []})

    arquivos = [f for f in os.listdir(dir_path) if f.endswith('.json')]
    projetos = []
    for nome in sorted(arquivos):
        caminho = os.path.join(dir_path, nome)
        try:
            with open(caminho, 'r', encoding='utf-8') as f:
                data = json.load(f)
            projetos.append(
                {
                    'arquivo': nome,
                    'obra': data.get('obra', os.path.splitext(nome)[0]),
                    'ano': data.get('ano', ''),
                }
            )
        except Exception:
            continue
    return jsonify({'projetos': projetos})


@bp.route('/posto08_iqe/checklist', methods=['GET'])
def obter_posto08_iqe_checklist():
    """Return full IQE checklist for a given obra."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    file_path = os.path.join(BASE_DIR, 'posto08_IQE', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    return jsonify(data)


@bp.route('/posto08_iqe/upload', methods=['POST'])
def posto08_iqe_upload():
    """Store IQE inspector checklist and remove IQM file."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400
    dir_path = os.path.join(BASE_DIR, 'posto08_IQE')
    os.makedirs(dir_path, exist_ok=True)
    file_path = os.path.join(dir_path, f'checklist_{obra}.json')
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    src_path = os.path.join(BASE_DIR, 'posto08_IQM', f'checklist_{obra}.json')
    try:
        os.remove(src_path)
    except OSError:
        pass
    _ensure_nc_preview(file_path)
    return jsonify({'caminho': file_path})


@bp.route('/posto08_teste/upload', methods=['POST'])
def posto08_teste_upload():
    """Append IQE answers and move checklist to POSTO08_TESTE."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(BASE_DIR, 'posto08_IQE', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo base não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    itens = []
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        itens.append(
            {
                'numero': numero,
                'pergunta': pergunta,
                'respostas': {'inspetor': resposta},
            }
        )

    base['posto08_iqe'] = {
        'inspetor': data.get('inspetor'),
        'itens': itens,
    }

    dest_dir = os.path.join(BASE_DIR, 'POSTO08_TESTE')
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    _ensure_nc_preview(dest_path)
    return jsonify({'caminho': dest_path})


@bp.route('/posto08_teste/projects', methods=['GET'])
def listar_posto08_teste_projetos():
    """List checklists awaiting electrical tests."""
    dir_path = os.path.join(BASE_DIR, 'POSTO08_TESTE')
    if not os.path.isdir(dir_path):
        return jsonify({'projetos': []})
    arquivos = [f for f in os.listdir(dir_path) if f.endswith('.json')]
    projetos = []
    for nome in sorted(arquivos):
        caminho = os.path.join(dir_path, nome)
        try:
            with open(caminho, 'r', encoding='utf-8') as f:
                data = json.load(f)
            projetos.append(
                {
                    'arquivo': nome,
                    'obra': data.get('obra', os.path.splitext(nome)[0]),
                    'ano': data.get('ano', ''),
                }
            )
        except Exception:
            continue
    return jsonify({'projetos': projetos})


@bp.route('/posto08_teste/checklist', methods=['GET'])
def obter_posto08_teste_checklist():
    """Return POSTO08_TESTE checklist for a given obra."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400
    file_path = os.path.join(BASE_DIR, 'POSTO08_TESTE', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404
    _ensure_nc_preview(file_path)
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    return jsonify(data)


@bp.route('/posto08_teste/update', methods=['POST'])
def posto08_teste_update():
    """Store test results and move checklist to EXPEDICAO."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400
    src_path = os.path.join(BASE_DIR, 'POSTO08_TESTE', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo base não encontrado'}), 404
    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    itens = []
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        itens.append({'numero': numero, 'pergunta': pergunta, 'respostas': {'inspetor': resposta}})

    base['posto08_teste'] = {
        'inspetor': data.get('inspetor'),
        'itens': itens,
    }

    dest_dir = os.path.join(BASE_DIR, 'EXPEDICAO')
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    _ensure_nc_preview(dest_path)
    return jsonify({'caminho': dest_path})


  
  
@bp.route('/posto02/upload', methods=['POST'])
def posto02_upload():
    """Append Posto02 produção checklist and move it for inspection."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(BASE_DIR, 'Posto02_Oficina', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    itens: list = []
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        itens.append({
            'numero': numero,
            'pergunta': pergunta,
            'respostas': {'produção': resposta},
        })

    base['posto02'] = {
        'produção': data.get('produção'),
        'itens': itens,
    }

    insp_dir = os.path.join(BASE_DIR, 'Posto02_Oficina', 'Posto02_Oficina_Inspetor')
    os.makedirs(insp_dir, exist_ok=True)
    dest_path = os.path.join(insp_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path})


@bp.route('/posto02/insp/projects', methods=['GET'])
def listar_posto02_insp_proj():
    """List projects awaiting inspector review."""
    dir_path = os.path.join(BASE_DIR, 'Posto02_Oficina', 'Posto02_Oficina_Inspetor')
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


@bp.route('/posto02/insp/upload', methods=['POST'])
def posto02_insp_upload():
    """Process inspector answers and advance or return checklist."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(BASE_DIR, 'Posto02_Oficina', 'Posto02_Oficina_Inspetor', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    prod_itens = {item.get('numero'): item for item in base.get('posto02', {}).get('itens', [])}
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        entry = prod_itens.setdefault(numero, {'numero': numero, 'pergunta': pergunta, 'respostas': {}})
        entry['pergunta'] = entry.get('pergunta') or pergunta
        entry.setdefault('respostas', {})['inspetor'] = resposta

    divergencias = []
    for entry in prod_itens.values():
        resp_prod = entry.get('respostas', {}).get('produção')
        resp_insp = entry.get('respostas', {}).get('inspetor')
        if resp_prod is not None and resp_insp is not None and resp_prod != resp_insp:
            divergencias.append({
                'numero': entry.get('numero'),
                'pergunta': entry.get('pergunta'),
                'produção': resp_prod,
                'inspetor': resp_insp,
            })

    base['posto02']['inspetor'] = data.get('inspetor')
    base['posto02']['itens'] = list(prod_itens.values())
    if divergencias:
        base['posto02']['divergencias'] = divergencias
        dest_dir = os.path.join(BASE_DIR, 'Posto02_Oficina')
    else:
        dest_dir = os.path.join(BASE_DIR, 'Posto03_Pre_montagem_01')
        base['posto02']['divergencias'] = []
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path, 'divergencias': divergencias})


@bp.route('/posto06_cab2/projects', methods=['GET'])
def listar_posto06_cab2_projetos():
    """List projects awaiting Cablagem 02 production."""
    dir_path = os.path.join(BASE_DIR, 'POSTO06_1_06Cablagem02')
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


@bp.route('/posto06_cab2/checklist', methods=['GET'])
def obter_posto06_cab2_checklist():
    """Return full checklist data for a given obra in Cablagem 02."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    file_path = os.path.join(BASE_DIR, 'POSTO06_1_06Cablagem02', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    return jsonify(data)


@bp.route('/posto06_cab2/upload', methods=['POST'])
def posto06_cab2_upload():
    """Append Cablagem 02 checklist and move it for inspection."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(BASE_DIR, 'POSTO06_1_06Cablagem02', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    itens = []
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        itens.append({
            'numero': numero,
            'pergunta': pergunta,
            'respostas': {'montador': resposta},
        })

    base['posto06_cablagem_02'] = {
        'montador': data.get('montador'),
        'itens': itens,
    }

    insp_dir = os.path.join(BASE_DIR, 'POSTO06_1_06Cablagem02', 'POSTO06_1_06Cablagem02_inspetor')
    os.makedirs(insp_dir, exist_ok=True)
    dest_path = os.path.join(insp_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path})


@bp.route('/posto06_cab2/insp/projects', methods=['GET'])
def listar_posto06_cab2_insp_proj():
    """List projects awaiting Cablagem 02 inspection."""
    dir_path = os.path.join(BASE_DIR, 'POSTO06_1_06Cablagem02', 'POSTO06_1_06Cablagem02_inspetor')
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


@bp.route('/posto06_cab2/insp/upload', methods=['POST'])
def posto06_cab2_insp_upload():
    """Process inspector answers for Cablagem 02 and advance or return checklist."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(
        BASE_DIR,
        'POSTO06_1_06Cablagem02',
        'POSTO06_1_06Cablagem02_inspetor',
        f'checklist_{obra}.json',
    )
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    prod_itens = {
        item.get('numero'): item
        for item in base.get('posto06_cablagem_02', {}).get('itens', [])
    }
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        entry = prod_itens.setdefault(
            numero, {'numero': numero, 'pergunta': pergunta, 'respostas': {}}
        )
        entry['pergunta'] = entry.get('pergunta') or pergunta
        entry.setdefault('respostas', {})['inspetor'] = resposta

    divergencias = []
    for entry in prod_itens.values():
        resp_mont = entry.get('respostas', {}).get('montador')
        resp_insp = entry.get('respostas', {}).get('inspetor')
        if resp_mont is not None and resp_insp is not None and resp_mont != resp_insp:
            divergencias.append({
                'numero': entry.get('numero'),
                'pergunta': entry.get('pergunta'),
                'montador': resp_mont,
                'inspetor': resp_insp,
            })

    base['posto06_cablagem_02']['inspetor'] = data.get('inspetor')
    base['posto06_cablagem_02']['itens'] = list(prod_itens.values())
    if divergencias:
        base['posto06_cablagem_02']['divergencias'] = divergencias
        dest_dir = os.path.join(BASE_DIR, 'POSTO06_1_06Cablagem02')
    else:
        base['posto06_cablagem_02']['divergencias'] = []
        dest_dir = os.path.join(BASE_DIR, 'posto08_IQM')
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path, 'divergencias': divergencias})



@bp.route('/posto05/projects', methods=['GET'])
def listar_posto05_projetos():
    """List projects awaiting Cablagem 01 production."""
    dir_path = os.path.join(BASE_DIR, 'Posto05_cablagem_01')
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


@bp.route('/posto05/checklist', methods=['GET'])
def obter_posto05_checklist():
    """Return full checklist data for a given obra in Cablagem 01."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    file_path = os.path.join(BASE_DIR, 'Posto05_cablagem_01', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    return jsonify(data)


@bp.route('/posto05/upload', methods=['POST'])
def posto05_upload():
    """Append Cablagem 01 checklist and move it for inspection."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(BASE_DIR, 'Posto05_cablagem_01', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    itens = []
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        itens.append({
            'numero': numero,
            'pergunta': pergunta,
            'respostas': {'montador': resposta},
        })

    base['posto05_cablagem_01'] = {
        'montador': data.get('montador'),
        'itens': itens,
    }

    insp_dir = os.path.join(BASE_DIR, 'Posto05_cablagem_01', 'Posto05_cablagem_01_inspetor')
    os.makedirs(insp_dir, exist_ok=True)
    dest_path = os.path.join(insp_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path})


@bp.route('/posto05/insp/projects', methods=['GET'])
def listar_posto05_insp_proj():
    """List projects awaiting Cablagem 01 inspection."""
    dir_path = os.path.join(BASE_DIR, 'Posto05_cablagem_01', 'Posto05_cablagem_01_inspetor')
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


@bp.route('/posto05/insp/upload', methods=['POST'])

def posto05_insp_upload():
    """Process inspector answers and advance or return checklist."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(
        BASE_DIR, 'Posto05_cablagem_01', 'Posto05_cablagem_01_inspetor', f'checklist_{obra}.json'
    )
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    prod_itens = {
        item.get('numero'): item
        for item in base.get('posto05_cablagem_01', {}).get('itens', [])
    }
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        entry = prod_itens.setdefault(numero, {'numero': numero, 'pergunta': pergunta, 'respostas': {}})
        entry['pergunta'] = entry.get('pergunta') or pergunta
        entry.setdefault('respostas', {})['inspetor'] = resposta

    divergencias = []
    for entry in prod_itens.values():
        resp_mont = entry.get('respostas', {}).get('montador')
        resp_insp = entry.get('respostas', {}).get('inspetor')
        if resp_mont is not None and resp_insp is not None and resp_mont != resp_insp:
            divergencias.append({
                'numero': entry.get('numero'),
                'pergunta': entry.get('pergunta'),
                'montador': resp_mont,
                'inspetor': resp_insp,
            })

    base['posto05_cablagem_01']['inspetor'] = data.get('inspetor')
    base['posto05_cablagem_01']['itens'] = list(prod_itens.values())
    if divergencias:
        base['posto05_cablagem_01']['divergencias'] = divergencias
        dest_dir = os.path.join(BASE_DIR, 'Posto05_cablagem_01')
    else:
        base['posto05_cablagem_01']['divergencias'] = []
        dest_dir = os.path.join(BASE_DIR, 'Posto06_Pre_montagem_02')
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path, 'divergencias': divergencias})


@bp.route('/posto06_pre/projects', methods=['GET'])
def listar_posto06_pre_projetos():
    """List projects awaiting Pre-montagem 02 production."""
    dir_path = os.path.join(BASE_DIR, 'Posto06_Pre_montagem_02')
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


@bp.route('/posto06_pre/checklist', methods=['GET'])
def obter_posto06_pre_checklist():
    """Return full checklist data for a given obra in Pre-montagem 02."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    file_path = os.path.join(BASE_DIR, 'Posto06_Pre_montagem_02', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    return jsonify(data)


@bp.route('/posto06_pre/upload', methods=['POST'])
def posto06_pre_upload():
    """Append Pre-montagem 02 checklist and move it for inspection."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(BASE_DIR, 'Posto06_Pre_montagem_02', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    itens: list = []
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        itens.append({
            'numero': numero,
            'pergunta': pergunta,
            'respostas': {'montador': resposta},
        })

    base['posto06_pre_montagem_02'] = {
        'montador': data.get('montador'),
        'itens': itens,
    }

    insp_dir = os.path.join(
        BASE_DIR, 'Posto06_Pre_montagem_02', 'Posto06_Pre_montagem_02_inspetor'
    )
    os.makedirs(insp_dir, exist_ok=True)
    dest_path = os.path.join(insp_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path})


@bp.route('/posto06_pre/insp/projects', methods=['GET'])
def listar_posto06_pre_insp_proj():
    """List projects awaiting Pre-montagem 02 inspection."""
    dir_path = os.path.join(
        BASE_DIR, 'Posto06_Pre_montagem_02', 'Posto06_Pre_montagem_02_inspetor'
    )
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


@bp.route('/posto06_pre/insp/upload', methods=['POST'])
def posto06_pre_insp_upload():
    """Process inspector answers for Pre-montagem 02 and advance or return checklist."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(
        BASE_DIR,
        'Posto06_Pre_montagem_02',
        'Posto06_Pre_montagem_02_inspetor',
        f'checklist_{obra}.json',
    )
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    prod_itens = {
        item.get('numero'): item
        for item in base.get('posto06_pre_montagem_02', {}).get('itens', [])
    }
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        entry = prod_itens.setdefault(
            numero, {'numero': numero, 'pergunta': pergunta, 'respostas': {}}
        )
        entry['pergunta'] = entry.get('pergunta') or pergunta
        entry.setdefault('respostas', {})['inspetor'] = resposta

    divergencias = []
    for entry in prod_itens.values():
        resp_mont = entry.get('respostas', {}).get('montador')
        resp_insp = entry.get('respostas', {}).get('inspetor')
        if resp_mont is not None and resp_insp is not None and resp_mont != resp_insp:
            divergencias.append({
                'numero': entry.get('numero'),
                'pergunta': entry.get('pergunta'),
                'montador': resp_mont,
                'inspetor': resp_insp,
            })

    base['posto06_pre_montagem_02']['inspetor'] = data.get('inspetor')
    base['posto06_pre_montagem_02']['itens'] = list(prod_itens.values())
    if divergencias:
        base['posto06_pre_montagem_02']['divergencias'] = divergencias
        dest_dir = os.path.join(BASE_DIR, 'Posto06_Pre_montagem_02')
    else:
        base['posto06_pre_montagem_02']['divergencias'] = []
        dest_dir = os.path.join(BASE_DIR, 'POSTO06_1_06Cablagem02')
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path, 'divergencias': divergencias})



@bp.route('/posto04/projects', methods=['GET'])
def listar_posto04_projetos():
    """List projects awaiting Barramento production."""
    dir_path = os.path.join(BASE_DIR, 'POSTO_04_BARRAMENTO')
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


@bp.route('/posto04/checklist', methods=['GET'])
def obter_posto04_checklist():
    """Return full checklist data for a given obra in Barramento."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    file_path = os.path.join(BASE_DIR, 'POSTO_04_BARRAMENTO', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    return jsonify(data)


@bp.route('/posto04/upload', methods=['POST'])
def posto04_upload():
    """Append Barramento checklist and move it for inspection."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(BASE_DIR, 'POSTO_04_BARRAMENTO', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    itens = []
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        itens.append({
            'numero': numero,
            'pergunta': pergunta,
            'respostas': {'montador': resposta},
        })

    base['posto04_barramento'] = {
        'montador': data.get('montador'),
        'itens': itens,
    }

    insp_dir = os.path.join(BASE_DIR, 'POSTO_04_BARRAMENTO', 'POSTO_04_BARRAMENTO_Inspetor')
    os.makedirs(insp_dir, exist_ok=True)
    dest_path = os.path.join(insp_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path})


@bp.route('/posto04/insp/projects', methods=['GET'])
def listar_posto04_insp_proj():
    """List projects awaiting Barramento inspection."""
    dir_path = os.path.join(BASE_DIR, 'POSTO_04_BARRAMENTO', 'POSTO_04_BARRAMENTO_Inspetor')
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


@bp.route('/posto04/insp/upload', methods=['POST'])
def posto04_insp_upload():
    """Process inspector answers and advance or return checklist."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(
        BASE_DIR, 'POSTO_04_BARRAMENTO', 'POSTO_04_BARRAMENTO_Inspetor', f'checklist_{obra}.json'
    )
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    prod_itens = {
        item.get('numero'): item
        for item in base.get('posto04_barramento', {}).get('itens', [])
    }
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        entry = prod_itens.setdefault(numero, {'numero': numero, 'pergunta': pergunta, 'respostas': {}})
        entry['pergunta'] = entry.get('pergunta') or pergunta
        entry.setdefault('respostas', {})['inspetor'] = resposta

    divergencias = []
    for entry in prod_itens.values():
        resp_mont = entry.get('respostas', {}).get('montador')
        resp_insp = entry.get('respostas', {}).get('inspetor')
        if resp_mont is not None and resp_insp is not None and resp_mont != resp_insp:
            divergencias.append({
                'numero': entry.get('numero'),
                'pergunta': entry.get('pergunta'),
                'montador': resp_mont,
                'inspetor': resp_insp,
            })

    base['posto04_barramento']['inspetor'] = data.get('inspetor')
    base['posto04_barramento']['itens'] = list(prod_itens.values())
    if divergencias:
        base['posto04_barramento']['divergencias'] = divergencias
        dest_dir = os.path.join(BASE_DIR, 'POSTO_04_BARRAMENTO')
    else:
        base['posto04_barramento']['divergencias'] = []
        dest_dir = os.path.join(BASE_DIR, 'Posto05_cablagem_01')
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path, 'divergencias': divergencias})
  
@bp.route('/posto03_pre/projects', methods=['GET'])
def listar_posto03_pre_projetos():
    """List projects awaiting pre-montagem 01 production."""
    dir_path = os.path.join(BASE_DIR, 'Posto03_Pre_montagem_01')
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


@bp.route('/posto03_pre/checklist', methods=['GET'])
def obter_posto03_pre_checklist():
    """Return full checklist data for a given obra in Pre-montagem 01."""
    obra = request.args.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    file_path = os.path.join(BASE_DIR, 'Posto03_Pre_montagem_01', f'checklist_{obra}.json')
    if not os.path.exists(file_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    return jsonify(data)


@bp.route('/posto03_pre/upload', methods=['POST'])
def posto03_pre_upload():
    """Append Pre-montagem 01 checklist and move it for inspection."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(BASE_DIR, 'Posto03_Pre_montagem_01', f'checklist_{obra}.json')
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    itens: list = []
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        itens.append({
            'numero': numero,
            'pergunta': pergunta,
            'respostas': {'montador': resposta},
        })

    base['posto03_pre_montagem_01'] = {
        'montador': data.get('montador'),
        'itens': itens,
    }

    insp_dir = os.path.join(
        BASE_DIR, 'Posto03_Pre_montagem_01', 'Posto03_Pre_montagem_01_Inspetor'
    )
    os.makedirs(insp_dir, exist_ok=True)
    dest_path = os.path.join(insp_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path})


@bp.route('/posto03_pre/insp/projects', methods=['GET'])
def listar_posto03_pre_insp_proj():
    """List projects awaiting Pre-montagem 01 inspection."""
    dir_path = os.path.join(
        BASE_DIR, 'Posto03_Pre_montagem_01', 'Posto03_Pre_montagem_01_Inspetor'
    )
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


@bp.route('/posto03_pre/insp/upload', methods=['POST'])
def posto03_pre_insp_upload():
    """Process inspector answers and advance or return checklist."""
    data = request.get_json() or {}
    obra = data.get('obra')
    if not obra:
        return jsonify({'erro': 'obra obrigatória'}), 400

    src_path = os.path.join(
        BASE_DIR, 'Posto03_Pre_montagem_01', 'Posto03_Pre_montagem_01_Inspetor', f'checklist_{obra}.json'
    )
    if not os.path.exists(src_path):
        return jsonify({'erro': 'arquivo não encontrado'}), 404

    with open(src_path, 'r', encoding='utf-8') as f:
        base = json.load(f)

    prod_itens = {
        item.get('numero'): item
        for item in base.get('posto03_pre_montagem_01', {}).get('itens', [])
    }
    for item in data.get('itens', []):
        numero = item.get('numero')
        pergunta = item.get('pergunta')
        resposta = item.get('resposta') if isinstance(item.get('resposta'), list) else None
        entry = prod_itens.setdefault(
            numero, {'numero': numero, 'pergunta': pergunta, 'respostas': {}}
        )
        entry['pergunta'] = entry.get('pergunta') or pergunta
        entry.setdefault('respostas', {})['inspetor'] = resposta

    divergencias = []
    for entry in prod_itens.values():
        resp_mont = entry.get('respostas', {}).get('montador')
        resp_insp = entry.get('respostas', {}).get('inspetor')
        if resp_mont is not None and resp_insp is not None and resp_mont != resp_insp:
            divergencias.append({
                'numero': entry.get('numero'),
                'pergunta': entry.get('pergunta'),
                'montador': resp_mont,
                'inspetor': resp_insp,
            })

    base['posto03_pre_montagem_01']['inspetor'] = data.get('inspetor')
    base['posto03_pre_montagem_01']['itens'] = list(prod_itens.values())
    if divergencias:
        base['posto03_pre_montagem_01']['divergencias'] = divergencias
        dest_dir = os.path.join(BASE_DIR, 'Posto03_Pre_montagem_01')
    else:
        base['posto03_pre_montagem_01']['divergencias'] = []
        dest_dir = os.path.join(BASE_DIR, 'POSTO_04_BARRAMENTO')
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, f'checklist_{obra}.json')
    with open(dest_path, 'w', encoding='utf-8') as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
    try:
        os.remove(src_path)
    except OSError:
        pass
    return jsonify({'caminho': dest_path, 'divergencias': divergencias})


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

# utilidades de mesclagem
from .merge_checklists import merge_checklists, merge_directory, find_mismatches
from .merge_checklists import move_matching_checklists
