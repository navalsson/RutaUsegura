import os

from dotenv import load_dotenv
from flask import Flask

from app.controllers.alerta_controller import alerta_bp

load_dotenv()

app = Flask(__name__, template_folder="app/views")
app.secret_key = os.getenv("SECRET_KEY", "clave-de-desarrollo")
app.register_blueprint(alerta_bp)

if __name__ == "__main__":
    app.run(debug=True)
    