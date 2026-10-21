from flask import Flask

from app.controllers.alerta_controller import alerta_bp

app = Flask(__name__, template_folder="app/views")
app.register_blueprint(alerta_bp)

if __name__ == "__main__":
    app.run(debug=True)