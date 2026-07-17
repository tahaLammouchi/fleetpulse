from pathlib import Path
import json

CONTRACTS_DIR = Path(__file__).resolve().parent.parent.parent.parent / "contracts"

with open(CONTRACTS_DIR / "alert-event.schema.json", encoding="utf-8") as f:
    ALERT_EVENT_SCHEMA = json.load(f)