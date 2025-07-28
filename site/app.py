# app.py
import os
from flask import Flask, redirect, url_for
from models import db, User
from projetista import bp as projetista_bp
from compras import bp as compras_bp
from auth import bp as auth_bp
from flask_login import LoginManager

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

    app.register_blueprint(projetista_bp, url_prefix='/projetista')
    app.register_blueprint(compras_bp, url_prefix='/compras')
    app.register_blueprint(auth_bp)

    @app.route('/')
    def root():
        return redirect(url_for('auth.login'))

    with app.app_context():
        db.create_all()
        if not User.query.filter_by(username='admin').first():
            admin = User(username='admin', role='admin')
            admin.set_password('admin')
            db.session.add(admin)
            db.session.commit()

    return app

if __name__ == '__main__':
    app = create_app()
    app.run(debug=True, host='0.0.0.0')
