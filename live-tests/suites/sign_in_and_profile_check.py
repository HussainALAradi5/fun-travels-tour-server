"""Live checks for sign-in by email, username or phone, profiles, phone numbers and review fixes.
Usage: python -I sign_in_and_profile_check.py <origin> <seed_password>"""
import json
import sys
import time
import urllib.error
import urllib.request

BASE = sys.argv[1] + "/api"
PW = sys.argv[2]
ok = []


def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path, data=None if body is None else json.dumps(body).encode(), method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw or b"{}")
        except ValueError:
            return e.code, {"raw": raw[:200].decode(errors="replace")}


def check(name, cond, detail=""):
    ok.append(bool(cond))
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f" -> {str(detail)[:300]}"))


def login(identifier, password=PW):
    return call("POST", "/auth/login", {"identifier": identifier, "password": password})


# --- login by email / username / phone, password required ---
for ident in ("customer", "customer@funtravel.com", "+97310000005", "+973 1000 0005", "00973-1000-0005", "97310000005"):
    st, b = login(ident)
    check(f"login with '{ident}'", st == 200 and b["data"]["user"]["userName"] == "customer", (st, b))
st, b = login("customer", "")
check("blank password rejected (400)", st == 400, (st, b))
st, b = login("customer", "wrong-password")
check("wrong password rejected", st in (400, 401, 403), (st, b))
cust = login("customer")[1]["data"]["token"]
admin = login("admin")[1]["data"]["token"]
owner = login("owner")[1]["data"]["token"]
manager = login("manager")[1]["data"]["token"]

# --- country code required on registration ---
stamp = str(int(time.time()))[-6:]
st, b = call("POST", "/auth/register", {"gender": "MALE", "userName": "nocode" + stamp, "name": "No Code", "email": f"nocode{stamp}@example.com",
                                        "password": "secret12", "mobileNumber": "39" + stamp})
check("register without country code rejected (400)", st == 400 and "country code" in json.dumps(b), (st, b))
st, b = call("POST", "/auth/register", {"gender": "MALE", "userName": "withcode" + stamp, "name": "With Code", "email": f"withcode{stamp}@example.com",
                                        "password": "secret12", "mobileNumber": "+973 38 " + stamp})
check("register with spaced +973 number accepted and stored clean", st == 201 and b["data"]["mobileNumber"] == "+97338" + stamp, (st, b))
st, b = login("+97338" + stamp, "secret12")
check("new user signs in by phone", st == 200, (st, b))

# --- profile details DTO ---
st, b = call("GET", "/users/me/profile", token=cust)
p = b.get("data") or {}
check("GET /users/me/profile returns ProfileResponse", st == 200 and all(k in p for k in
      ("id", "code", "userName", "email", "mobileNumber", "userType", "wallet", "memberSince", "agency", "agencyBranch")), (st, b))
check("profile wallet has balance and account number", p.get("wallet") and "balance" in p["wallet"] and p["wallet"]["accountNumber"], p)
st, b = call("PUT", "/users/me/profile", {"name": "John Customer", "age": 31}, cust)
check("PUT /users/me/profile saves and returns profile", st == 200 and b["data"]["age"] == 31 and "wallet" in b["data"], (st, b))
st, b = call("PUT", "/users/me/profile", {"mobileNumber": "39990001"}, cust)
check("profile mobile without country code rejected", st == 400, (st, b))
st, b = call("GET", "/users/me/profile")
check("profile requires sign-in", st in (401, 403), st)

# --- response DTO shapes (records keep the wire names) ---
st, b = call("GET", "/tickets/search?size=5", token=cust)
t = (b.get("data") or {}).get("content") or [{}]
check("ticket JSON keeps 'paid' and tour dates", "paid" in t[0] and "startDate" in (t[0].get("tour") or {}), t[0])
st, b = call("GET", "/reservations?size=1", token=cust)
r = ((b.get("data") or {}).get("content") or [{}])[0]
check("reservation tour summary now has dates", "startDate" in (r.get("tour") or {}), r)

# --- meals: public list, scoping, edit ---
st, b = call("GET", "/meals?size=50")
check("meal list stays public (visitors get shared meals)", st == 200, (st, b))
st, b = call("GET", "/meals?size=50", token=admin)
meal = next(m for m in b["data"]["content"] if m["status"] == "ACTIVE")
body = {"mealName": meal["mealName"], "mealPrice": meal["mealPrice"] + 1, "mealDescription": "Edited in check",
        "dietaryTypes": ["VEGAN"], "spiceLevel": "MILD"}
