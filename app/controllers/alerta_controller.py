import mysql.connector
from flask import Blueprint, jsonify, render_template, request

from app.models import alerta, contacto

alerta_bp = Blueprint("alerta", __name__)

ID_ESTUDIANTE_DEMO = 1


@alerta_bp.route("/")
def inicio():
    return render_template("sos.html")


@alerta_bp.route("/sos", methods=["POST"])
def enviar_sos():
    datos = request.get_json(silent=True) or {}

    try:
        latitud = float(datos.get("latitud"))
        longitud = float(datos.get("longitud"))
    except (TypeError, ValueError):
        return jsonify({"ok": False, "mensaje": "No se pudo obtener la ubicación."}), 400

    try:
        datos_contacto = contacto.obtener_por_estudiante(ID_ESTUDIANTE_DEMO)
        if datos_contacto is None:
            return jsonify({
                "ok": False,
                "mensaje": "Registra un contacto de confianza antes de enviar una alerta.",
            }), 400

        id_alerta = alerta.crear(
            ID_ESTUDIANTE_DEMO, datos_contacto["id_contacto"], latitud, longitud
        )
    except mysql.connector.Error:
        return jsonify({
            "ok": False,
            "mensaje": "La operación no pudo completarse. La alerta no fue enviada.",
        }), 500

    return jsonify({"ok": True, "mensaje": "Alerta enviada", "id_alerta": id_alerta})