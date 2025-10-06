import streamlit as st
import pandas as pd
from data import fetch_latest, generate_random_records, add_records_to_session, persist_dataframe_to_sqlite
from viz import show_primary_type_bar, show_map_points_and_heat, show_additional_charts


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
    show_map_points_and_heat(df, heat_threshold=50)


if __name__ == '__main__':
    app()
