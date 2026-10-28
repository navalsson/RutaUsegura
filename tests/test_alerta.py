import os
import unittest
from unittest.mock import patch

import mysql.connector
from flask import Flask

from app.controllers import alerta_controller


def crear_app():
    ruta_vistas = os.path.join(os.path.dirname(__file__), "..", "app", "views")
    app = Flask(__name__, template_folder=ruta_vistas)
    app.secret_key = "clave-de-pruebas"
    app.register_blueprint(alerta_controller.alerta_bp)
    return app


class PruebasPMV1(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.app = crear_app()

    def setUp(self):
        self.client = self.app.test_client()

    def iniciar_sesion(self, id_estudiante=7):
        with self.client.session_transaction() as sesion:
            sesion["id_estudiante"] = id_estudiante

    def datos_registro(self, **cambios):
        datos = {
            "nombre": "Rosa Mendoza",
            "codigo": "U20240001",
            "telefono": "987654321",
            "c_nombre": "Carmen Mendoza",
            "c_telefono": "912345678",
            "parentesco": "Madre",
        }
        datos.update(cambios)
        return datos

    # ---------- HU-01: Registro ----------

    def test_cp01_registro_exitoso(self):
        with patch.object(alerta_controller.estudiante, "registrar",
                          return_value=7) as registrar:
            r = self.client.post("/registro", data=self.datos_registro())
        self.assertEqual(r.status_code, 302)
        self.assertTrue(r.headers["Location"].endswith("/"))
        registrar.assert_called_once()
        with self.client.session_transaction() as sesion:
            self.assertEqual(sesion.get("id_estudiante"), 7)

    def test_cp02_campos_obligatorios(self):
        obligatorios = ("nombre", "codigo", "telefono", "c_nombre", "c_telefono")
        for campo in obligatorios:
            for valor in ("", "   "):
                with self.subTest(campo=campo, valor=repr(valor)):
                    with patch.object(alerta_controller.estudiante,
                                      "registrar") as registrar:
                        r = self.client.post(
                            "/registro",
                            data=self.datos_registro(**{campo: valor}))
                    self.assertEqual(r.status_code, 400)
                    self.assertIn("Completa todos los campos obligatorios.",
                                  r.get_data(as_text=True))
                    registrar.assert_not_called()

    def test_cp03_codigo_duplicado(self):
        with patch.object(alerta_controller.estudiante, "registrar",
                          side_effect=mysql.connector.IntegrityError("duplicado")):
            r = self.client.post("/registro", data=self.datos_registro())
        self.assertEqual(r.status_code, 400)
        self.assertIn("Ese código universitario ya está registrado.",
                      r.get_data(as_text=True))
        with self.client.session_transaction() as sesion:
            self.assertNotIn("id_estudiante", sesion)

    def test_cp04_error_de_base_de_datos_en_registro(self):
        with patch.object(alerta_controller.estudiante, "registrar",
                          side_effect=mysql.connector.Error("base caída")):
            r = self.client.post("/registro", data=self.datos_registro())
        self.assertEqual(r.status_code, 500)
        with self.client.session_transaction() as sesion:
            self.assertNotIn("id_estudiante", sesion)

    # ---------- HU-02: Alerta SOS ----------

    def test_cp05_alerta_exitosa(self):
        self.iniciar_sesion(7)
        with patch.object(alerta_controller.contacto, "obtener_por_estudiante",
                          return_value={"id_contacto": 3}), \
             patch.object(alerta_controller.alerta, "crear",
                          return_value=55) as crear:
            r = self.client.post("/sos",
                                 json={"latitud": -6.77, "longitud": -79.84})
        datos = r.get_json()
        self.assertEqual(r.status_code, 200)
        self.assertTrue(datos["ok"])
        self.assertEqual(datos["id_alerta"], 55)
        crear.assert_called_once_with(7, 3, -6.77, -79.84)

    def test_cp06_sin_ubicacion(self):
        self.iniciar_sesion(7)
        casos = (
            {},
            {"latitud": None, "longitud": None},
            {"latitud": "abc", "longitud": "xyz"},
        )
        for cuerpo in casos:
            with self.subTest(cuerpo=cuerpo):
                with patch.object(alerta_controller.alerta, "crear") as crear:
                    r = self.client.post("/sos", json=cuerpo)
                self.assertEqual(r.status_code, 400)
                self.assertEqual(r.get_json()["mensaje"],
                                 "No se pudo obtener la ubicación.")
                crear.assert_not_called()

    def test_cp07_sin_contacto_de_confianza(self):
        self.iniciar_sesion(7)
        with patch.object(alerta_controller.contacto, "obtener_por_estudiante",
                          return_value=None), \
             patch.object(alerta_controller.alerta, "crear") as crear:
            r = self.client.post("/sos",
                                 json={"latitud": -6.77, "longitud": -79.84})
        self.assertEqual(r.status_code, 400)
        self.assertIn("contacto de confianza", r.get_json()["mensaje"])
        crear.assert_not_called()

    def test_cp08_error_de_base_de_datos_en_sos(self):
        self.iniciar_sesion(7)
        with patch.object(alerta_controller.contacto, "obtener_por_estudiante",
                          return_value={"id_contacto": 3}), \
             patch.object(alerta_controller.alerta, "crear",
                          side_effect=mysql.connector.Error("base caída")):
            r = self.client.post("/sos",
                                 json={"latitud": -6.77, "longitud": -79.84})
        datos = r.get_json()
        self.assertEqual(r.status_code, 500)
        self.assertFalse(datos["ok"])
        self.assertNotIn("id_alerta", datos)

    def test_cp09_sin_registro_previo(self):
        with patch.object(alerta_controller.alerta, "crear") as crear:
            r = self.client.post("/sos",
                                 json={"latitud": -6.77, "longitud": -79.84})
        self.assertEqual(r.status_code, 401)
        self.assertEqual(r.get_json()["mensaje"], "Primero debes registrarte.")
        crear.assert_not_called()

    def test_cp10_ubicacion_invalida(self):
        self.iniciar_sesion(7)
        casos = (
            {"latitud": 999, "longitud": -79.84},
            {"latitud": -6.77, "longitud": 500},
            {"latitud": "nan", "longitud": -79.84},
        )
        for cuerpo in casos:
            with self.subTest(cuerpo=cuerpo):
                with patch.object(alerta_controller.contacto,
                                  "obtener_por_estudiante",
                                  return_value={"id_contacto": 3}), \
                     patch.object(alerta_controller.alerta, "crear") as crear:
                    r = self.client.post("/sos", json=cuerpo)
                self.assertEqual(r.status_code, 400)
                self.assertEqual(r.get_json()["mensaje"],
                                 "La ubicación recibida no es válida.")
                crear.assert_not_called()


if __name__ == "__main__":
    unittest.main()