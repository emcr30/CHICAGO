import streamlit as st
import pandas as pd
import requests
try:
    # prefer package imports when running from repo root
    import CHICAGO.data as data_module
    from CHICAGO.viz import show_primary_type_bar, show_map_points_and_heat, show_additional_charts
    from CHICAGO.auth import admin_login_ui, admin_logout
except Exception:
    # fallback to local imports when running inside CHICAGO/ folder
    import data as data_module
    from viz import show_primary_type_bar, show_map_points_and_heat, show_additional_charts
    from auth import admin_login_ui, admin_logout
import inspect

DEFAULT_LIMIT = 5000

# Definir zonas de Arequipa
AREQUIPA_ZONES = {
    "Centro Histórico": {
        "bounds": [
            (-16.424240, -71.556179),
            (-16.424528, -71.556496),
            (-16.423735, -71.557225),
            (-16.423735, -71.557225)
        ],
        "center": (-16.424060, -71.556775)
    },
    "Yanahuara": {
        "bounds": [
            (-16.390, -71.545),
            (-16.395, -71.550),
            (-16.400, -71.545),
            (-16.395, -71.540)
        ],
        "center": (-16.395, -71.545)
    }
}

# admin auth is handled by auth.admin_login_ui() and auth.admin_logout()

def admin_panel():
    """Panel de control para administradores"""
    st.sidebar.markdown("---")
    st.sidebar.success("✅ Sesión: Administrador")
    
    if st.sidebar.button("Cerrar Sesión"):
        # use auth helper to clear admin session
        admin_logout()
        st.experimental_rerun()
    
    st.sidebar.markdown("---")
    st.sidebar.subheader("⚙️ Panel de Administración")
    
    # Selección de zona
    zone_name = st.sidebar.selectbox(
        "Zona de Arequipa",
        options=list(AREQUIPA_ZONES.keys())
    )
    
    zone_info = AREQUIPA_ZONES[zone_name]
    
    # Generar datos sintéticos
    st.sidebar.markdown("### 📊 Generar Datos")
    inject_count = st.sidebar.number_input(
        'Registros sintéticos',
        min_value=10,
        max_value=1000,
        value=50,
        step=10
    )
    
    crime_types = st.sidebar.multiselect(
        "Tipos de crimen",
        options=['ROBO', 'ASALTO', 'HURTO', 'VANDALISMO', 'VIOLENCIA FAMILIAR'],
        default=['ROBO', 'ASALTO', 'HURTO']
    )
    
    if st.sidebar.button('🎲 Generar Datos en Zona'):
        # choose an appropriate generator function
        if hasattr(data_module, 'generate_random_records_in_zone'):
            gen_fn = getattr(data_module, 'generate_random_records_in_zone')
            # function signature expected: n, zone_bounds, crime_types, ...
            synth = gen_fn(n=int(inject_count), zone_bounds=zone_info["bounds"], crime_types=crime_types if crime_types else None)
        else:
            # fallback to simple generator that only takes n (generate_random_records)
            gen_fn = getattr(data_module, 'generate_random_records')
            synth = gen_fn(int(inject_count))

        # use the session adder available in the module
        add_fn = getattr(data_module, 'add_records_to_session')
        add_fn(synth)
        st.sidebar.success(f'{len(synth)} registros generados en {zone_name}')
        st.rerun()
    
    # Gestión de base de datos
    st.sidebar.markdown("---")
    st.sidebar.markdown("### 💾 Base de Datos")
    
    col1, col2 = st.sidebar.columns(2)
    with col1:
        if st.button('Guardar', use_container_width=True):
            df = st.session_state.get('_chicago_last_df', pd.DataFrame())
            if not df.empty:
                # persist using function from data module
                persist_fn = getattr(data_module, 'persist_dataframe_to_sqlite')
                persist_fn(df)
                st.sidebar.success('✅ Guardado')
            else:
                st.sidebar.warning('No hay datos')
    
    with col2:
        if st.button('Limpiar', use_container_width=True):
            import os
            try:
                os.remove('chicago.db')
                st.sidebar.success(' DB eliminada')
            except FileNotFoundError:
                st.sidebar.info('No existe DB')
            except Exception as e:
                st.sidebar.error(f'Error: {e}')
    
    # Exportar datos
    st.sidebar.markdown("---")
    st.sidebar.markdown("### 📤 Exportar Datos")
    
    df = st.session_state.get('_chicago_last_df', pd.DataFrame())
    if not df.empty:
        csv = df.to_csv(index=False).encode('utf-8')
        st.sidebar.download_button(
            label="Descargar CSV",
            data=csv,
            file_name=f"crimenes_arequipa_{pd.Timestamp.now().strftime('%Y%m%d')}.csv",
            mime="text/csv",
            use_container_width=True
        )
    
    return zone_name, zone_info

