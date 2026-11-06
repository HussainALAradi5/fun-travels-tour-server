"""Runs every live suite against a running server on a freshly seeded database and fails if any check fails.

Usage: python -I live-tests/run.py <origin> <seed_password> <database>
Example: python -I live-tests/run.py http://localhost:8080 Seed-Pass-2026 fun_travels_tour

Each suite prints "<passed>/<total> ..." as its last line. The suites create their own data with fixed names, so
run them once per fresh database. rules_check reaches the database with psql (PGHOST, PGPORT, PGUSER, PGPASSWORD).
"""
import os
import re
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
SUITES = os.path.join(HERE, "suites")


def suites(origin, password, database, reports):
    """The suites in the order they were written; later ones rely on nothing from earlier ones but the seed."""
    python = [sys.executable, "-I", "-B"]
    return [
        ("End to end", python + ["end_to_end_check.py", origin, password]),
        ("Mobile contract", python + ["mobile_contract_check.py", origin, password]),
        ("Sign-in and profile", python + ["sign_in_and_profile_check.py", origin, password]),
        ("Mobile screens", python + ["mobile_screens_check.py", origin, password]),
        ("Access and ownership", python + ["access_check.py", origin, password]),
        ("Reports", python + ["report_check.py", origin, password, reports]),
        ("WebSocket", ["node", "websocket_check.mjs", origin, password]),
        ("Business rules", python + ["business_check.py", origin, password]),
        ("Gender", python + ["gender_check.py", origin, password]),
        ("Meals, seats, vehicles, staff", python + ["rules_check.py", origin, password, database]),
    ]


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    origin, password, database = sys.argv[1].rstrip("/"), sys.argv[2], sys.argv[3]
    total_passed = total_checks = 0
    failures = []
    with tempfile.TemporaryDirectory() as reports:
        for name, command in suites(origin, password, database, reports):
            result = subprocess.run(command, cwd=SUITES, capture_output=True, text=True)
            output = (result.stdout + result.stderr).strip()
            last = output.splitlines()[-1] if output else ""
            score = re.match(r"(\d+)/(\d+)", last)
            passed, checks = (int(score.group(1)), int(score.group(2))) if score else (0, 0)
            ok = result.returncode == 0 and score is not None and passed == checks
            total_passed, total_checks = total_passed + passed, total_checks + checks
            print(f"{'PASS' if ok else 'FAIL'}  {name:<32} {last}")
            if not ok:
                failures.append((name, output))
    print(f"\n{total_passed}/{total_checks} live checks passed")
    for name, output in failures:
        print(f"\n--- {name} ---\n{output[-3000:]}")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
