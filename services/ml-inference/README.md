# ML-Inference Service

## Lancement local
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8002

## Tests
pytest tests/