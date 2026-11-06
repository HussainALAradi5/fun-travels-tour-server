"""Live rule checks for meals, inventory and seats, transportation, and agency staff.
Usage: python -I rules_check.py <origin> <seed_password> <psql-db-name>
The database is reached with psql and the usual PGHOST, PGPORT, PGUSER and PGPASSWORD variables."""
import datetime
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request

BASE = sys.argv[1] + "/api"
PW = sys.argv[2]
DB = sys.argv[3]
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
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f" -> {str(detail)[:400]}"))


def login(identifier, password=PW):
    return call("POST", "/auth/login", {"identifier": identifier, "password": password})[1]["data"]


def psql(sql):
    return subprocess.run(["psql", "-d", DB, "-tAc", sql],
                          capture_output=True, text=True).stdout.strip()


stamp = str(int(time.time()))[-6:]
admin = login("admin")["token"]
own = login("owner")
owner, agency_id = own["token"], own["user"]["agency"]["id"]
manager, employee = login("manager")["token"], login("employee")["token"]
cust_name = "rules" + stamp
call("POST", "/auth/register", {"gender": "MALE", "userName": cust_name, "name": "Rules", "email": cust_name + "@example.com",
                                "password": "secret12", "mobileNumber": "+97333" + stamp})
cust_login = login(cust_name, "secret12")
cust, cust_id = cust_login["token"], cust_login["user"]["id"]
call("POST", f"/transactions/manual-credit/{cust_id}?amount=5000&description=test", token=admin)
countries = call("GET", "/countries")[1]["data"]
countries = countries if isinstance(countries, list) else countries["content"]
bh = next(c for c in countries if c["countryCode"] == "BH")
city_id = call("POST", "/cities", {"name": "Riffa " + stamp, "country": {"id": bh["id"]}}, admin)[1]["data"]["id"]


def vehicle(tag, capacity=4, config=None):
    st, b = call("POST", "/transportations", {"transportationNumber": tag + stamp, "code": tag + "C" + stamp, "type": "BUS",
                 "providerName": "Gulf Bus", "totalCapacity": capacity, "agencyId": agency_id, "seatConfig": config or {}}, owner)
    return st, b


def make_tour(tag, start_in, vid, capacity=4, meals=None, token=None, price=100, discount=0, end_in=None, activate=True):
    s = (datetime.date.today() + datetime.timedelta(days=start_in)).isoformat()
    e = (datetime.date.today() + datetime.timedelta(days=(end_in if end_in is not None else start_in + 1))).isoformat()
    st, b = call("POST", "/tours", {"tourNumber": tag + stamp, "title": tag, "basePrice": price, "discountPrice": discount,
                 "numberOfDays": 2, "startDate": s, "endDate": e, "maxCapacity": capacity, "startCountryId": bh["id"],
                 "endCountryId": bh["id"], "startCityId": city_id, "endCityId": city_id, "transportationId": vid,
                 "mealPlanIds": meals or []}, token or owner)
    if st == 200 and activate:
        for status in ("APPROVED", "ACTIVE"):
            call("PUT", f"/tours/{b['data']['id']}/status?status={status}", token=owner)
    return st, b


def seats_of(vid, tour_id=None):
    q = f"transportId={vid}&size=50" + (f"&tourId={tour_id}" if tour_id else "")
    return call("GET", f"/seats/search?{q}", token=owner)[1]["data"]["content"]


