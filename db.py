import sqlite3
from pathlib import Path
import pandas as pd

DB_PATH = Path('chicago.db')


def ensure_alerts_table(db_path: str = 'chicago.db'):
    conn = sqlite3.connect(db_path)
    try:
        cur = conn.cursor()
        cur.execute('''
        CREATE TABLE IF NOT EXISTS alerts (
            lat_bin REAL,
            lon_bin REAL,
            count INTEGER,
            created_at TEXT
        )
        ''')
        conn.commit()
    finally:
        conn.close()


def persist_alerts(df: pd.DataFrame, db_path: str = 'chicago.db'):
    ensure_alerts_table(db_path)
    conn = sqlite3.connect(db_path)
    try:
        df.to_sql('alerts', conn, if_exists='append', index=False)
    finally:
        conn.close()


def list_alerts(limit: int = 100, db_path: str = 'chicago.db'):
    if not DB_PATH.exists():
        return []
    conn = sqlite3.connect(db_path)
    try:
        df = pd.read_sql_query(f"SELECT * FROM alerts ORDER BY created_at DESC LIMIT {int(limit)}", conn)
        return df
    finally:
        conn.close()