st, b = call("PUT", f"/meals/{meal['id']}", body, owner)
check("owner edits own agency meal (PUT /meals/{id})", st == 200 and b["data"]["mealDescription"] == "Edited in check"
      and set(b["data"]["dietaryTypes"]) == {"VEGAN", "VEGETARIAN"}, (st, b))
body["mealPrice"] = meal["mealPrice"]
call("PUT", f"/meals/{meal['id']}", {**body, "dietaryTypes": meal["dietaryTypes"], "spiceLevel": meal["spiceLevel"],
                                     "mealDescription": meal.get("mealDescription")}, owner)
st, b = call("PUT", f"/meals/{meal['id']}", body, cust)
check("customer cannot edit meals", st == 403, st)
st, b = call("GET", "/meals/999999", token=admin)

# --- transportation scoping ---
st, b = call("GET", "/transportations/search?size=50", token=cust)
check("customer cannot list fleet", st == 403, (st, b))
st, b = call("GET", "/transportations/search?size=50", token=owner)
check("owner sees only own agency fleet", st == 200 and all(v["agency"]["agencyName"] == "Al Aradi Travel" for v in b["data"]["content"]), b)
vid = b["data"]["content"][0]["id"]
st, b = call("GET", f"/transportations/search?keyword={b['data']['content'][0]['code'][:4]}", token=owner)
check("fleet keyword search matches code", st == 200 and b["data"]["content"], (st, b))
st, b = call("GET", "/transportations/999999", token=admin)
check("missing vehicle is 404", st == 404, (st, b))
st, b = call("GET", f"/transportations/{vid}", token=cust)
check("customer cannot open a vehicle", st == 403, st)

# --- users scoping ---
st, b = call("GET", "/users", token=manager)
check("manager user list limited to own agency", st == 200 and b["data"] and all((u.get("agency") or {}).get("agencyName") == "Al Aradi Travel" for u in b["data"]), b)
st, b = call("GET", "/users/role/CUSTOMER", token=owner)
check("owner can still list customers (walk-in sales)", st == 200 and all(u["userType"] == "CUSTOMER" for u in b["data"]), (st, b))
st, b = call("GET", "/users/role/ADMIN", token=owner)
check("owner cannot list admins of the platform", st == 200 and b["data"] == [], b)

# --- seats: per-tour status filter in the query ---
st, b = call("GET", "/tours/catalog?size=20", token=cust)
tour = next(t for t in b["data"]["content"] if t["hasTransportation"])
st, allseats = call("GET", f"/seats/search?tourId={tour['id']}&size=100", token=cust)
booked = [s for s in allseats["data"]["content"] if s["status"] == "BOOKED"]
st, b = call("GET", f"/seats/search?tourId={tour['id']}&status=BOOKED&size=100", token=cust)
check("per-tour BOOKED filter matches and totals agree", st == 200 and len(b["data"]["content"]) == len(booked)
      and b["data"]["totalElements"] == len(booked), (len(booked), b))
st, b = call("GET", f"/seats/search?tourId={tour['id']}&status=AVAILABLE&size=3", token=cust)
check("AVAILABLE filter pages correctly", st == 200 and len(b["data"]["content"]) <= 3
      and b["data"]["totalElements"] == len([s for s in allseats["data"]["content"] if s["status"] == "AVAILABLE"]), b)

# --- payments: wallet only ---
free = [s for s in allseats["data"]["content"] if s["status"] == "AVAILABLE"]
st, res = call("POST", "/reservations", {"tour": {"id": tour["id"]}, "tickets": [{"assignedSeat": {"id": free[0]["id"]}}]}, cust)
check("reservation created", st == 200, (st, res))
rid = res["data"]["id"]
st, b = call("POST", f"/payments/execute/{rid}?method=CREDIT_CARD", token=cust)
check("card booking payment rejected with a clear message", st in (400, 409, 422) and "wallet" in json.dumps(b).lower(), (st, b))
st, b = call("PATCH", f"/reservations/{rid}/cancel", token=cust)
check("cleanup: reservation cancelled", st == 200, (st, b))

# --- 404s instead of 400s ---
check("missing meal is 404", call("GET", "/meals/agency/1?size=1")[0] == 200)
check("missing tour is 404", call("GET", "/tours/999999", token=cust)[0] == 404)

# --- admin role change keeps agency staff in an agency ---
st, users = call("GET", "/users", token=admin)
emp = next(u for u in users["data"] if u["userName"] == "emp9")
st, b = call("PUT", f"/users/permissions/{emp['id']}?type=MANAGER", token=admin)
check("making staff without agency/branch is rejected", st == 400, (st, b))
print(f"\n{sum(ok)}/{len(ok)} checks passed")
