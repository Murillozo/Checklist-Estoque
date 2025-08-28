# projetista/__init__.py
from flask import Blueprint, render_template, request, redirect, url_for, flash
from models import db, Solicitacao, Item, User, AuthorizedIP, EstoqueSolicitacao, EstoqueItem
from flask_login import login_required, current_user
import json
import io
from flask import send_file
import openpyxl
import pytz
from collections import Counter
from flask import jsonify
import os
from datetime import datetime
from werkzeug.utils import secure_filename
import urllib.parse
from fpdf import FPDF

bp = Blueprint('projetista', __name__)

# Diretório onde os arquivos de checklist (JSON) são salvos.
# Permite sobrepor via variável de ambiente CHECKLIST_DIR.
CHECKLIST_DIR = os.environ.get(
    "CHECKLIST_DIR",
    os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'json_api')),
)

# Diretório base onde os projetos são armazenados no servidor
BASE_PRODUCAO = r"F:\03 - ENGENHARIA\03 - PRODUCAO"

# Diretório onde as fotos são salvas (pode ser sobrescrito via FOTOS_DIR)
# Por padrão aponta para a estrutura de projetos utilizada no servidor
FOTOS_DIR = os.environ.get(
    "FOTOS_DIR",
    r"F:\\03 - ENGENHARIA\\02 - PROJETOS"
)

# Garante que o diretório base exista para evitar erros de leitura
os.makedirs(FOTOS_DIR, exist_ok=True)

# Subpastas que devem ser criadas para cada obra
SUBPASTAS_OBRA = [
    'AS BUILT',
    'CHECKLIST',
    'FOTOS',
    'IDENTIFICAÇÕES',
    'LAYOUT',
    'PROJETO ELETROMECÂNICO',
]


@bp.route('/')
@login_required
def index():
    consulta = Solicitacao.query.order_by(Solicitacao.data.desc()).all()

    # Mantém apenas a ocorrência mais recente de cada obra
    unicas = []
    vistas = set()
    for sol in consulta:
        if sol.obra not in vistas:
            unicas.append(sol)
            vistas.add(sol.obra)

    # prepara lista de pendências para exibição
    for sol in unicas:
        try:
            sol.pendencias_list = json.loads(sol.pendencias or "[]")
        except json.JSONDecodeError:
            sol.pendencias_list = []

    return render_template('index.html', solicitacoes=unicas)

@bp.route('/solicitacoes')
@login_required
def solicitacoes():
    consulta = Solicitacao.query.order_by(Solicitacao.data.asc()).all()
    tz = pytz.timezone('America/Sao_Paulo')

    for sol in consulta:
        # converte horário
        utc_dt = sol.data.replace(tzinfo=pytz.UTC)
        sol.local_time = utc_dt.astimezone(tz)

        # agrupa itens repetidos
        agrupados = {}
        for it in sol.itens:
            agrupados[it.referencia] = agrupados.get(it.referencia, 0) + it.quantidade
        sol.itens_agrupados = list(agrupados.items())

    return render_template('solicitacoes.html', solicitacoes=consulta)


@bp.route('/iniciar_projeto')
@login_required
def iniciar_projeto():
    """Exibe os projetos divididos por status."""
    consulta = Solicitacao.query.order_by(Solicitacao.data.desc()).all()

    for sol in consulta:
        try:
            sol.pendencias_list = json.loads(sol.pendencias or "[]")
        except json.JSONDecodeError:
            sol.pendencias_list = []

    analise = [s for s in consulta if s.status == 'analise']
    aprovado = [s for s in consulta if s.status == 'aprovado']
    compras = [s for s in consulta if s.status == 'compras']

    return render_template(
        'iniciar_projeto.html',
        analise=analise,
        aprovado=aprovado,
        compras=compras,
    )


