from flask import Flask

# Desabilita rota /static para não atrapalhar
app = Flask(__name__, static_folder=None)

@app.route('/', methods=['GET'])
def index():
    return 'Flask raiz OK!', 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
