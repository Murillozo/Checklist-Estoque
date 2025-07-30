# models.py
from datetime import datetime
from flask_sqlalchemy import SQLAlchemy
from flask_login import UserMixin
from werkzeug.security import generate_password_hash, check_password_hash

db = SQLAlchemy()

# Opções de status individuais para cada item
ITEM_STATUS_OPTIONS = [
    'Nao iniciada',
    'Em cotação',
    'Fechado',
    'Faturado',
    'Entregue',
    'Separado',
    'Faturamento',
    'Cancelado',
]

class Solicitacao(db.Model):
    __tablename__ = 'solicitacao'
    id = db.Column(db.Integer, primary_key=True)
    obra = db.Column(db.String(100), nullable=False)
    data = db.Column(db.DateTime, default=datetime.utcnow)
    # novo status padrao indicando que a solicitacao esta em analise
    status = db.Column(db.String(20), default='analise')
    pendencias = db.Column(db.Text)
    itens = db.relationship('Item', backref='solicitacao', cascade='all, delete-orphan')

class Item(db.Model):
    __tablename__ = 'item'
    id = db.Column(db.Integer, primary_key=True)
    solicitacao_id = db.Column(db.Integer, db.ForeignKey('solicitacao.id'), nullable=False)
    referencia = db.Column(db.String(100), nullable=False)
    quantidade = db.Column(db.Integer, nullable=False)
    status = db.Column(db.String(20), default='Nao iniciada')


class ItemStatusHistory(db.Model):
    """Historico de mudancas de status de um item."""
    __tablename__ = 'item_status_history'

    id = db.Column(db.Integer, primary_key=True)
    item_id = db.Column(db.Integer, db.ForeignKey('item.id'), nullable=False)
    old_status = db.Column(db.String(20), nullable=False)
    new_status = db.Column(db.String(20), nullable=False)
    changed_at = db.Column(db.DateTime, default=datetime.utcnow, nullable=False)


class User(UserMixin, db.Model):
    __tablename__ = 'user'
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    password_hash = db.Column(db.String(128), nullable=False)
    role = db.Column(db.String(20), nullable=False)

    def set_password(self, password: str) -> None:
        self.password_hash = generate_password_hash(password)

    def check_password(self, password: str) -> bool:
        return check_password_hash(self.password_hash, password)


class AuthorizedIP(db.Model):
    __tablename__ = 'authorized_ip'
    id = db.Column(db.Integer, primary_key=True)
    ip_address = db.Column(db.String(45), unique=True, nullable=False)