@bp.route('/solicitacao/nova', methods=['GET', 'POST'])
@login_required
def nova_solicitacao():
    # tenta descobrir os anos e obras disponíveis no servidor
    try:
        anos = [d for d in os.listdir(BASE_PRODUCAO)
                if os.path.isdir(os.path.join(BASE_PRODUCAO, d))]
        anos.sort()
        obras_por_ano = {}
        for ano in anos:
            ano_dir = os.path.join(BASE_PRODUCAO, ano)
            obras = [d for d in os.listdir(ano_dir)
                     if os.path.isdir(os.path.join(ano_dir, d))]
            obras.sort()
            obras_por_ano[ano] = obras
    except OSError:
        # se o diretório não estiver acessível, usa o ano atual
        ano_atual = str(datetime.now().year)
        anos = [ano_atual]
        obras_por_ano = {ano_atual: []}

    if request.method == 'POST':
        obra = request.form['obra'].strip()
        ano = request.form.get('ano', '').strip()
        data_entrega_str = request.form['data_entrega']
        data_entrega = datetime.strptime(data_entrega_str, '%Y-%m-%d').date()

        sol = Solicitacao(obra=obra, data_entrega=data_entrega)
        db.session.add(sol)
        db.session.flush()

        # 1) Se enviou um arquivo .xlsx, use ele:
        file = request.files.get('xlsx_file')
        if file and file.filename.lower().endswith(('.xls', '.xlsx')):
            wb = openpyxl.load_workbook(io.BytesIO(file.read()), data_only=True)
            ws = wb.active
            # espera cabeçalho em linha 1: Referência | Quantidade
            for row in ws.iter_rows(min_row=2, values_only=True):
                ref, qt = row[0], row[1]
                if not ref or not qt:
                    continue
                item = Item(
                    solicitacao_id=sol.id,
                    referencia=str(ref).strip(),
                    quantidade=int(qt),
                    status='Nao iniciada'
                )
                db.session.add(item)

        # 2) Caso não tenha enviado arquivo, tenta textareas (legacy)
        else:
            refs = request.form.get('referencias', '').strip().splitlines()
            qts = request.form.get('quantidades', '').strip().splitlines()
            for ref, qt in zip(refs, qts):
                ref, qt = ref.strip(), qt.strip()
                if not ref or not qt:
                    continue
                item = Item(
                    solicitacao_id=sol.id,
                    referencia=ref,
                    quantidade=int(qt),
                    status='Nao iniciada'
                )
                db.session.add(item)

        db.session.commit()

        # cria as pastas da obra no servidor, se possível
        if ano:
            try:
                obra_dir = os.path.join(BASE_PRODUCAO, ano, obra)
                os.makedirs(obra_dir, exist_ok=True)

                # subpastas da obra principal
                for nome in SUBPASTAS_OBRA:
                    os.makedirs(os.path.join(obra_dir, nome), exist_ok=True)
            except OSError:
                # ignora falhas de criação de diretório
                pass

        flash('Solicitação criada com sucesso!', 'success')
        return redirect(url_for('projetista.solicitacoes'))

    return render_template('nova_solicitacao.html', anos=anos, obras_por_ano=obras_por_ano)


@bp.route('/verificar_estoque', methods=['GET', 'POST'])
@login_required
def verificar_estoque():
    if request.method == 'POST':
        itens = []
        file = request.files.get('xlsx_file')
        if file and file.filename.lower().endswith(('.xls', '.xlsx')):
            wb = openpyxl.load_workbook(io.BytesIO(file.read()), data_only=True)
            ws = wb.active
            for row in ws.iter_rows(min_row=2, values_only=True):
                ref, qt = row[0], row[1]
                if not ref or not qt:
                    continue
                itens.append({'referencia': str(ref).strip(), 'quantidade': int(qt)})
        else:
            refs = request.form.get('referencias', '').strip().splitlines()
            qts = request.form.get('quantidades', '').strip().splitlines()
            for ref, qt in zip(refs, qts):
                ref, qt = ref.strip(), qt.strip()
                if not ref or not qt:
                    continue
                itens.append({'referencia': ref, 'quantidade': int(qt)})

        if itens:
            sol = EstoqueSolicitacao()
            for it in itens:
                sol.itens.append(
                    EstoqueItem(
                        referencia=it['referencia'],
                        quantidade=it['quantidade']
                    )
                )
            db.session.add(sol)
            db.session.commit()
            flash('Solicitação registrada', 'success')
        return redirect(url_for('projetista.verificar_estoque'))

    solicitacoes = EstoqueSolicitacao.query.order_by(EstoqueSolicitacao.id.desc()).all()
    return render_template('verificar_estoque.html', solicitacoes=solicitacoes)


