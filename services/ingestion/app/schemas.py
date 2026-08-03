import json
from pathlib import Path

CONTRACTS_DIR = Path(__file__).resolve().parent.parent.parent.parent / "contracts"

with open(CONTRACTS_DIR / "telemetry.schema.json", encoding="utf-8") as f:
    TELEMETRY_SCHEMA = json.load(f)
