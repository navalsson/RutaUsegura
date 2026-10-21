
from app.config.database import get_connection


def obtener_por_id(id_estudiante):
    conexion = get_connection()
    cursor = conexion.cursor(dictionary=True)
    cursor.execute(
        "SELECT * FROM estudiante WHERE id_estudiante = %s",
        (id_estudiante,),
    )
    estudiante = cursor.fetchone()
    cursor.close()
    conexion.close()
    return estudiante


def registrar(nombre, codigo, telefono, c_nombre, c_telefono, parentesco):
    conexion = get_connection()
    cursor = conexion.cursor()

    try:
        cursor.execute(
            "INSERT INTO estudiante (nombre, codigo_universitario, telefono) "
            "VALUES (%s, %s, %s)",
            (nombre, codigo, telefono),
        )

        id_estudiante = cursor.lastrowid

        cursor.execute(
            "INSERT INTO contacto_confianza "
            "(id_estudiante, nombre, telefono, parentesco) "
            "VALUES (%s, %s, %s, %s)",
            (id_estudiante, c_nombre, c_telefono, parentesco or None),
        )

        conexion.commit()
        return id_estudiante

    except Exception:
        conexion.rollback()
        raise

    finally:
        cursor.close()
        conexion.close()
