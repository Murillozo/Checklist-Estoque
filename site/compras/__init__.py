from flask import Blueprint, render_template, redirect, url_for, flash
from flask_login import login_required
from models import db, Solicitacao

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
    return render_template('compras.html', solicitacoes=solicitacoes)


@bp.route('/<int:id>/concluir', methods=['POST'])
@login_required
def concluir(id: int):
    """Marca a solicitação como concluída."""
    sol = Solicitacao.query.get_or_404(id)
    sol.status = 'concluido'
    db.session.commit()
    flash('Solicitação concluída.', 'success')
    return redirect(url_for('compras.index'))
