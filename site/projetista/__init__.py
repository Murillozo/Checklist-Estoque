# projetista/__init__.py
from flask import Blueprint, render_template, request, redirect, url_for, flash
from models import db, Solicitacao, Item, User, AuthorizedIP
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

bp = Blueprint('projetista', __name__)

# Diretório base onde os projetos são armazenados no servidor
BASE_PRODUCAO = r"F:\03 - ENGENHARIA\03 - PRODUCAO"

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
    # tenta descobrir os anos disponíveis no servidor
    try:
        anos = [d for d in os.listdir(BASE_PRODUCAO)
                if os.path.isdir(os.path.join(BASE_PRODUCAO, d))]
    except OSError:
        # se o diretório não estiver acessível, usa o ano atual
        anos = [str(datetime.now().year)]

    if request.method == 'POST':
        obra = request.form['obra'].strip()
        ano = request.form.get('ano', '').strip()

        sol = Solicitacao(obra=obra)
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

    return render_template('nova_solicitacao.html', anos=anos)


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
            "itens": itens,
            "status": sol.status,
            "pendencias": sol.pendencias
        })
    return jsonify(resultados)


@bp.route('/api/solicitacoes/<int:id>/aprovar', methods=['POST'])
@login_required
def api_aprovar(id):
    sol = Solicitacao.query.get_or_404(id)
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
