# app.py
import os
from flask import Flask
from models import db
from projetista import bp as projetista_bp

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
    app.register_blueprint(projetista_bp, url_prefix='/projetista')

    with app.app_context():
        db.create_all()

    return app

if __name__ == '__main__':
    app = create_app()
    app.run(debug=True, host='0.0.0.0')
