from pathlib import Path
import json

CONTRACTS_DIR = Path(__file__).resolve().parent.parent.parent.parent / "contracts"

with open(CONTRACTS_DIR / "anomaly-score.schema.json", encoding="utf-8") as f:
    ANOMALY_SCORE_SCHEMA = json.load(f)