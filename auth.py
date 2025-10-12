
import json
import hashlib
from pathlib import Path
from typing import Dict, Any, Optional, Union
import streamlit as st

USERS_PATH: Path = Path('users.json')


def _hash_password(password: str) -> str:
    return hashlib.sha256(password.encode('utf-8')).hexdigest()


def load_users() -> Dict[str, Any]:
    if not USERS_PATH.exists():
        default = {'admin': {'password_hash': _hash_password('admin123'), 'is_admin': True}}
        save_users(default)
        return default
    try:
        with USERS_PATH.open('r', encoding='utf-8') as f:
            return json.load(f)
    except Exception:
        return {}


def save_users(users: Dict[str, Any]) -> None:
    with USERS_PATH.open('w', encoding='utf-8') as f:
        json.dump(users, f, indent=2)


def create_user(username: str, password: str, is_admin: bool = False) -> bool:
    users = load_users()
    if username in users:
        return False
    users[username] = {'password_hash': _hash_password(password), 'is_admin': bool(is_admin)}
    save_users(users)
    return True


def verify_user(username: str, password: str) -> bool:
    users = load_users()
    if username not in users:
        return False
    return users[username].get('password_hash') == _hash_password(password)


def is_admin(username: str) -> bool:
    users = load_users()
    return bool(users.get(username, {}).get('is_admin', False))


def admin_login_ui() -> bool:
    """Show admin login UI in the sidebar. Returns True if admin session active."""
    if 'is_admin' not in st.session_state:
        st.session_state['is_admin'] = False
    if st.session_state.get('is_admin'):
        return True

    with st.sidebar:
        st.subheader("Acceso Administrador")
        # use stable keys for the inputs so session state can persist
        username = st.text_input("Usuario", key="admin_user")
        password = st.text_input("Contraseña", type="password", key="admin_pass")
        if st.button("Iniciar Sesión", key="admin_login_btn"):
            # verify user exists and has admin flag
            if verify_user(username, password) and is_admin(username):
                st.session_state['is_admin'] = True
                st.session_state['_admin_user'] = username
                st.success(f'Ingresado como administrador: {username}')
                # avoid calling experimental_rerun which raises a rerun exception
                # instead return True so caller will render admin UI on next interaction
                return True
            else:
                st.error("Credenciales incorrectas o usuario no es admin")
    return False


def admin_logout() -> None:
    """Logout admin session."""
    if 'is_admin' in st.session_state:
        st.session_state['is_admin'] = False
    if '_admin_user' in st.session_state:
        st.session_state['_admin_user'] = None
    # don't rerun here; caller can rerun if desired


def current_admin() -> Optional[str]:
    """Return username of logged admin or None."""
    return st.session_state.get('_admin_user')
