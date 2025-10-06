import requests
import pandas as pd
import time
import random
from datetime import datetime
from typing import List, Dict, Any, Optional
import streamlit as st

SCODA_URL = "https://data.cityofchicago.org/resource/ijzp-q8t2.json"
DEFAULT_FROM_DATE = "2024-01-01T00:00:00"

SCHEMA_COLUMNS = [
    'id', 'case_number', 'date', 'block', 'iucr', 'primary_type',
    'description', 'location_description', 'arrest', 'domestic', 'beat',
    'district', 'ward', 'community_area', 'fbi_code', 'year', 'updated_on',
    'x_coordinate', 'y_coordinate', 'latitude', 'longitude', 'location'
]


def _records_to_dataframe(records: List[Dict[str, Any]]) -> pd.DataFrame:
    if not records:
        return pd.DataFrame(columns=SCHEMA_COLUMNS)
    df = pd.DataFrame(records)
    for c in SCHEMA_COLUMNS:
        if c not in df.columns:
            df[c] = None
    for col in ['date', 'updated_on']:
        if col in df.columns:
            df[col] = pd.to_datetime(df[col], errors='coerce')
    for coord in ['latitude', 'longitude']:
        if coord in df.columns:
            df[coord] = pd.to_numeric(df[coord], errors='coerce')
    return df[SCHEMA_COLUMNS]


def fetch_latest(limit: int = 5000, force: bool = False, refresh_interval: int = 60) -> pd.DataFrame:
    """Return the latest `limit` records (ordered by date DESC).
    Caches in Streamlit session state for `refresh_interval` seconds unless `force` is True.
    """
    key_time = '_chicago_last_fetch_time'
    key_df = '_chicago_last_df'
    now = time.time()
    last = st.session_state.get(key_time, 0)
    if (not force) and (now - last < refresh_interval) and (key_df in st.session_state):
        return st.session_state[key_df]

    params = {'$limit': limit, '$order': 'date DESC'}
    try:
        resp = requests.get(SCODA_URL, params=params, timeout=30)
        resp.raise_for_status()
        records = resp.json()
    except Exception as e:
        st.error(f'Error fetching data from API: {e}')
        records = []

    df = _records_to_dataframe(records)
    st.session_state[key_time] = now
    st.session_state[key_df] = df
    return df


def generate_random_records(n: int, base_lat: Optional[float] = None, base_lon: Optional[float] = None) -> pd.DataFrame:
    rows = []
    now = datetime.utcnow()
    for i in range(n):
        lat = None
        lon = None
        if base_lat is not None and base_lon is not None:
            lat = base_lat + random.uniform(-0.01, 0.01)
            lon = base_lon + random.uniform(-0.01, 0.01)
        row = {
            'id': f'fake-{int(time.time()*1000)}-{i}',
            'case_number': f'FAKE{i:06d}',
            'date': now.isoformat(),
            'block': 'UNKNOWN',
            'iucr': '0000',
            'primary_type': random.choice(['THEFT', 'BATTERY', 'ROBBERY', 'CRIMINAL DAMAGE', 'ASSAULT']),
            'description': 'Synthetic record for testing',
            'location_description': 'RESIDENCE',
            'arrest': random.choice([True, False]),
            'domestic': random.choice([True, False]),
            'beat': None,
            'district': None,
            'ward': None,
            'community_area': None,
            'fbi_code': None,
            'year': now.year,
            'updated_on': now.isoformat(),
            'x_coordinate': None,
            'y_coordinate': None,
            'latitude': lat,
            'longitude': lon,
            'location': None
        }
        rows.append(row)
    return _records_to_dataframe(rows)


def persist_dataframe_to_sqlite(df: pd.DataFrame, db_path: str = 'chicago.db', table: str = 'crimes') -> None:
    import sqlite3
    conn = sqlite3.connect(db_path)
    try:
        df.to_sql(table, conn, if_exists='append', index=False)
    finally:
        conn.close()


def add_records_to_session(df: pd.DataFrame) -> None:
    key_df = '_chicago_last_df'
    existing = st.session_state.get(key_df, pd.DataFrame(columns=SCHEMA_COLUMNS))
    st.session_state[key_df] = pd.concat([df, existing], ignore_index=True)