def public_view():
    """Vista pública sin controles de administrador"""
    st.sidebar.info(" Vista Pública")
    st.sidebar.markdown("Los datos se actualizan automáticamente")
    
    # Botón de login discreto
    if st.sidebar.button("Acceso Administrador"):
        st.rerun()

def app():
    st.set_page_config(
        page_title='Sistema de Alertas - Arequipa',
        layout='wide',
        initial_sidebar_state='expanded'
    )
    
    st.title('CRIMENGO')
    
    # Verificar si es admin
    is_admin = admin_login_ui()
    
    # Mostrar panel correspondiente
    if is_admin:
        zone_name, zone_info = admin_panel()
    else:
        public_view()
        zone_name = "Centro Histórico"
        zone_info = AREQUIPA_ZONES[zone_name]
    
    # Configuración de datos
    with st.sidebar:
        st.markdown("---")
        st.subheader("📡 Configuración")
        limit = st.number_input(
            'Registros a mostrar',
            min_value=100,
            max_value=10000,
            value=2000,
            step=100
        )
        
        if is_admin:
            auto_refresh = st.checkbox('Auto-refresh 60s', value=False)
            force_refresh = st.button('🔄 Refrescar Ahora')
        else:
            auto_refresh = True
            force_refresh = False
    
    # Obtener datos
    fetch_fn = getattr(data_module, 'fetch_latest')
    df = fetch_fn(
        limit=int(limit),
        force=force_refresh,
        refresh_interval=60 if auto_refresh else 999999
    )
    
    # Métricas principales
    col1, col2, col3, col4 = st.columns(4)
    
    with col1:
        st.metric("Total Registros", len(df))
    
    with col2:
        if 'date' in df.columns and not df['date'].isna().all():
            latest = df['date'].max()
            st.metric("Último Reporte", latest.strftime('%d/%m/%Y %H:%M') if pd.notna(latest) else 'N/A')
        else:
            st.metric("Último Reporte", "N/A")
    
    with col3:
        arrests = df['arrest'].sum() if 'arrest' in df.columns else 0
        st.metric("Arrestos", int(arrests))
    
    with col4:
        domestic = df['domestic'].sum() if 'domestic' in df.columns else 0
        st.metric("Domésticos", int(domestic))
    
    # Tabs para organizar contenido
    tab1, tab2, tab3 = st.tabs(["Mapa", "Estadísticas", "Datos"])
    
    with tab1:
        st.subheader(f"Mapa de Incidentes - {zone_name}")
        show_map_points_and_heat(df, heat_threshold=30)
    
    with tab2:
        col1, col2 = st.columns(2)
        with col1:
            show_primary_type_bar(df)
        with col2:
            show_additional_charts(df)
    
    with tab3:
        st.subheader("Tabla de Datos")
        
        # Filtros
        col1, col2 = st.columns(2)
        with col1:
            if 'primary_type' in df.columns:
                types = st.multiselect(
                    "Filtrar por tipo",
                    options=df['primary_type'].dropna().unique(),
                    default=None
                )
                if types:
                    df = df[df['primary_type'].isin(types)]
        
        with col2:
            if 'arrest' in df.columns:
                show_arrests = st.checkbox("Solo con arresto", value=False)
                if show_arrests:
                    df = df[df['arrest'] == True]
        
        st.dataframe(
            df,
            use_container_width=True,
            height=400
        )
        
        # Info adicional solo para admin
        if is_admin:
            with st.expander("ℹ️ Información Técnica"):
                st.write("**Columnas disponibles:**", list(df.columns))
                st.write("**Registros nulos por columna:**")
                st.write(df.isnull().sum())

if __name__ == '__main__':
    app()