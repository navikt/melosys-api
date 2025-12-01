#!/usr/bin/env python3
"""
Merge all batch files into a single final report.

Usage:
    python3 rett_opp_merge_rapport.py [output_dir]

Default output_dir is ./rett-opp-rapport/
"""

import sys
import json
from pathlib import Path
from datetime import datetime


def main():
    output_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("./rett-opp-rapport")

    if not output_dir.exists():
        print(f"ERROR: Directory not found: {output_dir}")
        sys.exit(1)

    batch_files = sorted(output_dir.glob("batch_*.json"))

    if not batch_files:
        print(f"ERROR: No batch files found in {output_dir}")
        sys.exit(1)

    print(f"Merging {len(batch_files)} batch files...")

    all_results = []
    for batch_file in batch_files:
        batch_data = json.loads(batch_file.read_text())
        all_results.extend(batch_data)
        print(f"  {batch_file.name}: {len(batch_data)} items")

    # Save merged report
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    final_file = output_dir / f"full_rapport_{timestamp}.json"
    final_file.write_text(json.dumps(all_results, indent=2, ensure_ascii=False))

    print(f"\nFinal report: {final_file}")
    print(f"Total items: {len(all_results)}")

    # Summary by utfall
    summary = {}
    for item in all_results:
        utfall = item.get("utfall", "UNKNOWN")
        summary[utfall] = summary.get(utfall, 0) + 1

    print("\nSummary by utfall:")
    for utfall, count in sorted(summary.items()):
        print(f"  {utfall}: {count}")

    # Summary by saksstatus
    status_summary = {}
    for item in all_results:
        status = item.get("saksstatus", "UNKNOWN")
        status_summary[status] = status_summary.get(status, 0) + 1

    print("\nSummary by saksstatus:")
    for status, count in sorted(status_summary.items()):
        print(f"  {status}: {count}")


if __name__ == "__main__":
    main()
