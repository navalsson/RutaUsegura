from app.config.database import get_connection


def obtener_por_estudiante(id_estudiante):
    conexion = get_connection()
    cursor = conexion.cursor(dictionary=True)
    cursor.execute(
        "SELECT * FROM contacto_confianza WHERE id_estudiante = %s LIMIT 1",
        (id_estudiante,),
    )
    contacto = cursor.fetchone()
    cursor.close()
    conexion.close()
    return contacto