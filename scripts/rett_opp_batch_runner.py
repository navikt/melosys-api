#!/usr/bin/env python3
"""
Batch runner for RettOppFeilMedlPerioderJob.

Runs batches one at a time, saving results locally.
Can resume from where it left off if interrupted.

Usage:
    1. Connect naisdevice: naisdevice connect
    2. Set API key: export MELOSYS_ADMIN_APIKEY="your-prod-api-key"
    3. Run: python3 rett_opp_batch_runner.py

Results will be saved in ./rett-opp-rapport/
Use rett_opp_merge_rapport.py to merge all batch files into final report.
"""

import os
import sys
import requests
import json
import time
from pathlib import Path
from datetime import datetime

# Configuration
# Use kubectl port-forward to avoid ingress timeout:
#   kubectl port-forward -n teammelosys deploy/melosys-api 8080:8080
BASE_URL = os.environ.get("MELOSYS_API_URL", "http://localhost:8080")
API_KEY = os.environ.get("MELOSYS_ADMIN_APIKEY", "")
BATCH_SIZE = int(os.environ.get("BATCH_SIZE", "50"))  # Can be larger with port-forward (no 30s ingress timeout)
DRY_RUN = os.environ.get("DRY_RUN", "true").lower() == "true"
OUTPUT_DIR = Path(os.environ.get("OUTPUT_DIR", "./rett-opp-rapport"))
DELAY_BETWEEN_BATCHES = int(os.environ.get("DELAY_SECONDS", "1"))
REQUEST_TIMEOUT = int(os.environ.get("REQUEST_TIMEOUT", "120"))  # 2 min - no ingress timeout with port-forward


def main():
    if not API_KEY:
        print("ERROR: MELOSYS_ADMIN_APIKEY environment variable not set!")
        print("Usage: export MELOSYS_ADMIN_APIKEY='your-api-key'")
        sys.exit(1)

    print(f"Configuration:")
    print(f"  BASE_URL: {BASE_URL}")
    print(f"  BATCH_SIZE: {BATCH_SIZE}")
    print(f"  DRY_RUN: {DRY_RUN}")
    print(f"  OUTPUT_DIR: {OUTPUT_DIR}")
    print(f"  DELAY_BETWEEN_BATCHES: {DELAY_BETWEEN_BATCHES}s")
    print(f"  REQUEST_TIMEOUT: {REQUEST_TIMEOUT}s")
    print()

    OUTPUT_DIR.mkdir(exist_ok=True)
    progress_file = OUTPUT_DIR / "progress.json"

    # Resume from progress if exists
    if progress_file.exists():
        progress = json.loads(progress_file.read_text())
        next_id = progress.get("next_start_fra_behandling_id", 0)
        print(f"Resuming from behandlingId {next_id}")
        print(f"Previous progress: {progress['batches_completed']} batches, {progress['total_items']} items")
    else:
        next_id = 0
        progress = {
            "batches_completed": 0,
            "total_items": 0,
            "started_at": datetime.now().isoformat()
        }

    headers = {"X-MELOSYS-ADMIN-APIKEY": API_KEY}

    try:
        while True:
            print(f"\n[{datetime.now().strftime('%H:%M:%S')}] Batch from ID {next_id}...", end=" ", flush=True)

            try:
                response = requests.post(
                    f"{BASE_URL}/admin/rett-opp-feil-medl-perioder/kjor-en-batch",
                    params={
                        "dryRun": str(DRY_RUN).lower(),
                        "batchStørrelse": BATCH_SIZE,
                        "startFraBehandlingId": next_id
                    },
                    headers=headers,
                    timeout=REQUEST_TIMEOUT
                )
                response.raise_for_status()
            except requests.exceptions.Timeout:
                print(f"TIMEOUT!")
                print(f"  Request timed out after {REQUEST_TIMEOUT}s. Try reducing BATCH_SIZE.")
                print("  Progress saved. Run again to resume.")
                break
            except requests.exceptions.RequestException as e:
                print(f"ERROR!")
                print(f"  Request failed: {e}")
                print("  Progress saved. Run again to resume.")
                break

            result = response.json()

            # Save batch results
            batch_file = OUTPUT_DIR / f"batch_{next_id:010d}.json"
            batch_file.write_text(json.dumps(result["rapport"], indent=2, ensure_ascii=False))

            items_this_batch = len(result["rapport"])
            scenario1_count = result["scenario1"]["hentetDenneBatch"]
            scenario2_count = result["scenario2"]["hentetDenneBatch"]

            progress["batches_completed"] += 1
            progress["total_items"] += items_this_batch
            progress["next_start_fra_behandling_id"] = result["nextStartFraBehandlingId"]
            progress["last_updated"] = datetime.now().isoformat()
            progress_file.write_text(json.dumps(progress, indent=2))

            print(f"S1:{scenario1_count} S2:{scenario2_count} -> {items_this_batch} items. Total: {progress['total_items']}")

            if not result["hasMoreItems"]:
                print("\n" + "=" * 50)
                print("ALL BATCHES COMPLETE!")
                print(f"Total items: {progress['total_items']}")
                print(f"Total batches: {progress['batches_completed']}")
                print(f"\nRun: python3 rett_opp_merge_rapport.py")
                break

            next_id = result["nextStartFraBehandlingId"]
            time.sleep(DELAY_BETWEEN_BATCHES)

    except KeyboardInterrupt:
        print("\n\nInterrupted. Progress saved.")
        print(f"Run again to resume from ID {progress.get('next_start_fra_behandling_id', next_id)}")


if __name__ == "__main__":
    main()
