import json
import hashlib
from pathlib import Path
from typing import Dict, Any

USERS_PATH = Path('users.json')


def _hash_password(password: str) -> str:
    return hashlib.sha256(password.encode('utf-8')).hexdigest()


def load_users() -> Dict[str, Any]:
    if not USERS_PATH.exists():
        # create default admin user (change password after first run)
        default = {'admin': {'password_hash': _hash_password('admin'), 'is_admin': True}}
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