@bp.post('/verificar_estoque/<int:sol_id>/delete')
@login_required
def deletar_estoque_solicitacao(sol_id: int):
    """Remove uma solicitação de verificação de estoque."""
    sol = EstoqueSolicitacao.query.get_or_404(sol_id)
    db.session.delete(sol)
    db.session.commit()
    flash('Solicitação removida', 'success')
    return redirect(url_for('projetista.verificar_estoque'))


@bp.route('/subpastas', methods=['GET', 'POST'])
@login_required
def criar_subpastas():
    try:
        anos = [d for d in os.listdir(BASE_PRODUCAO)
                if os.path.isdir(os.path.join(BASE_PRODUCAO, d))]
    except OSError:
        anos = [str(datetime.now().year)]

    if request.method == 'POST':
        obra = request.form['obra'].strip()
        ano = request.form.get('ano', '').strip()
        subpastas_raw = request.form.get('subpastas', '0').strip()
        try:
            qtd_subpastas = int(subpastas_raw)
        except ValueError:
            qtd_subpastas = 0

        if ano and obra and qtd_subpastas > 0:
            try:
                obra_dir = os.path.join(BASE_PRODUCAO, ano, obra)
                for i in range(1, qtd_subpastas + 1):
                    sub_dir = os.path.join(obra_dir, f"{obra}.{i}")
                    for nome in SUBPASTAS_OBRA:
                        os.makedirs(os.path.join(sub_dir, nome), exist_ok=True)
                flash('Subpastas criadas com sucesso!', 'success')
            except OSError:
                flash('Falha ao criar subpastas.', 'danger')
        else:
            flash('Dados inválidos.', 'warning')
        return redirect(url_for('projetista.criar_subpastas'))

    return render_template('subpastas.html', anos=anos)


@bp.route('/comparador', methods=['GET', 'POST'])
@login_required
def comparador():
    # lista de obras únicas
    obras = [row[0] for row in db.session.query(Solicitacao.obra).distinct().all()]
    # todas as solicitações ordenadas por data ascendente
    solicitacoes = Solicitacao.query.order_by(Solicitacao.data.asc()).all()

    selecionada = None
    base = None
    nova = None
    adicionados = []
    removidos = []
    alterados = []

    if request.method == 'POST':
        selecionada = request.form.get('obra')
        id_base_raw = request.form.get('base')
        id_nova_raw = request.form.get('nova')

        # se só escolheu obra, mostra o form de IDs
        if selecionada and (not id_base_raw or not id_nova_raw):
            return render_template(
                'comparador.html',
                obras=obras,
                solicitacoes=solicitacoes,
                selecionada=selecionada
            )

        # converte para int
        id_base = int(id_base_raw)
        id_nova = int(id_nova_raw)

        # carrega objetos para comparar data
        sol_base = Solicitacao.query.get(id_base)
        sol_nova = Solicitacao.query.get(id_nova)

        # se base for mais recente que nova, troca
        if sol_base.data > sol_nova.data:
            sol_base, sol_nova = sol_nova, sol_base
            id_base, id_nova = id_nova, id_base

        # agrupa somando quantidades
        cnt_base = Counter()
        for it in Item.query.filter_by(solicitacao_id=sol_base.id):
            cnt_base[it.referencia] += it.quantidade

        cnt_nova = Counter()
        for it in Item.query.filter_by(solicitacao_id=sol_nova.id):
            cnt_nova[it.referencia] += it.quantidade

        # compara
        for ref in set(cnt_base) | set(cnt_nova):
            q0 = cnt_base.get(ref, 0)
            q1 = cnt_nova.get(ref, 0)
            if q0 == 0 and q1 > 0:
                adicionados.append((ref, q1))
            elif q1 == 0 and q0 > 0:
                removidos.append((ref, q0))
            elif q0 != q1:
                alterados.append((ref, q0, q1))

        return render_template(
            'comparador.html',
            obras=obras,
            solicitacoes=solicitacoes,
            selecionada=selecionada,
            base=id_base,
            nova=id_nova,
            adicionados=adicionados,
            removidos=removidos,
            alterados=alterados
        )

    # GET inicial
    return render_template(
        'comparador.html',
        obras=obras,
        solicitacoes=solicitacoes
    )
