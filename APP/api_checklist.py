from flask import Flask, jsonify, send_from_directory, abort, request
import os, datetime, traceback, sqlite3

# ---------------------------------------------------------
# Configurações de banco de dados para “compras”
DB_DIR  = r"C:\Users\Administrador\Desktop\APP\database"
DB_PATH = os.path.join(DB_DIR, "compras.db")
os.makedirs(DB_DIR, exist_ok=True)

def init_compras_db():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("""
        CREATE TABLE IF NOT EXISTS itens_compras (
            id        INTEGER PRIMARY KEY AUTOINCREMENT,
            relpath   TEXT,
            codigo    TEXT,
            qtd       TEXT
        )
    """)
    conn.commit()
    conn.close()

# ---------------------------------------------------------
# Cria o Flask app
app = Flask(__name__, static_folder=None, static_url_path='')

# Inicializa o banco de compras
init_compras_db()

# ---------------------------------------------------------
# Ajuste aqui se for usar UNC em vez de letra de unidade
PASTA_RAIZ = r"F:\03 - ENGENHARIA\03 - PRODUCAO"

# ---------------------------------------------------------
# Rota única de “compras”
@app.route('/compras', methods=['POST'])
def compras():
    data = request.get_json()
    if not data or 'relpath' not in data or 'itens' not in data:
        abort(400, "Payload inválido")

    relpath = data['relpath']
    itens   = data['itens']  # lista de dicts {"codigo":..., "qtd":...}

    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    # opcional: limpa itens antigos deste relpath
    c.execute("DELETE FROM itens_compras WHERE relpath = ?", (relpath,))
    for itm in itens:
        c.execute(
            "INSERT INTO itens_compras (relpath, codigo, qtd) VALUES (?, ?, ?)",
            (relpath, itm.get('codigo'), itm.get('qtd'))
        )
    conn.commit()
    conn.close()

    return jsonify({"success": True, "count": len(itens)})

# ---------------------------------------------------------
# Log de todas as requisições
@app.before_request
def log_request():
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{now}] {request.method} {request.path}")

# ---------------------------------------------------------
# Tratamento central de exceções
@app.errorhandler(Exception)
def handle_exception(e):
    traceback.print_exc()
    code = getattr(e, 'code', 500)
    return jsonify({"error": str(e), "code": code}), code

# ---------------------------------------------------------
@app.route('/', methods=['GET'])
def raiz():
    return jsonify({
        "status": "ok",
        "endpoints": [
            "/projetos",
            "/pastas/<tipo>",
            "/checklists",
            "/arquivos/<path:relpath>/<nome_arquivo>",
            "/arquivos/upload",
            "/compras"
        ]
    })

# ---------------------------------------------------------
@app.route('/projetos', methods=['GET'])
def listar_projetos():
    entradas = []
    for root, dirs, files in os.walk(PASTA_RAIZ):
        rel = os.path.relpath(root, PASTA_RAIZ).replace("\\", "/")
        partes = rel.split("/")
        if len(partes) >= 2:
            ano     = partes[0]
            projeto = partes[1]
            subproj = partes[2] if len(partes) >= 3 else None
            entradas.append((ano, projeto, subproj))

    anos_map = {}
    for ano, projeto, sub in entradas:
        proj_map = anos_map.setdefault(ano, {})
        subs = proj_map.setdefault(projeto, set())
        if sub:
            subs.add(sub)

    resultado = []
    for ano, projetos in sorted(anos_map.items()):
        projetos_list = []
        for proj, subs in sorted(projetos.items()):
            projetos_list.append({
                "projeto":     proj,
                "subprojetos": sorted(subs)
            })
        resultado.append({
            "ano":      ano,
            "projetos": projetos_list
        })

    return jsonify(resultado)

# ---------------------------------------------------------
PASTAS_PERMITIDAS = {
    "asbuilt":                "AS BUILT",
    "layout":                 "LAYOUT",
    "identificacoes":         "IDENTIFICAÇÕES",
    "projeto_eletromecanico": "PROJETO ELETROMECÂNICO",
    "checklist":              "CHECKLIST",
    "checklists":             "CHECKLISTS",
    "fotos":                  "FOTOS"
}

@app.route('/pastas/<tipo>', methods=['GET'])
def listar_por_tipo(tipo):
    chave = tipo.lower()
    if chave not in PASTAS_PERMITIDAS:
        abort(404, description=f"Tipo de pasta '{tipo}' não suportado.")
    nome_desejado = PASTAS_PERMITIDAS[chave].upper()
    resultado = []

    for root, dirs, files in os.walk(PASTA_RAIZ):
        if os.path.basename(root).upper() == nome_desejado:
            if not files:
                continue
            rel = os.path.relpath(root, PASTA_RAIZ).replace("\\", "/")
            partes = rel.split("/")
            resultado.append({
                "tipo":       nome_desejado,
                "ano":        partes[0] if len(partes) > 0 else None,
                "projeto":    partes[1] if len(partes) > 1 else None,
                "subprojeto": partes[2] if len(partes) > 2 else None,
                "arquivos":   files
            })
    return jsonify(resultado)

# ---------------------------------------------------------
@app.route('/checklists', methods=['GET'])
def listar_checklists():
    resultado = []
    for root, dirs, files in os.walk(PASTA_RAIZ):
        nome_pasta = os.path.basename(root).upper()
        if nome_pasta in ("CHECKLIST", "CHECKLISTS"):
            arquivos = [f for f in files if f.lower().endswith((".txt", ".pdf"))]
            if not arquivos:
                continue

            rel = os.path.relpath(root, PASTA_RAIZ).replace("\\", "/")
            partes = rel.split("/")
            ref_path = os.path.join(root, "REFERENCIA.txt")
            resultado.append({
                "ano":               partes[0],
                "projeto":           partes[1] if len(partes)>1 else None,
                "subprojeto":        partes[2] if len(partes)>2 else None,
                "relpath":           rel,
                "arquivos":          arquivos,
                "referenciaExiste":  os.path.isfile(ref_path),
                "referenciaVazia":   os.path.isfile(ref_path) and os.path.getsize(ref_path)==0
            })
    return jsonify(resultado)

# ---------------------------------------------------------
@app.route('/arquivos/<path:relpath>/<nome_arquivo>', methods=['GET'])
def baixar_arquivo(relpath, nome_arquivo):
    pasta = os.path.join(PASTA_RAIZ, relpath.replace("/", os.sep))
    arquivo_path = os.path.join(pasta, nome_arquivo)
    if not os.path.isfile(arquivo_path):
        abort(404, "Arquivo não encontrado")
    return send_from_directory(pasta, nome_arquivo, as_attachment=True)

# ---------------------------------------------------------
@app.route('/arquivos/upload', methods=['POST'])
def upload_arquivo():
    projeto = request.form.get('projeto')
    arquivo = request.files.get('arquivo')
    if not projeto or not arquivo:
        abort(400, "Campos 'projeto' e 'arquivo' são obrigatórios")
    pasta_destino = os.path.join(PASTA_RAIZ, *projeto.split("/"))
    os.makedirs(pasta_destino, exist_ok=True)
    arquivo.save(os.path.join(pasta_destino, arquivo.filename))
    return jsonify({"success": True})

# ---------------------------------------------------------
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
