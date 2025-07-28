from flask import Blueprint, render_template
from flask_login import login_required
from models import Solicitacao

bp = Blueprint('compras', __name__, template_folder='../projetista/templates')

@bp.route('/')
@login_required
def index():
    solicitacoes = Solicitacao.query.order_by(Solicitacao.data.desc()).all()
    return render_template('index.html', solicitacoes=solicitacoes)
