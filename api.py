from flask import Flask, request, jsonify
from flask_cors import CORS
from typing import Any, Dict, List
import json
from datetime import datetime

import db_postgres as db

app = Flask(__name__)
CORS(app)


def _serialize_row(row: Dict[str, Any]) -> Dict[str, Any]:
    out = {}
    for k, v in row.items():
        if isinstance(v, datetime):
            out[k] = v.isoformat()
        else:
            try:
                json.dumps(v)
                out[k] = v
            except Exception:
                out[k] = str(v)
    return out


@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok'})


@app.route('/records', methods=['GET'])
def get_records():
    try:
        limit = int(request.args.get('limit', 1000))
    except Exception:
        limit = 1000
    try:
        rows = db.fetch_latest_crimes(limit=limit)
        rows = [_serialize_row(r) for r in rows]
        return jsonify({'count': len(rows), 'records': rows})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/records/<string:crime_id>', methods=['GET'])
def get_record(crime_id: str):
    try:
        rec = db.fetch_crime_by_id(crime_id)
        if rec is None:
            return jsonify({'error': 'Not found'}), 404
        return jsonify(_serialize_row(rec))
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/records', methods=['POST'])
def post_records():
    try:
        payload = request.get_json()
        if payload is None:
            return jsonify({'error': 'Invalid JSON payload'}), 400
        # Accept either single object or list
        records: List[Dict[str, Any]] = []
        if isinstance(payload, list):
            records = payload
        elif isinstance(payload, dict):
            # API clients might send a wrapper {"records": [...]}
            if 'records' in payload and isinstance(payload['records'], list):
                records = payload['records']
            else:
                records = [payload]
        else:
            return jsonify({'error': 'Invalid payload format'}), 400

        db.insert_crimes(records)
        return jsonify({'status': 'ok', 'inserted': len(records)})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/records/<string:crime_id>', methods=['PUT'])
def put_record(crime_id: str):
    try:
        payload = request.get_json()
        if not isinstance(payload, dict):
            return jsonify({'error': 'Invalid JSON payload'}), 400
        # ensure id set
        payload['id'] = crime_id
        db.insert_crimes([payload])
        return jsonify({'status': 'ok'})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/records/<string:crime_id>', methods=['DELETE'])
def delete_record(crime_id: str):
    try:
        deleted = db.delete_crime_by_id(crime_id)
        if not deleted:
            return jsonify({'error': 'Not found'}), 404
        return jsonify({'status': 'deleted'})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    # Para desarrollo
    app.run(host='0.0.0.0', port=5000)
