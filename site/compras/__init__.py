from flask import Blueprint, render_template
from flask_login import login_required
from models import Solicitacao
import pytz

bp = Blueprint('compras', __name__, template_folder='../projetista/templates')


def _listar_pendentes():
    """Retorna solicitações que estão com status 'compras'."""
    consulta = (
        Solicitacao.query.filter_by(status='compras')
        .order_by(Solicitacao.data.asc())
        .all()
    )

    tz = pytz.timezone('America/Sao_Paulo')
    for sol in consulta:
        utc_dt = sol.data.replace(tzinfo=pytz.UTC)
        sol.local_time = utc_dt.astimezone(tz)

        agrupados = {}
        for it in sol.itens:
            agrupados[it.referencia] = agrupados.get(it.referencia, 0) + it.quantidade
        sol.itens_agrupados = list(agrupados.items())

    return consulta


@bp.route('/')
@login_required
def index():
    solicitacoes = _listar_pendentes()
    return render_template('solicitacoes.html', solicitacoes=solicitacoes)
