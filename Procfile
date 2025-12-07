web: gunicorn -w 4 -b 0.0.0.0:$PORT api:app
streamlit: streamlit run main.py --server.port=$PORT --server.address=0.0.0.0
