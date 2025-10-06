import streamlit as st
import pandas as pd
from data import fetch_latest, generate_random_records, add_records_to_session, persist_dataframe_to_sqlite
from viz import show_primary_type_bar, show_map_points_and_heat, show_additional_charts, calculate_hotspots
from auth import verify_user, create_user, is_admin
import sqlite3
from db import persist_alerts, ensure_alerts_table, list_alerts
from notify import send_email, send_webhook, load_config


DEFAULT_LIMIT = 5000


def app():
    st.set_page_config(page_title='Chicago Crimes - Streamlit', layout='wide')
    st.title('Visualizador de datos de Chicago (últimos registros)')

    with st.sidebar:
        st.header('Controles')
        limit = st.number_input('Últimos registros a traer', min_value=100, max_value=50000, value=DEFAULT_LIMIT, step=100)
        auto_refresh = st.checkbox('Auto-refresh cada 60s', value=True)
        force_refresh = st.button('Forzar refresco ahora')
        st.markdown('---')
        st.subheader('Autenticación')
        if '_user' not in st.session_state:
            st.session_state['_user'] = None
        if st.session_state.get('_user') is None:
            username = st.text_input('Usuario')
            password = st.text_input('Contraseña', type='password')
            if st.button('Ingresar'):
                if verify_user(username, password):
                    st.session_state['_user'] = username
                    st.success(f'Ingresado como {username}')
                else:
                    st.error('Credenciales inválidas')
            st.markdown('Registrarse (crear cuenta local)')
            new_user = st.text_input('Nuevo usuario')
            new_pass = st.text_input('Nueva contraseña', type='password')
            if st.button('Crear usuario'):
                ok = create_user(new_user, new_pass)
                if ok:
                    st.success('Usuario creado. Ahora ingrese con sus credenciales.')
                else:
                    st.error('Usuario ya existe')
        else:
            st.write(f'Usuario: {st.session_state.get("_user")}')
            if st.button('Cerrar sesión'):
                st.session_state['_user'] = None

        st.markdown('---')
        st.subheader('Filtros de visualización')
        # enable optional date filters
        use_date_filter = st.checkbox('Activar filtro de fecha', value=False)
        if use_date_filter:
            min_date = st.date_input('Fecha desde')
            max_date = st.date_input('Fecha hasta')
        else:
            min_date = None
            max_date = None

        st.markdown('---')
        st.subheader('Inyectar datos desde UI o código')
        inject_count = st.number_input('Cantidad sintética (UI)', min_value=1, max_value=10000, value=10)
        home_lat = st.number_input('Latitud base (opcional)', format="%.6f", value=0.0)
        home_lon = st.number_input('Longitud base (opcional)', format="%.6f", value=0.0)
        use_home = st.checkbox('Usar lat/lon base (UI)', value=False)
        btn_inject = st.button('Generar e inyectar (UI)')

        st.markdown('---')
        st.subheader('Persistencia y DB')
        persist_db = st.button('Persistir en SQLite (append)')
        clear_db = st.button('Eliminar DB local (chicago.db)')

    # always fetch latest on load or when forced
    df = fetch_latest(limit=int(limit), force=force_refresh, refresh_interval=60 if auto_refresh else 999999)

    # apply simple filters
    try:
        if min_date is not None:
            df = df[df['date'] >= pd.to_datetime(min_date)]
        if max_date is not None:
            df = df[df['date'] <= pd.to_datetime(max_date)]
    except Exception:
        pass
    # primary type options population
    all_types = sorted(df['primary_type'].dropna().unique().tolist())
    # use previous selection from session state if present
    prev_sel = st.session_state.get('_primary_type_sel', [])
    sel = st.sidebar.multiselect('Primary Type', options=all_types, default=prev_sel or all_types[:5])
    st.session_state['_primary_type_sel'] = sel
    if sel:
        df = df[df['primary_type'].isin(sel)]

    # code-based injection area: allow the user to paste or run a small snippet
    st.sidebar.markdown('---')
    st.sidebar.caption('Inyección desde código: usa `add_records_to_session(df)` desde un script local o aquí en dev')

    if btn_inject:
        base_lat = None
        base_lon = None
        if use_home and (home_lat != 0.0 or home_lon != 0.0):
            base_lat = float(home_lat)
            base_lon = float(home_lon)
        synth = generate_random_records(int(inject_count), base_lat, base_lon)
        add_records_to_session(synth)
        df = st.session_state.get('_chicago_last_df', df)
        st.success(f'Se inyectaron {len(synth)} registros sintéticos (UI)')

    if persist_db:
        persist_dataframe_to_sqlite(df)
        st.success('Datos persistidos en chicago.db')

    if clear_db:
        import os
        try:
            os.remove('chicago.db')
            st.success('Archivo chicago.db eliminado')
        except FileNotFoundError:
            st.info('No existía chicago.db')
        except Exception as e:
            st.error(f'No se pudo eliminar chicago.db: {e}')

    st.header('Datos y visualizaciones')
    st.write(f'Registros en memoria: {len(df)}')
    st.dataframe(df.head(200))

    # charts
    show_primary_type_bar(df)
    show_additional_charts(df)
    hotspots = show_map_points_and_heat(df, heat_threshold=50)

    # admin panel: allow persisting hotspot alerts
    user = st.session_state.get('_user')
    if user and is_admin(user):
        st.markdown('---')
        st.subheader('Panel admin: alertas y acciones')
        round_digits = st.number_input('Hotspot rounding digits (granularity)', min_value=2, max_value=5, value=3)
        min_count = st.number_input('Min incidents to consider hotspot', min_value=1, max_value=1000, value=50)
        if st.button('Calcular hotspots (admin)'):
            hs = calculate_hotspots(df, round_digits=round_digits, min_count=min_count)
            st.dataframe(hs)
            if not hs.empty:
                if st.button('Persistir alertas en DB'):
                    try:
                        hs['created_at'] = pd.Timestamp.utcnow()
                        persist_alerts(hs)
                        st.success(f'Persistidas {len(hs)} alertas en tabla alerts')
                    except Exception as e:
                        st.error(f'No se pudo persistir alertas: {e}')
                if st.button('Enviar alertas (email/webhook)'):
                    cfg = load_config()
                    body = f'Hotspots detectados:\n{hs.to_csv(index=False)}'
                    # try email
                    try:
                        if cfg.get('email') and cfg['email'].get('recipients'):
                            send_email('Hotspots Chicago', body, cfg['email'].get('recipients'))
                            st.success('Correo enviado a destinatarios configurados')
                    except Exception as e:
                        st.error(f'Error enviando email: {e}')
                    try:
                        if cfg.get('webhook') and cfg['webhook'].get('url'):
                            send_webhook({'text': 'Hotspots detectados', 'hotspots': hs.to_dict(orient='records')})
                            st.success('Webhook enviado')
                    except Exception as e:
                        st.error(f'Error enviando webhook: {e}')
                if st.button('Ver alertas persistidas'):
                    try:
                        out = list_alerts(200)
                        st.dataframe(out)
                    except Exception as e:
                        st.error(f'No se pudo leer alertas: {e}')


if __name__ == '__main__':
    app()
