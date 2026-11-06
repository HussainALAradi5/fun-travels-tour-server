"""Live checks for the user gender. Usage: python -I gender_check.py <origin> <seed_password>"""
import json
import sys
import time
import urllib.error
import urllib.request

BASE = sys.argv[1] + "/api"
PW = sys.argv[2]
passed = failed = 0


def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path, method=method, data=json.dumps(body).encode() if body is not None else None)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or b"{}")


def check(name, ok, detail=""):
    global passed, failed
    if ok:
        passed += 1
    else:
        failed += 1
        print("FAIL", name, str(detail)[:300])


stamp = str(int(time.time()))[-6:]
person = {"userName": "g" + stamp, "name": "Gina Gender", "email": f"g{stamp}@example.com", "password": "Secret-123",
          "mobileNumber": "+97331" + stamp}
st, b = call("POST", "/auth/register", person)
check("sign-up without a gender is rejected with a field error", st == 400 and "gender" in json.dumps(b.get("fieldErrors", {})).lower(), (st, b))
st, b = call("POST", "/auth/register", {**person, "gender": "OTHER"})
check("only MALE or FEMALE is accepted", st == 400, (st, b))
st, b = call("POST", "/auth/register", {**person, "gender": "FEMALE"})
check("sign-up with FEMALE is stored and returned", st == 201 and b["data"]["gender"] == "FEMALE", (st, b))

tok = call("POST", "/auth/login", {"identifier": person["userName"], "password": person["password"]})[1]["data"]["token"]
st, prof = call("GET", "/users/me/profile", token=tok)
uid = prof["data"]["id"]
check("profile shows the gender", st == 200 and prof["data"]["gender"] == "FEMALE", prof)
st, b = call("PUT", f"/users/{uid}", {"gender": "MALE"}, tok)
check("a user can change their own gender", st == 200 and b["data"]["gender"] == "MALE", (st, b))
st, b = call("PUT", f"/users/{uid}", {"name": "Gina G"}, tok)
check("editing other fields keeps the gender", st == 200 and b["data"]["gender"] == "MALE", (st, b))

owner = call("POST", "/auth/login", {"identifier": "owner", "password": PW})[1]["data"]["token"]
staff = {"userName": "gs" + stamp, "name": "Sam Staff", "email": f"gs{stamp}@example.com", "password": "Secret-123",
         "mobileNumber": "+97332" + stamp, "userType": "EMPLOYEE"}
st, b = call("POST", "/users/add-employee", staff, owner)
check("staff without a gender is rejected", st == 400, (st, b))
st, b = call("POST", "/users/add-employee", {**staff, "gender": "MALE"}, owner)
check("staff with a gender is created", st == 201 and b["data"]["gender"] == "MALE", (st, b))
st, b = call("GET", "/agencies/1/employees?size=50", token=owner)
d = b.get("data") or []
rows = d.get("content", []) if isinstance(d, dict) else d
check("staff lists include the gender", any(r.get("gender") for r in rows), rows[:2])
st, b = call("GET", "/users/me/profile", token=owner)
check("seeded accounts have a gender", b["data"]["gender"] in ("MALE", "FEMALE"), b)

print(f"{passed}/{passed + failed} gender checks passed")