@bp.route('/template-solicitacao.xlsx')
def export_template():
    # Cria a planilha
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Solicitação"

    # Cabeçalhos
    ws.append(["Referência", "Quantidade"])

    # Você pode, opcionalmente, já deixar umas linhas de exemplo
    # ws.append(["ABC123", "10"])
    # ws.append(["DEF456", "5"])

    # Escreve num buffer em memória
    stream = io.BytesIO()
    wb.save(stream)
    stream.seek(0)

    # Envia como download
    return send_file(
        stream,
        as_attachment=True,
        download_name="template_solicitacao.xlsx",
        mimetype="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )


@bp.route('/api/solicitacoes')
@login_required
def api_listar_solicitacoes():
    resultados = []
    for sol in Solicitacao.query.order_by(Solicitacao.id.desc()).all():
        itens = [
            {
                "id": it.id,
                "referencia": it.referencia,
                "quantidade": it.quantidade,
                "status": it.status,
                "previsao_entrega": it.previsao_entrega.isoformat() if it.previsao_entrega else None,
            }
            for it in sol.itens
        ]
        resultados.append({
            "id": sol.id,
            "obra": sol.obra,
            "data": sol.data.isoformat(),
            "data_entrega": sol.data_entrega.isoformat(),
            "itens": itens,
            "status": sol.status,
            "pendencias": sol.pendencias
        })
    return jsonify(resultados)


@bp.route('/api/solicitacoes/<int:id>/aprovar', methods=['POST'])
@login_required
def api_aprovar(id):
    sol = Solicitacao.query.get_or_404(id)
    # Evita sobrescrever pendências já existentes quando a rota
    # de aprovação é chamada inadvertidamente. Assim, a solicitação
    # permanece com o status atual até que as pendências sejam
    # tratadas pelo setor responsável.
    if sol.pendencias and sol.pendencias != '[]':
        return jsonify({'ok': True})

    sol.status = 'aprovado'
    sol.pendencias = None
    db.session.commit()
    return jsonify({'ok': True})


@bp.route('/api/solicitacoes/<int:id>/compras', methods=['POST'])
@login_required
def api_compras(id):
    sol = Solicitacao.query.get_or_404(id)
    dados = request.get_json() or {}
    pendencias = dados.get('pendencias', [])

    # Se nenhuma pendência for enviada mas já existirem pendências
    # registradas, mantemos o status atual para evitar que a
    # solicitação seja marcada como completa de forma inadvertida.
    if not pendencias and sol.pendencias and sol.pendencias != '[]':
        return jsonify({'ok': True})

    total = len(sol.itens)
    concluido = total - len(pendencias)
    porcentagem = concluido / total if total else 0

    if porcentagem >= 0.8:
        sol.status = 'aprovado'
    elif sol.status != 'aprovado':
        sol.status = 'compras'
    sol.pendencias = json.dumps(pendencias)
    db.session.commit()
    return jsonify({'ok': True})


@bp.route('/checklist')
@login_required
def checklist_list():
    """Renderiza a interface de visualização dos checklists."""
    return render_template('checklist.html')


@bp.route('/checklist/pdf/<path:filename>')
@login_required
def checklist_pdf(filename):
    """Gera um PDF com base no checklist JSON informado."""
    caminho = os.path.join(CHECKLIST_DIR, filename)
    if not os.path.isfile(caminho):
        flash('Arquivo não encontrado.', 'danger')
        return redirect(url_for('projetista.checklist_list'))

    with open(caminho, encoding='utf-8') as f:
        dados = json.load(f)

    class ChecklistPDF(FPDF):
        def __init__(self, obra='', ano='', suprimento='', *args, **kwargs):
            super().__init__(*args, **kwargs)
            self.obra = obra
            self.ano = ano
            self.suprimento = suprimento

        def header(self):
            self.set_fill_color(33, 150, 243)
            self.rect(0, 0, self.w, 25, 'F')
            self.set_y(5)
            self.set_text_color(255, 255, 255)
            self.set_font('Arial', 'B', 16)
            self.cell(0, 8, 'Checklist', align='C')
            self.set_font('Arial', '', 10)
            self.ln(6)
            self.cell(
                0,
                5,
                f"Obra: {self.obra}   Ano: {self.ano}   Suprimento: {self.suprimento}",
                align='C',
            )
            self.ln(6)
            # posiciona o cursor abaixo da barra azul para todas as páginas
            self.set_y(40)
            self.set_text_color(0, 0, 0)

        def footer(self):
            self.set_y(-15)
            self.set_font('Arial', 'I', 8)
            self.set_text_color(128)
            self.cell(0, 10, f'Página {self.page_no()}/{{nb}}', align='C')

    pdf = ChecklistPDF(
        obra=dados.get('obra', ''),
        ano=dados.get('ano', ''),
        suprimento=dados.get('suprimento', ''),
    )
    pdf.alias_nb_pages()
    pdf.add_page()
    pdf.set_font("Arial", size=12)
    def coletar_itens(node, acumulador):
        """Coleta recursivamente todos os itens em qualquer nível do JSON."""
        if isinstance(node, dict):
            lista = node.get('itens')
            if isinstance(lista, list):
                for it in lista:
                    pergunta = it.get('pergunta', '')
                    respostas = it.get('respostas') or it.get('resposta', [])
                    if isinstance(respostas, dict):
                        resp_vals = []
                        for val in respostas.values():
                            if isinstance(val, list):
                                resp_vals.extend([str(v) for v in val])
                            elif val:
                                resp_vals.append(str(val))
                        resposta = ', '.join(resp_vals)
                    elif isinstance(respostas, list):
                        resposta = ', '.join(str(v) for v in respostas)
                    else:
                        resposta = str(respostas) if respostas else ''
                    acumulador.append({'pergunta': pergunta, 'resposta': resposta})
            for v in node.values():
                coletar_itens(v, acumulador)
        elif isinstance(node, list):
            for elem in node:
                coletar_itens(elem, acumulador)

    itens = []
    coletar_itens(dados, itens)
    for idx, item in enumerate(itens):
        if item['pergunta'].strip() == "1.15 - POLICARBONATO: Material em bom estado":
            itens.insert(idx + 1, {'pergunta': 'Posto - 02 MATERIAIS', 'resposta': ''})
            break

    col_widths = [95, 95]  # [coluna pergunta, coluna resposta]
    line_height = 8
    pdf.set_draw_color(50, 50, 100)
    pdf.set_fill_color(200, 200, 200)
    pdf.set_font("Arial", 'B', 11)
    pdf.cell(col_widths[0], line_height, "Pergunta", border=1, align='C', fill=True)
    pdf.cell(col_widths[1], line_height, "Resposta", border=1, align='C', fill=True, ln=1)
    pdf.set_font("Arial", size=10)
    for idx, item in enumerate(itens, 1):
        if idx % 2 == 0:
            pdf.set_fill_color(245, 245, 245)
        else:
            pdf.set_fill_color(255, 255, 255)
        if item['pergunta'] == 'Posto - 02 MATERIAIS':
            # linha destacada ocupando toda a largura da tabela
            pdf.cell(sum(col_widths), line_height, item['pergunta'], border=1, fill=True, ln=1)
        else:
            pdf.cell(col_widths[0], line_height, item['pergunta'], border=1, fill=True)
            pdf.cell(col_widths[1], line_height, item['resposta'], border=1, fill=True, ln=1)

    pdf_bytes = pdf.output(dest='S').encode('latin-1')
    return send_file(
        io.BytesIO(pdf_bytes),
        mimetype='application/pdf',
        as_attachment=True,
        download_name='checklist.pdf'
    )


@bp.route('/checklist/<path:filename>')
@login_required
def checklist_view(filename):
    caminho = os.path.join(CHECKLIST_DIR, filename)
    if not os.path.isfile(caminho):
        flash('Arquivo não encontrado.', 'danger')
        return redirect(url_for('projetista.checklist_list'))
    with open(caminho, encoding='utf-8') as f:
        dados = json.load(f)
    # verifica se existe revisão anterior para comparação
    obra = dados.get('obra', 'Desconhecida') or 'Desconhecida'
    safe_obra = "".join(c for c in obra if c.isalnum() or c in ('-','_')) or 'obra'
    todos = [n for n in os.listdir(CHECKLIST_DIR)
             if n.endswith('.json') and n.startswith(f"checklist_{safe_obra}_")]
    todos.sort()
    try:
        idx = todos.index(filename)
        prev_filename = todos[idx - 1] if idx > 0 else None
    except ValueError:
        prev_filename = None
    return render_template(
        'checklist_view.html', filename=filename, dados=dados, prev_filename=prev_filename
    )


@bp.route('/checklist/diff/<path:filename>')
@login_required
def checklist_diff(filename):
    """Exibe as diferenças entre o checklist selecionado e o anterior."""
    caminho = os.path.join(CHECKLIST_DIR, filename)
    if not os.path.isfile(caminho):
        flash('Arquivo não encontrado.', 'danger')
        return redirect(url_for('projetista.checklist_list'))

    with open(caminho, encoding='utf-8') as f:
        atual = json.load(f)

    obra = atual.get('obra', 'Desconhecida') or 'Desconhecida'
    safe_obra = "".join(c for c in obra if c.isalnum() or c in ('-','_')) or 'obra'

    # Localiza o checklist anterior para a mesma obra
    todos = [n for n in os.listdir(CHECKLIST_DIR)
             if n.endswith('.json') and n.startswith(f"checklist_{safe_obra}_")]
    todos.sort()
    try:
        idx = todos.index(filename)
    except ValueError:
        idx = -1

    if idx <= 0:
        flash('Não há checklist anterior para comparação.', 'warning')
        return redirect(url_for('projetista.checklist_view', filename=filename))

    anterior_nome = todos[idx - 1]
    caminho_ant = os.path.join(CHECKLIST_DIR, anterior_nome)
    with open(caminho_ant, encoding='utf-8') as f:
        anterior = json.load(f)

    antigos = {i['pergunta']: i.get('resposta', [])
               for i in anterior.get('itens', [])}
    novos = {i['pergunta']: i.get('resposta', [])
             for i in atual.get('itens', [])}

    diff = []
    perguntas = sorted(set(antigos) | set(novos))
    for pergunta in perguntas:
        resp_ant = antigos.get(pergunta, [])
        resp_novo = novos.get(pergunta, [])
        if resp_ant != resp_novo:
            diff.append({
                'pergunta': pergunta,
                'antigo': ', '.join(map(str, resp_ant)),
                'novo': ', '.join(map(str, resp_novo))
            })

    return render_template(
        'checklist_diff.html',
        filename=filename,
        anterior=anterior_nome,
        diff=diff,
        obra=obra,
    )


@bp.route('/solicitacao/<int:id>/delete', methods=['POST'])
@login_required
def delete_solicitacao(id):
    sol = Solicitacao.query.get_or_404(id)
    db.session.delete(sol)
    db.session.commit()
    flash('Solicitação removida.', 'success')
    return redirect(url_for('projetista.index'))


@bp.route('/config', methods=['GET', 'POST'])
@login_required
def config():
    """Página para configurar usuários e senhas."""
    if current_user.role != 'admin':
        return redirect(url_for('projetista.index'))

    admin_user = User.query.filter_by(role='admin').first()
    compras_user = User.query.filter_by(role='compras').first()
    ips = AuthorizedIP.query.all()

    if request.method == 'POST':
        # Gerenciar adicao ou remocao de IPs
        new_ip = request.form.get('new_ip')
        delete_ip = request.form.get('delete_ip')
        if new_ip is not None:
            ip_val = new_ip.strip()
            if ip_val and not AuthorizedIP.query.filter_by(ip_address=ip_val).first():
                db.session.add(AuthorizedIP(ip_address=ip_val))
                db.session.commit()
                flash('IP adicionado.', 'success')
            return redirect(url_for('projetista.config'))
        if delete_ip:
            ip_obj = AuthorizedIP.query.get(int(delete_ip))
            if ip_obj:
                db.session.delete(ip_obj)
                db.session.commit()
                flash('IP removido.', 'success')
            return redirect(url_for('projetista.config'))

        admin_username = request.form.get('admin_username', '').strip()
        admin_password = request.form.get('admin_password', '').strip()
        compras_username = request.form.get('compras_username', '').strip()
        compras_password = request.form.get('compras_password', '').strip()

        if admin_username:
            if not admin_user:
                admin_user = User(username=admin_username, role='admin')
                db.session.add(admin_user)
            else:
                admin_user.username = admin_username
            if admin_password:
                admin_user.set_password(admin_password)

        if compras_username:
            if not compras_user:
                compras_user = User(username=compras_username, role='compras')
                db.session.add(compras_user)
            else:
                compras_user.username = compras_username
            if compras_password:
                compras_user.set_password(compras_password)

        db.session.commit()
        flash('Credenciais atualizadas.', 'success')
        return redirect(url_for('projetista.config'))

    return render_template(
        'config.html',
        admin_user=admin_user,
        compras_user=compras_user,
        ips=ips
    )


@bp.route('/api/inspecoes')
def api_inspecoes():
    dados = []
    for sol in EstoqueSolicitacao.query.filter_by(verificado=False).order_by(EstoqueSolicitacao.id.desc()).all():
        dados.append({
            'id': sol.id,
            'itens': [
                {
                    'id': i.id,
                    'referencia': i.referencia,
                    'quantidade': i.quantidade,
                    'verificado': i.verificado,
                    'faltante': i.faltante,
                }
                for i in sol.itens
            ]
        })
    return jsonify(dados)


@bp.route('/api/inspecoes/<int:id>/resultado', methods=['POST'])
def api_inspecao_resultado(id):
    sol = EstoqueSolicitacao.query.get_or_404(id)
    dados = request.get_json() or {}
    for item_data in dados.get('itens', []):
        item = EstoqueItem.query.get(item_data.get('id'))
        if item and item.solicitacao_id == id:
            item.verificado = item_data.get('verificado', False)
            item.faltante = item_data.get('faltante', 0)
    sol.verificado = True
    db.session.commit()
    return jsonify({'ok': True})


def _safe_join(root: str, *paths: str) -> str:
    root = os.path.abspath(root)
    path = os.path.abspath(os.path.join(root, *paths))
    if not path.startswith(root + os.sep):
        raise ValueError('Caminho inválido')
    return path


def _build_asbuilt_tree(base: str) -> list:
    """Percorre ``base`` e lista pastas ``AS BUILT/FOTOS`` e suas subpastas."""
    extensoes = (".jpg", ".jpeg", ".png")
    dados = {}
    alvo = os.path.join("AS BUILT", "FOTOS").upper()

    for root, _dirs, files in os.walk(base):
        if alvo not in root.upper():
            continue

        rel = os.path.relpath(root, base)
        partes = rel.split(os.sep)
        if len(partes) < 3:
            continue

        ano = partes[0]
        obra_path = "/".join(partes[1:])

        arquivos = [f for f in sorted(files) if f.lower().endswith(extensoes)]

        ano_dict = dados.setdefault(ano, {})
        ano_dict[obra_path] = [{'name': f} for f in arquivos]

    arvore = []
    for ano in sorted(dados.keys()):
        obras = []
        for obra in sorted(dados[ano].keys()):
            obras.append({'name': obra, 'children': dados[ano][obra]})
        arvore.append({'name': ano, 'children': obras})

    return arvore


@bp.route('/api/fotos')
def api_listar_fotos():
    """Lista pastas de fotos, incluindo subpastas vazias."""
    return jsonify(_build_asbuilt_tree(FOTOS_DIR))


@bp.route('/api/fotos/raw/<path:filepath>')
def api_foto_raw(filepath: str):
    filepath = urllib.parse.unquote(filepath)
    try:
        file_path = _safe_join(FOTOS_DIR, *filepath.split('/'))
    except ValueError:
        return jsonify({'error': 'Caminho inválido'}), 400
    if not os.path.isfile(file_path):
        return jsonify({'error': 'Arquivo não encontrado'}), 404
    return send_file(file_path)


@bp.route('/api/fotos/upload', methods=['POST'])
def api_enviar_foto():
    ano = request.form.get('ano', '').strip()
    obra = request.form.get('obra', '').strip()
    arquivo = request.files.get('foto')
    if not ano or not obra or not arquivo:
        return jsonify({'error': 'Dados incompletos'}), 400
    filename = secure_filename(arquivo.filename)
    try:
        destino = _safe_join(FOTOS_DIR, ano, *obra.split('/'))
        os.makedirs(destino, exist_ok=True)
    except ValueError:
        return jsonify({'error': 'Caminho inválido'}), 400
    caminho = os.path.join(destino, filename)
    arquivo.save(caminho)
    return jsonify({'ok': True})
