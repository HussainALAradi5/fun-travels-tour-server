"""Live checks for the access and logic fixes. Usage: python -I access_check.py <origin> <seed_password>"""
import json
import sys
import time
import urllib.error
import urllib.request

BASE, PW = sys.argv[1] + "/api", sys.argv[2]
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
        try:
            return e.code, json.loads(e.read() or b"{}")
        except ValueError:
            return e.code, {}


def check(name, cond, detail=""):
    ok.append(bool(cond))
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f" -> {str(detail)[:300]}"))


def token(user):
    return call("POST", "/auth/login", {"identifier": user, "password": PW})[1]["data"]["token"]


admin, owner, manager, emp, cust = (token(u) for u in ("admin", "owner", "manager", "employee", "customer"))
stamp = str(int(time.time()))[-6:]

# --- branches and agencies: admin or the agency's own staff only
for who, tok in (("customer", cust),):
    check(f"{who} cannot list agency branches", call("GET", "/branches/agency/1", token=tok)[0] == 403)
    check(f"{who} cannot search agency branches", call("GET", "/branches/agency/1/search", token=tok)[0] == 403)
    check(f"{who} cannot open agency details", call("GET", "/agencies/1", token=tok)[0] == 403)
    check(f"{who} cannot list agency staff", call("GET", "/agencies/1/employees", token=tok)[0] == 403)
    check(f"{who} cannot search agencies", call("GET", "/agencies/search", token=tok)[0] == 403)
for who, tok in (("owner", owner), ("manager", manager), ("employee", emp), ("admin", admin)):
    check(f"{who} lists agency branches", call("GET", "/branches/agency/1", token=tok)[0] == 200)
check("owner opens own agency", call("GET", "/agencies/1", token=owner)[0] == 200)

# a second agency with its own manager
st, other = call("POST", "/agencies", {"agencyName": "Other Agency " + stamp, "address": "Riffa"}, admin)
check("admin creates a second agency", st == 200, (st, other))
other_id = other["data"]["id"]
st, mgr = call("POST", f"/users/add-employee?agencyId={other_id}", {"gender": "MALE", "userName": "omgr" + stamp, "name": "Other Manager",
               "email": f"omgr{stamp}@example.com", "password": "secret12", "mobileNumber": "+97335" + stamp, "age": 30,
               "userType": "MANAGER"}, admin)
check("admin adds a manager to the second agency", st == 201, (st, mgr))
check("owner cannot read another agency", call("GET", f"/agencies/{other_id}", token=owner)[0] == 403)
check("owner cannot list another agency's branches", call("GET", f"/branches/agency/{other_id}", token=owner)[0] == 403)
check("owner cannot list another agency's staff", call("GET", f"/agencies/{other_id}/employees", token=owner)[0] == 403)
st, b = call("GET", "/agencies/search", token=owner)
check("owner's agency search returns only own agency", st == 200 and [a["id"] for a in b["data"]["content"]] == [1], b)
st, b = call("GET", "/agencies", token=owner)
check("owner's agency list returns only own agency", st == 200 and [a["id"] for a in b["data"]] == [1], b)
st, b = call("GET", "/agencies", token=admin)
check("admin still lists every agency", st == 200 and len(b["data"]) >= 2, b)
st, b = call("POST", "/branches/agency/1", {"branchName": "Stolen " + stamp, "branchManagerId": mgr["data"]["id"]}, owner)
check("owner cannot pull another agency's manager into a branch", st == 400 and "another agency" in json.dumps(b), (st, b))

# --- tour visibility (any city with its own country)
_city = call("GET", "/cities", token=owner)[1]["data"]
_city = (_city if isinstance(_city, list) else _city["content"])[0]
_cc = _city.get("countryId") or (_city.get("country") or {}).get("id")
st, created = call("POST", "/tours", {"tourNumber": "T-H" + stamp, "title": "Hidden draft", "basePrice": 80, "numberOfDays": 2,
                   "startDate": "2027-06-01", "endDate": "2027-06-02", "maxCapacity": 4, "startCountryId": _cc, "endCountryId": _cc,
                   "startCityId": _city["id"], "endCityId": _city["id"]}, owner)
check("owner creates a pending tour (no price field needed)", st in (200, 201) and created["data"]["status"] == "PENDING", (st, created))
hid = created["data"]["id"]
check("visitor gets 404 for a pending tour", call("GET", f"/tours/{hid}")[0] == 404)
check("customer gets 404 for a pending tour", call("GET", f"/tours/{hid}", token=cust)[0] == 404)
check("owning agency's staff can open it", call("GET", f"/tours/{hid}", token=emp)[0] == 200)
check("admin can open it", call("GET", f"/tours/{hid}", token=admin)[0] == 200)
st, b = call("GET", "/tours/search?size=100")
check("visitor tour search shows only ACTIVE tours", st == 200 and b["data"]["content"] and all(t["status"] == "ACTIVE" for t in b["data"]["content"]), b)
st, b = call("GET", "/tours?size=100", token=cust)
check("customer tour list shows only ACTIVE tours", st == 200 and all(t["status"] == "ACTIVE" for t in b["data"]["content"]), b)
st, b = call("GET", "/tours/search?size=100", token=admin)
check("admin search still includes the pending tour", st == 200 and any(t["id"] == hid for t in b["data"]["content"]), b)
st, tickets = call("GET", "/tickets/search?size=50", token=cust)
held = [t for t in tickets["data"]["content"]]
if held:
    tid = held[0]["tour"]["id"]
    check("customer can open a tour they hold a ticket for", call("GET", f"/tours/{tid}", token=cust)[0] == 200)

# --- available places counted from tickets
st, cat = call("GET", "/tours/catalog?size=20", token=cust)
tour = next(t for t in cat["data"]["content"] if t["availableSlots"] > 0)
st, res = call("POST", "/reservations", {"tour": {"id": tour["id"]}, "requestedSlots": 1}, cust)
check("booking succeeds", st == 200, (st, res))
st, all_tickets = call("GET", f"/tickets/search?tourId={tour['id']}&size=100", token=admin)
active = sum(1 for t in all_tickets["data"]["content"] if t["ticketStatus"] != "CANCELLED")
st, after = call("GET", f"/tours/{tour['id']}", token=admin)
check("available places = capacity - active tickets", after["data"]["availableSlots"] == after["data"]["maxCapacity"] - active,
      (after["data"]["availableSlots"], after["data"]["maxCapacity"], active))
call("PATCH", f"/reservations/{res['data']['id']}/cancel", token=cust)
st, after = call("GET", f"/tours/{tour['id']}", token=admin)
check("cancelling returns the place", after["data"]["availableSlots"] == after["data"]["maxCapacity"] - (active - 1), after["data"]["availableSlots"])

# --- removed vehicle counters
st, v = call("GET", "/transportations/search?size=5", token=owner)
check("vehicle responses no longer carry seat counters",
      st == 200 and all("remainingSeats" not in x and "calculatedAvailable" not in x for x in v["data"]["content"]), v)
print(f"\n{sum(ok)}/{len(ok)} access and logic checks passed")
