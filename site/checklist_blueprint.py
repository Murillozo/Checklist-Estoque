import os
import re
import json
from datetime import datetime
from flask import Blueprint, jsonify, render_template, request

bp = Blueprint('checklist', __name__, template_folder='templates')

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), 'json_api'))
ALLOWED_RE = re.compile(r'^[\w-]+$')
MAX_FILE_SIZE = 2 * 1024 * 1024  # 2MB


def safe_join(root, *paths):
    root = os.path.abspath(root)
    path = os.path.abspath(os.path.join(root, *paths))
    if not path.startswith(root + os.sep):
        raise ValueError('Caminho inválido')
    return path


def _validate_part(part: str) -> bool:
    return bool(ALLOWED_RE.fullmatch(part))


@bp.route('/')
def index():
    return render_template('checklist.html')


@bp.route('/api/folders')
def list_folders():
    try:
        folders = [
            d for d in os.listdir(BASE_DIR)
            if os.path.isdir(os.path.join(BASE_DIR, d))
        ]
    except OSError as e:
        return jsonify({'error': str(e)}), 500
    folders.sort()
    return jsonify(folders)


@bp.route('/api/files')
def list_files():
    folder = request.args.get('folder', '')
    if not _validate_part(folder):
        return jsonify({'error': 'Pasta inválida'}), 400
    try:
        folder_path = safe_join(BASE_DIR, folder)
        entries = []
        for name in os.listdir(folder_path):
            if name.lower().endswith('.json'):
                fp = os.path.join(folder_path, name)
                st = os.stat(fp)
                entries.append({
                    'name': name,
                    'size': st.st_size,
                    'mtime': st.st_mtime,
                    'mtime_h': datetime.fromtimestamp(st.st_mtime).isoformat()
                })
        entries.sort(key=lambda x: x['name'])
        return jsonify(entries)
    except FileNotFoundError:
        return jsonify({'error': 'Pasta não encontrada'}), 404
    except OSError as e:
        return jsonify({'error': str(e)}), 500


@bp.route('/api/file')
def get_file():
    folder = request.args.get('folder', '')
    name = request.args.get('name', '')
    if not (_validate_part(folder) and name.lower().endswith('.json') and _validate_part(name[:-5])):
        return jsonify({'error': 'Parâmetros inválidos'}), 400
    try:
        file_path = safe_join(BASE_DIR, folder, name)
        st = os.stat(file_path)
        if st.st_size > MAX_FILE_SIZE:
            return jsonify({'error': 'Arquivo muito grande'}), 413
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return jsonify(data)
    except FileNotFoundError:
        return jsonify({'error': 'Arquivo não encontrado'}), 404
    except PermissionError:
        return jsonify({'error': 'Permissão negada'}), 403
    except json.JSONDecodeError:
        return jsonify({'error': 'JSON inválido'}), 400
    except OSError as e:
        return jsonify({'error': str(e)}), 500
