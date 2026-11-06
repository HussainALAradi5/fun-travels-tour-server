"""Live checks for server reports and media. Usage: python -I report_check.py <origin> <seed_password> <out_dir>"""
import json
import os
import sys
import urllib.error
import urllib.request

BASE, PW, OUT = sys.argv[1] + "/api", sys.argv[2], sys.argv[3]
os.makedirs(OUT, exist_ok=True)
ok = []


def call(method, path, body=None, token=None, raw=False):
    req = urllib.request.Request(BASE + path, data=None if body is None else json.dumps(body).encode(), method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            data = r.read()
            return r.status, (data if raw else json.loads(data or b"{}")), dict(r.headers)
    except urllib.error.HTTPError as e:
        data = e.read()
        try:
            return e.code, json.loads(data or b"{}"), {}
        except ValueError:
            return e.code, {"raw": data[:200]}, {}


def check(name, cond, detail=""):
    ok.append(bool(cond))
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f" -> {str(detail)[:300]}"))


def token(user):
    return call("POST", "/auth/login", {"identifier": user, "password": PW})[1]["data"]["token"]


admin, owner, cust, emp = token("admin"), token("owner"), token("customer"), token("employee")
tours = call("GET", "/tours/catalog?size=20", token=cust)[1]["data"]["content"]
tour_id = tours[0]["id"]

cases = [("TOURS", {}, admin), ("TOUR_CATALOG", {}, cust), ("TICKETS", {}, cust), ("TRANSACTIONS", {}, cust),
         ("PASSENGER_MANIFEST", {"tourId": tour_id}, owner), ("TRANSPORTATIONS", {}, owner), ("MEALS", {}, emp),
         ("USERS", {}, admin), ("AGENCY_BRANCHES", {"agencyId": 1}, owner), ("AGENCY_EMPLOYEES", {"agencyId": 1}, owner)]
first_media = None
for rtype, extra, tok in cases:
    for fmt in ("PDF", "XLSX"):
        st, b, _ = call("POST", "/reports", {"type": rtype, "format": fmt, **extra}, tok)
        m = (b or {}).get("data") or {}
        good = st == 201 and m.get("downloadPath") and m.get("reportType") == rtype and m.get("reportFormat") == fmt
        check(f"{rtype} {fmt} generated", good, (st, b))
        if not good:
            continue
        st, data, headers = call("GET", f"/media/{m['id']}/download", token=tok, raw=True)
        magic = data[:4] == (b"%PDF" if fmt == "PDF" else b"PK\x03\x04")
        disp = headers.get("Content-Disposition", "")
        check(f"{rtype} {fmt} downloads ({m['recordCount']} rows, {len(data)} bytes)", st == 200 and magic and m["fileName"] in disp, (st, disp))
        open(os.path.join(OUT, m["fileName"]), "wb").write(data)
        first_media = first_media or (m, tok)

# --- permissions ---
check("customer cannot export tour inventory", call("POST", "/reports", {"type": "TOURS", "format": "PDF"}, cust)[0] == 403)
check("employee cannot export user directory", call("POST", "/reports", {"type": "USERS", "format": "PDF"}, emp)[0] == 403)
check("manifest without tourId is rejected", call("POST", "/reports", {"type": "PASSENGER_MANIFEST", "format": "PDF"}, owner)[0] in (400, 409))
check("missing type/format is 400", call("POST", "/reports", {"format": "PDF"}, admin)[0] == 400)
check("reports need sign-in", call("POST", "/reports", {"type": "TICKETS", "format": "PDF"})[0] in (401, 403))

# --- media ownership and listing ---
m, tok = first_media
check("owner of a file cannot be another user", call("GET", f"/media/{m['id']}/download", token=cust, raw=True)[0] == 403)
st, b, _ = call("GET", f"/media/{m['id']}/download", token=admin, raw=True)
check("admin can download any file", st == 200)
st, b, _ = call("GET", "/media?category=REPORT&size=50", token=cust)
mine = b["data"]["content"]
check("customer lists only own reports", st == 200 and mine and all(x["reportType"] in ("TOUR_CATALOG", "TICKETS", "TRANSACTIONS") for x in mine), b)
check("storage path never exposed", all("storagePath" not in x for x in mine))
st, _, _ = call("GET", "/uploads/../media", token=None, raw=True)
victim = mine[0]["id"]
check("other users cannot delete my file", call("DELETE", f"/media/{victim}", token=owner)[0] == 403)
check("I can delete my file", call("DELETE", f"/media/{victim}", token=cust)[0] == 200)
check("deleted file is gone", call("GET", f"/media/{victim}/download", token=cust, raw=True)[0] == 404)
print(f"\n{sum(ok)}/{len(ok)} report checks passed")
