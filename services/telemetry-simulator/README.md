# Telemetry-Simulator Service

## Lancement local
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8005

## Tests
pytest tests/