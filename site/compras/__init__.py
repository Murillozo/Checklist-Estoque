from flask import Blueprint, render_template, redirect, url_for, flash, request, jsonify
from flask_login import login_required
from models import db, Solicitacao, Item, ITEM_STATUS_OPTIONS
import json

bp = Blueprint('compras', __name__, template_folder='../projetista/templates')

@bp.route('/')
@login_required
def index():
    """Lista apenas as solicitações que estão com status 'compras'."""
    solicitacoes = (
        Solicitacao.query
        .filter_by(status='compras')
        .order_by(Solicitacao.data.desc())
        .all()
    )

    # carrega lista de pendências (itens faltantes) se existir
    for sol in solicitacoes:
        try:
            sol.pendencias_list = json.loads(sol.pendencias or "[]")
        except json.JSONDecodeError:
            sol.pendencias_list = []


    return render_template(
        'compras.html',
        solicitacoes=solicitacoes,
        hide_navbar=True,
        status_options=ITEM_STATUS_OPTIONS,
    )


@bp.route('/<int:id>/concluir', methods=['POST'])
@login_required
def concluir(id: int):
    """Marca a solicitação como aprovada."""
    sol = Solicitacao.query.get_or_404(id)
    sol.status = 'aprovado'
    db.session.commit()
    flash('Solicitação aprovada.', 'success')
    return redirect(url_for('compras.index'))


@bp.route('/item/<int:item_id>/status', methods=['POST'])
@login_required
def atualizar_item_status(item_id: int):
    """Atualiza o status de um item individual."""
    item = Item.query.get_or_404(item_id)
    dados = request.get_json() or {}
    status = dados.get('status')
    if status not in ITEM_STATUS_OPTIONS:
        return jsonify({'ok': False, 'error': 'status inválido'}), 400
    item.status = status
    db.session.commit()
    return jsonify({'ok': True})
