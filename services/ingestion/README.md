# Ingestion Service

## Lancement local
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001

## Tests
pytest tests/