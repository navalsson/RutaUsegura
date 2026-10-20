from app.config.database import get_connection


def crear(id_estudiante, id_contacto, latitud, longitud):
    conexion = get_connection()
    cursor = conexion.cursor()
    try:
        cursor.execute(
            "INSERT INTO alerta (id_estudiante, id_contacto, latitud, longitud) "
            "VALUES (%s, %s, %s, %s)",
            (id_estudiante, id_contacto, latitud, longitud),
        )
        conexion.commit()
        return cursor.lastrowid
    finally:
        cursor.close()
        conexion.close()