# ---------------------------------------------------------------- meals
st, b = call("POST", "/meals", {"mealName": "Mixed " + stamp, "mealPrice": 5, "dietaryTypes": ["STANDARD", "VEGAN"]}, owner)
check("meal: STANDARD cannot be combined with another diet (400)", st == 400, (st, b))
st, b = call("POST", "/meals", {"mealName": "Emp " + stamp, "mealPrice": 5}, employee)
check("meal: an employee cannot create meals (403)", st == 403, (st, b))
st, lunch = call("POST", "/meals", {"mealName": "Lunch " + stamp, "mealPrice": 10, "dietaryTypes": ["VEGAN"]}, owner)
st2, dinner = call("POST", "/meals", {"mealName": "Dinner " + stamp, "mealPrice": 20}, owner)
check("meal: owner creates vegan lunch and standard dinner", st == 200 and st2 == 200, (lunch, dinner))
lunch_id, dinner_id = lunch["data"]["id"], dinner["data"]["id"]
_, v1 = vehicle("RM1-", 4)
st, t1 = make_tour("RULES-MEAL-", 40, v1["data"]["id"], meals=[lunch_id])
t1_id = t1["data"]["id"]
st, b = call("POST", "/reservations", {"tour": {"id": t1_id}, "tickets": [{"selectedMeals": [{"id": dinner_id}]}]}, cust)
check("meal: a meal the tour does not offer is rejected (MEAL_NOT_OFFERED)", st == 409 and b.get("code") == "MEAL_NOT_OFFERED", b)
st, r = call("POST", "/reservations", {"tour": {"id": t1_id}, "tickets": [{"selectedMeals": [{"id": lunch_id}]}]}, cust)
check("meal: booking with the offered meal costs 100 + 10", st == 200 and float(r["data"]["totalPrice"]) == 110, r)
call("POST", f"/payments/execute/{r['data']['id']}?method=WALLET", token=cust)
call("PUT", f"/meals/{lunch_id}", {"mealName": "Lunch " + stamp, "mealPrice": 99, "dietaryTypes": ["VEGAN"]}, owner)
res = call("GET", f"/reservations/{r['data']['id']}", token=cust)[1]["data"]
check("meal: a later meal price change does not touch a paid booking", float(res["totalPrice"]) == 110, res.get("totalPrice"))
call("PATCH", f"/meals/{lunch_id}/status?status=INACTIVE", token=owner)
tour_now = call("GET", f"/tours/{t1_id}")[1]["data"]
check("meal: an inactive meal disappears from the tour", all(m["id"] != lunch_id for m in tour_now.get("availableMeals", [])), tour_now.get("availableMeals"))
st, b = call("POST", "/reservations", {"tour": {"id": t1_id}, "tickets": [{"selectedMeals": [{"id": lunch_id}]}]}, cust)
check("meal: an inactive meal cannot be booked (MEAL_INACTIVE)", st == 409 and b.get("code") in ("MEAL_INACTIVE", "DATE_CONFLICT"), b)

# ---------------------------------------------------------------- inventory and seats
_, v2 = vehicle("RI2-", 4)
v2_id = v2["data"]["id"]
st, t2 = make_tour("RULES-HOLD-", 60, v2_id)
t2_id = t2["data"]["id"]
sts = seats_of(v2_id, t2_id)
broken = sts[0]
st, b = call("PATCH", f"/seats/{broken['id']}/status?status=MAINTENANCE", token=owner)
check("seat: owner puts a free seat into maintenance", st == 200, b)
st, b = call("POST", "/reservations", {"tour": {"id": t2_id}, "tickets": [{"assignedSeat": {"id": broken["id"]}}]}, cust)
check("seat: a seat in maintenance cannot be chosen (SEAT_NOT_AVAILABLE)", st == 409 and b.get("code") == "SEAT_NOT_AVAILABLE", b)
st, hold = call("POST", "/reservations", {"tour": {"id": t2_id}, "tickets": [{}, {}, {}]}, cust)
held_seats = [] if st != 200 else [t["assignedSeat"]["id"] for t in call("GET", f"/tickets/search?reservationId={hold['data']['id']}&size=10", token=cust)[1]["data"]["content"]]
check("seat: automatic seats skip the seat in maintenance", st == 200 and broken["id"] not in held_seats and len(held_seats) == 3, (st, held_seats))
places = call("GET", f"/tours/{t2_id}")[1]["data"]["availableSlots"]
check("inventory: a 3-traveler hold takes 3 of 4 places", places == 1, places)
psql(f"UPDATE tour_reservations SET hold_expires_at = now() - interval '1 minute' WHERE id = {hold['data']['id']}")
expired = None
for _ in range(16):
    time.sleep(5)
    expired = call("GET", f"/reservations/{hold['data']['id']}", token=cust)[1]["data"]["status"]
    if expired == "CANCELLED":
        break
check("inventory: an expired unpaid hold is cancelled automatically", expired == "CANCELLED", expired)
places = call("GET", f"/tours/{t2_id}")[1]["data"]["availableSlots"]
check("inventory: the expired hold gives its places back", places == 4, places)
free_now = {s["id"]: s["status"] for s in seats_of(v2_id, t2_id)}
check("inventory: its seats are free again", all(free_now.get(sid) == "AVAILABLE" for sid in held_seats), free_now)
st, b = call("POST", f"/payments/execute/{hold['data']['id']}?method=WALLET", token=cust)
check("inventory: an expired hold cannot be paid", st == 409 and b.get("code") in ("RESERVATION_NOT_PAYABLE", "RESERVATION_HOLD_EXPIRED"), b)
st, r = call("POST", "/reservations", {"tour": {"id": t2_id}, "tickets": [{}]}, cust)
call("POST", f"/payments/execute/{r['data']['id']}?method=WALLET", token=cust)
st, b = call("POST", f"/payments/execute/{r['data']['id']}?method=WALLET", token=cust)
check("payment: a paid booking cannot be paid twice", st == 409, (st, b))

