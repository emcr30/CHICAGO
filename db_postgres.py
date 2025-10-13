import os
import psycopg2
from psycopg2.extras import execute_values
from typing import Any, List, Dict, Optional

# Configuración directa para Azure PostgreSQL (ajusta estos valores a los de tu base de datos)
PG_HOST: str = "chic2025.postgres.database.azure.com"  # ejemplo: chicagodb.postgres.database.azure.com
PG_DBNAME: str = "postgres"
PG_USER: str = "chic1"  
PG_PASSWORD: str = "Atlantis31"
PG_PORT: str = "5432"
PG_SSLMODE: str = "require"


def get_pg_conn() -> psycopg2.extensions.connection:
    return psycopg2.connect(
        host=PG_HOST,
        dbname=PG_DBNAME,
        user=PG_USER,
        password=PG_PASSWORD,
        port=PG_PORT,
        sslmode=PG_SSLMODE
    )
    """
    Crea y retorna una conexión a la base de datos PostgreSQL usando las variables de entorno.

    @returns Conexión activa a PostgreSQL.
    """
    return psycopg2.connect(
        host=PG_HOST,
        dbname=PG_DBNAME,
        user=PG_USER,
        password=PG_PASSWORD,
        port=PG_PORT,
        sslmode=PG_SSLMODE
    )


def insert_crimes(records: List[Dict[str, Any]]) -> None:
    if not records:
        return
    columns = [
        'id', 'case_number', 'date', 'block', 'iucr', 'primary_type',
        'description', 'location_description', 'arrest', 'domestic', 'beat',
        'district', 'ward', 'community_area', 'fbi_code', 'year', 'updated_on',
        'latitude', 'longitude', 'location'
    ]
    values = [tuple(rec.get(col) for col in columns) for rec in records]
    insert_sql = f"""
        INSERT INTO crimes ({', '.join(columns)})
        VALUES %s
        ON CONFLICT (id) DO UPDATE SET
        {', '.join([f'{col}=EXCLUDED.{col}' for col in columns if col != 'id'])}
    """
    conn = get_pg_conn()
    try:
        with conn.cursor() as cur:
            execute_values(cur, insert_sql, values)
        conn.commit()
    finally:
        conn.close()
    """
    Inserta o actualiza múltiples registros de crimen en la base de datos PostgreSQL.

    @param records Lista de diccionarios con los datos de los crímenes a insertar o actualizar.
    """
    if not records:
        return
    columns: list[str] = [
        'id', 'case_number', 'date', 'block', 'iucr', 'primary_type',
        'description', 'location_description', 'arrest', 'domestic', 'beat',
        'district', 'ward', 'community_area', 'fbi_code', 'year', 'updated_on',
        'latitude', 'longitude', 'location'
    ]
    values: list[tuple[Any, ...]] = [tuple(rec.get(col) for col in columns) for rec in records]
    insert_sql: str = f"""
        INSERT INTO crimes ({', '.join(columns)})
        VALUES %s
        ON CONFLICT (id) DO UPDATE SET
        {', '.join([f'{col}=EXCLUDED.{col}' for col in columns if col != 'id'])}
    """
    conn = get_pg_conn()
    try:
        with conn.cursor() as cur:
            execute_values(cur, insert_sql, values)
        conn.commit()
    finally:
        conn.close()


def fetch_latest_crimes(limit: int = 5000) -> List[Dict[str, Any]]:
    conn = get_pg_conn()
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT * FROM crimes ORDER BY date DESC LIMIT %s", (limit,))
            cols = [desc[0] for desc in cur.description]
            rows = cur.fetchall()
            return [dict(zip(cols, row)) for row in rows]
    finally:
        conn.close()
    """
    Obtiene los registros más recientes de la tabla de crímenes en PostgreSQL.

    @param limit Número máximo de registros a obtener.
    @returns Lista de diccionarios con los registros de crímenes más recientes.
    """
    conn = get_pg_conn()
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT * FROM crimes ORDER BY date DESC LIMIT %s", (limit,))
            cols = [desc[0] for desc in cur.description]
            rows = cur.fetchall()
            return [dict(zip(cols, row)) for row in rows]
    finally:
        conn.close()
