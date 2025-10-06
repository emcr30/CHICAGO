import smtplib
import json
from email.message import EmailMessage
from pathlib import Path
import requests

CONFIG_PATH = Path('notify_config.json')


def load_config():
    if not CONFIG_PATH.exists():
        return {}
    try:
        with CONFIG_PATH.open('r', encoding='utf-8') as f:
            return json.load(f)
    except Exception:
        return {}


def send_email(subject: str, body: str, to_addrs: list[str]):
    cfg = load_config().get('email') or {}
    smtp_host = cfg.get('smtp_host')
    smtp_port = cfg.get('smtp_port', 587)
    username = cfg.get('username')
    password = cfg.get('password')
    from_addr = cfg.get('from_addr') or username
    if not smtp_host or not username or not password:
        raise RuntimeError('Email not configured in notify_config.json')
    msg = EmailMessage()
    msg['Subject'] = subject
    msg['From'] = from_addr
    msg['To'] = ','.join(to_addrs)
    msg.set_content(body)
    with smtplib.SMTP(smtp_host, smtp_port) as s:
        s.starttls()
        s.login(username, password)
        s.send_message(msg)


def send_webhook(payload: dict):
    cfg = load_config().get('webhook') or {}
    url = cfg.get('url')
    if not url:
        raise RuntimeError('Webhook not configured in notify_config.json')
    resp = requests.post(url, json=payload, timeout=15)
    resp.raise_for_status()
    return resp
