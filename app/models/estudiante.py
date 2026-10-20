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