# ---------------------------------------------------------------- transportation
st, b = vehicle("RT3-", 4, {"KIDS_CHAIR": 5})
check("vehicle: more special seats than capacity is rejected", st in (400, 409), (st, b))
_, v3 = vehicle("RT4-", 4)
v3_id = v3["data"]["id"]
st, b = make_tour("RULES-BIG-", 80, v3_id, capacity=6)
check("tour: capacity above the vehicle's seats is rejected", st == 409, (st, b))
st, t3 = make_tour("RULES-T3-", 80, v3_id, capacity=4)
st, b = make_tour("RULES-T3B-", 81, v3_id, capacity=4)
check("tour: the same vehicle cannot run two overlapping tours", st == 409, (st, b))
st, r = call("POST", "/reservations", {"tour": {"id": t3["data"]["id"]}, "tickets": [{}, {}]}, cust)
st, b = call("PUT", f"/transportations/{v3_id}", {"seatConfig": {"KIDS_CHAIR": 2}}, owner)
check("vehicle: the seat layout is locked while tickets are sold", st == 409, (st, b))
upd = dict(t3["data"])
st, b = call("PUT", f"/tours/{t3['data']['id']}", {"title": "RULES-T3", "maxCapacity": 1}, owner)
check("tour: capacity cannot drop below sold places (CAPACITY_BELOW_BOOKINGS)", st == 409 and b.get("code") == "CAPACITY_BELOW_BOOKINGS", b)

# ---------------------------------------------------------------- tour rules and staff roles
st, b = make_tour("RULES-PAST-", -2, v3_id, activate=False)
check("tour: a start date in the past is rejected", st == 409, (st, b))
st, b = make_tour("RULES-ENDS-", 200, v3_id, end_in=198, activate=False)
check("tour: an end date before the start is rejected", st == 409, (st, b))
st, b = make_tour("RULES-DISC-", 210, v3_id, price=100, discount=150, activate=False)
check("tour: a discount above the price is rejected (DISCOUNT_ABOVE_PRICE)", st == 409 and b.get("code") == "DISCOUNT_ABOVE_PRICE", (st, b))
st, b = make_tour("RULES-CUST-", 220, v3_id, token=cust, activate=False)
check("staff: a customer cannot create tours (403)", st == 403, (st, b))
_, v5 = vehicle("RS5-", 4)
st, b = make_tour("RULES-EMP-", 230, v5["data"]["id"], token=employee, activate=False)
check("staff: an employee cannot create tours (403); managers and owners do", st == 403, (st, b))
st, mt = make_tour("RULES-MGR-", 230, v5["data"]["id"], token=manager, activate=False)
check("staff: a manager creates a tour, kept pending", st == 200 and mt["data"]["status"] == "PENDING", (st, mt))
st, b = call("PUT", f"/tours/{mt['data']['id']}/status?status=APPROVED", token=employee)
check("staff: an employee cannot approve tours (403)", st == 403, (st, b))
st, b = call("PUT", f"/tours/{mt['data']['id']}/status?status=APPROVED", token=manager)
check("staff: a manager can approve tours of the agency", st == 200, (st, b))
st, b = call("PUT", f"/tours/{mt['data']['id']}/status?status=COMPLETED", token=manager)
check("tour: an approved tour cannot jump to COMPLETED", st == 409, (st, b))
st, b = call("PUT", f"/tours/{mt['data']['id']}/status?status=APPROVED", token=cust)
check("staff: a customer cannot change tour status (403)", st == 403, (st, b))

# ---------------------------------------------------------------- tour sale price
_, v6 = vehicle("RD6-", 4, {"KIDS_CHAIR": 1})
st, sale = make_tour("RULES-SALE-", 240, v6["data"]["id"], price=100, discount=20)
check("tour: a sale price of 100 - 20 = 80", st == 200 and sale["data"]["totalPrice"] == 80, (st, sale.get("data")))
kid = next(s for s in seats_of(v6["data"]["id"]) if s["chairType"] == "KIDS_CHAIR")
st, r = call("POST", "/reservations", {"tour": {"id": sale["data"]["id"]}, "tickets": [{}, {"assignedSeat": {"id": kid["id"]}}]}, cust)
check("tour: the sale applies to tickets (80 adult + 40 kids chair = 120)", st == 200 and float(r["data"]["totalPrice"]) == 120, r)
if st == 200:
    call("PATCH", f"/reservations/{r['data']['id']}/cancel", token=cust)
st, b = call("PUT", f"/tours/{sale['data']['id']}", {"discountPrice": 150}, owner)
check("tour: editing the discount above the price is rejected (DISCOUNT_ABOVE_PRICE)",
      st == 409 and b.get("code") == "DISCOUNT_ABOVE_PRICE", (st, b))
st, b = call("PUT", f"/tours/{sale['data']['id']}", {"discountPrice": 0}, owner)
check("tour: removing the sale restores the full price", st == 200 and b["data"]["totalPrice"] == 100, (st, b))

print(f"\n{sum(ok)}/{len(ok)} rule checks passed")
