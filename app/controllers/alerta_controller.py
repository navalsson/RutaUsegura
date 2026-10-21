import mysql.connector
from flask import (Blueprint, jsonify, redirect, render_template,
                   request, session, url_for)

from app.models import alerta, contacto, estudiante

alerta_bp = Blueprint("alerta", __name__)


@alerta_bp.route("/")
def inicio():
    if "id_estudiante" not in session:
        return redirect(url_for("alerta.registro"))
    return render_template("sos.html")


@alerta_bp.route("/registro", methods=["GET", "POST"])
def registro():
    if request.method == "GET":
        return render_template("registro.html", error=None, datos={})

    campos = ("nombre", "codigo", "telefono", "c_nombre", "c_telefono", "parentesco")
    datos = {c: request.form.get(c, "").strip() for c in campos}

    obligatorios = ("nombre", "codigo", "telefono", "c_nombre", "c_telefono")
    if any(not datos[c] for c in obligatorios):
        return render_template(
            "registro.html",
            error="Completa todos los campos obligatorios.",
            datos=datos,
        ), 400

    try:
        id_estudiante = estudiante.registrar(
            datos["nombre"], datos["codigo"], datos["telefono"],
            datos["c_nombre"], datos["c_telefono"], datos["parentesco"],
        )
    except mysql.connector.IntegrityError:
        return render_template(
            "registro.html",
            error="Ese código universitario ya está registrado.",
            datos=datos,
        ), 400
    except mysql.connector.Error:
        return render_template(
            "registro.html",
            error="El registro no pudo completarse. Intenta nuevamente.",
            datos=datos,
        ), 500

    session["id_estudiante"] = id_estudiante
    return redirect(url_for("alerta.inicio"))


@alerta_bp.route("/sos", methods=["POST"])
def enviar_sos():
    id_estudiante = session.get("id_estudiante")
    if id_estudiante is None:
        return jsonify({"ok": False, "mensaje": "Primero debes registrarte."}), 401

    datos = request.get_json(silent=True) or {}
    try:
        latitud = float(datos.get("latitud"))
        longitud = float(datos.get("longitud"))
    except (TypeError, ValueError):
        return jsonify({"ok": False, "mensaje": "No se pudo obtener la ubicación."}), 400

    try:
        datos_contacto = contacto.obtener_por_estudiante(id_estudiante)
        if datos_contacto is None:
            return jsonify({
                "ok": False,
                "mensaje": "Registra un contacto de confianza antes de enviar una alerta.",
            }), 400

        id_alerta = alerta.crear(
            id_estudiante, datos_contacto["id_contacto"], latitud, longitud
        )
    except mysql.connector.Error:
        return jsonify({
            "ok": False,
            "mensaje": "La operación no pudo completarse. La alerta no fue enviada.",
        }), 500

    return jsonify({"ok": True, "mensaje": "Alerta enviada", "id_alerta": id_alerta})