# Alerting Service

## Lancement local
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8003

## Tests
pytest tests/