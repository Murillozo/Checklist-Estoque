# app.py
import os

from models import db, User, AuthorizedIP, ITEM_STATUS_OPTIONS
from projetista import bp as projetista_bp
from compras import bp as compras_bp
from auth import bp as auth_bp
from json_api import bp as json_api_bp, merge_directory, move_matching_checklists
from checklist_blueprint import bp as checklist_bp
from flask_login import LoginManager, login_user, current_user
from flask import Flask, request
from sqlalchemy import inspect, text

def create_app():
    app = Flask(__name__, template_folder="projetista/templates")

    # Chave para sessões e flash
    app.secret_key = os.environ.get(
        'SECRET_KEY',
        'bfcc1cd715c7f281a72a7040b703f307e016a2897623b7981d23dffd582de161'
    )
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///db.sqlite3'
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

    db.init_app(app)

    login_manager = LoginManager()
    login_manager.login_view = 'auth.login'
    login_manager.init_app(app)

    @login_manager.user_loader
    def load_user(user_id):
        return User.query.get(int(user_id))

    @app.before_request
    def auto_login_ip():
        if current_user.is_authenticated:
            return
        ip = request.remote_addr
        if AuthorizedIP.query.filter_by(ip_address=ip).first():
            user = User.query.filter_by(role='compras').first()
            if not user:
                user = User.query.filter_by(role='admin').first()
            if user:
                login_user(user)

    app.register_blueprint(projetista_bp, url_prefix='/projetista')
    app.register_blueprint(compras_bp, url_prefix='/compras')
    app.register_blueprint(json_api_bp, url_prefix='/json_api')
    app.register_blueprint(checklist_bp, url_prefix='/projetista/checklist')
    app.register_blueprint(auth_bp)
    base_json = os.path.join(os.path.dirname(__file__), 'json_api')
    merge_directory(base_json)
    move_matching_checklists(base_json)

    with app.app_context():
        db.create_all()

        # garante colunas adicionais nas tabelas para bancos antigos
        insp = inspect(db.engine)
        if 'solicitacao' in insp.get_table_names():
            cols = [c['name'] for c in insp.get_columns('solicitacao')]
            if 'status' not in cols:
                db.session.execute(
                    text("ALTER TABLE solicitacao ADD COLUMN status VARCHAR(20) DEFAULT 'analise'")
                )
                db.session.commit()
            if 'pendencias' not in cols:
                db.session.execute(
                    text("ALTER TABLE solicitacao ADD COLUMN pendencias TEXT")
                )
                db.session.commit()
            if 'data_entrega' not in cols:
                db.session.execute(
                    text("ALTER TABLE solicitacao ADD COLUMN data_entrega DATE")
                )
                db.session.commit()

        if 'item' in insp.get_table_names():
            cols = [c['name'] for c in insp.get_columns('item')]
            if 'status' not in cols:
                db.session.execute(
                    text("ALTER TABLE item ADD COLUMN status VARCHAR(20) DEFAULT 'Nao iniciada'")
                )
                db.session.commit()
            if 'previsao_entrega' not in cols:
                db.session.execute(
                    text("ALTER TABLE item ADD COLUMN previsao_entrega DATE")
                )
                db.session.commit()

        if not User.query.filter_by(username='admin').first():
            admin = User(username='admin', role='admin')
            admin.set_password('admin')
            db.session.add(admin)
            db.session.commit()

    return app

if __name__ == '__main__':
    from json_api import list_checklists

    list_checklists.main()
    app = create_app()
    app.run(debug=True, host='0.0.0.0')
