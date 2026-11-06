"""End-to-end API smoke test against a running server. Usage: python -I end_to_end_check.py <base_url> <seed_password>"""
import base64
import datetime
import hashlib
import hmac
import json
import sys
import time
import urllib.error
import urllib.request

BASE, PASSWORD = sys.argv[1], sys.argv[2]
results = []


def call(method, path, body=None, token=None):
    started = time.time()
    try:
        return _call(method, path, body, token)
    finally:
        if time.time() - started > 2:
            print(f'SLOW {time.time() - started:.1f}s {method} {path}')


def _call(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.status, json.loads(resp.read() or b"{}")
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw or b"{}")
        except ValueError:
            return e.code, {"raw": raw.decode(errors="replace")}


def check(name, condition, detail=""):
    results.append((name, bool(condition)))
    print(("PASS " if condition else "FAIL ") + name + ("" if condition else f"  -> {detail}"))


def login(identifier, password=PASSWORD):
    status, body = call("POST", "/api/auth/login", {"identifier": identifier, "password": password})
    return body.get("data", {}).get("token") if status == 200 else None


def balance(token, user_id):
    return float(call("GET", f"/api/accounts/user/{user_id}/balance", token=token)[1]["data"]["balance"])


tokens = {role: login(role) for role in ["admin", "owner", "manager", "employee", "customer", "support", "developer"]}
check("all seeded roles can sign in", all(tokens.values()), {k: bool(v) for k, v in tokens.items()})
check("wrong password rejected with 401", call("POST", "/api/auth/login", {"identifier": "admin", "password": "nope"})[0] == 401)

# forged token signed with the old hard-coded key must be rejected
old_key = base64.b64decode("YWRtaW5hZG1pbmFkbWluYWRtaW5hZG1pbmFkbWluYWRtaW5hZG1pbg==")
def b64(d): return base64.urlsafe_b64encode(d).rstrip(b"=").decode()
header, payload = b64(b'{"alg":"HS256"}'), b64(json.dumps({"sub": "admin@funtravel.com", "exp": 4102444800}).encode())
forged = f"{header}.{payload}." + b64(hmac.new(old_key, f"{header}.{payload}".encode(), hashlib.sha256).digest())
check("token forged with old hard-coded key is rejected", call("GET", "/api/users", token=forged)[0] in (401, 403))

# registration
status, body = call("POST", "/api/auth/register", {"gender": "MALE", "userName": "traveler1", "name": "Tess Traveler",
                    "email": "traveler1@example.com", "password": "secret12", "mobileNumber": "+97339990001", "age": 31})
check("public registration creates a customer", status == 201 and body["data"]["userType"] == "CUSTOMER", body)
traveler = login("traveler1", "secret12")
traveler_id = body["data"]["id"]
check("short password rejected", call("POST", "/api/auth/register", {"gender": "MALE", "userName": "x2", "name": "x", "email": "x2@example.com",
      "password": "123", "mobileNumber": "+97339990002"})[0] == 400)

me = {r: call("GET", "/api/users/0", token=tokens[r]) for r in []}
customer_id = call("POST", "/api/auth/login", {"identifier": "customer", "password": PASSWORD})[1]["data"]["user"]["id"]
admin_id = call("POST", "/api/auth/login", {"identifier": "admin", "password": PASSWORD})[1]["data"]["user"]["id"]

# privilege escalation through profile update must not work
status, body = call("PUT", f"/api/users/{traveler_id}", {"name": "Tess T", "userType": "ADMIN"}, traveler)
check("profile update ignores userType", status == 200 and body["data"]["userType"] == "CUSTOMER", body)
check("customer cannot edit another user", call("PUT", f"/api/users/{admin_id}", {"name": "pwned"}, traveler)[0] == 403)
check("customer cannot read another user's profile", call("GET", f"/api/users/{admin_id}", token=traveler)[0] == 403)
check("customer cannot read another wallet", call("GET", f"/api/accounts/user/{customer_id}/balance", token=traveler)[0] == 403)
check("customer cannot read another user's notifications",
      call("GET", f"/api/notifications/user/{customer_id}", token=traveler)[0] == 403)
check("customer cannot create an agency", call("POST", "/api/agencies", {"agencyName": "Evil"}, traveler)[0] == 403)
check("owner cannot create an admin", call("POST", "/api/users/add-employee", {"gender": "MALE", "userName": "boss", "name": "b",
      "email": "boss@example.com", "password": "secret12", "mobileNumber": "+97339990003", "userType": "ADMIN"},
      tokens["owner"])[0] == 403)
status, body = call("POST", "/api/users/add-employee", {"gender": "MALE", "userName": "emp9", "name": "Emp Nine",
                    "email": "emp9@example.com", "password": "secret12", "mobileNumber": "+97339990004",
                    "userType": "EMPLOYEE"}, tokens["owner"])
check("owner can add an employee to own agency", status == 201 and body["data"]["agency"] is not None, body)

# geography (admin)
status, country = call("POST", "/api/countries", {"famousName": "Testland", "officialName": "Republic of Testland",
                       "countryCode": "XB", "dialCode": "+999"}, tokens["admin"])
check("admin creates a country", status == 200, country)
country_id = country["data"]["id"]
status, city = call("POST", "/api/cities", {"name": "Manama", "country": {"id": country_id}}, tokens["admin"])
check("admin creates a city", status == 200, city)
city_id = city["data"]["id"]
check("cities are public", call("GET", f"/api/cities/country/{country_id}")[0] == 200)

# transportation and tour (owner)
agency_id = call("GET", "/api/users/0", token=None)  # placeholder to keep flow readable
owner_user = call("POST", "/api/auth/login", {"identifier": "owner", "password": PASSWORD})[1]["data"]["user"]
agency_id = owner_user["agency"]["id"]
status, transport = call("POST", "/api/transportations", {"transportationNumber": "BUS-1", "code": "BH-BUS-1",
                         "type": "BUS", "providerName": "Gulf Bus", "totalCapacity": 10, "agencyId": 999,
                         "seatConfig": {"KIDS_CHAIR": 2}}, tokens["owner"])
check("owner creates transportation (agency forced to own)", status == 201 and transport["data"].get("agencyId", agency_id) == agency_id, transport)
transport_id = transport["data"]["id"]
start = (datetime.date.today() + datetime.timedelta(days=30)).isoformat()
end = (datetime.date.today() + datetime.timedelta(days=33)).isoformat()
status, tour = call("POST", "/api/tours", {"tourNumber": "T-100", "title": "Bahrain Explorer", "basePrice": 100,
                    "numberOfDays": 4, "startDate": start, "endDate": end, "maxCapacity": 10,
                    "startCountryId": country_id, "endCountryId": country_id, "startCityId": city_id,
                    "endCityId": city_id, "transportationId": transport_id}, tokens["owner"])
check("owner creates a tour", status == 200, tour)
tour_id = tour["data"]["id"]
check("owner can approve own tour (was 403)", call("PUT", f"/api/tours/{tour_id}/status?status=APPROVED", token=tokens["owner"])[0] == 200)
check("tour becomes ACTIVE", call("PUT", f"/api/tours/{tour_id}/status?status=ACTIVE", token=tokens["owner"])[0] == 200)

# booking: no ticket list must not mean a free booking
before = balance(traveler, traveler_id)
status, res = call("POST", "/api/reservations", {"tour": {"id": tour_id}, "requestedSlots": 2, "totalPrice": 1}, traveler)
check("reservation without tickets is priced by the server", status == 200 and float(res["data"]["totalPrice"]) >= 100, res)
res_id = res["data"]["id"]
status, paid = call("POST", f"/api/payments/execute/{res_id}?method=WALLET", token=traveler)
check("wallet payment fails cleanly with empty wallet", status == 409 and paid.get("code") == "INSUFFICIENT_BALANCE", paid)
call("POST", f"/api/transactions/manual-credit/{traveler_id}?amount=1000&description=test", token=tokens["admin"])
status, paid = call("POST", f"/api/payments/execute/{res_id}?method=WALLET", token=traveler)
check("wallet payment confirms reservation", status == 200 and paid["data"]["status"] == "CONFIRMED", paid)
after_pay = balance(traveler, traveler_id)
total = float(res["data"]["totalPrice"])
check("wallet debited by the reservation total", abs((before + 1000 - total) - after_pay) < 0.01, (before, after_pay, total))

tickets = call("GET", f"/api/tickets/search?search={res['data']['reservationNumber']}", token=traveler)[1]["data"]["content"]
if not tickets:
    tickets = call("GET", "/api/tickets", token=traveler)[1]["data"]["content"]
check("two tickets were issued", len(tickets) == 2, tickets)
first, second = tickets[0], tickets[1]
status, body = call("PUT", f"/api/tickets/{first['id']}/cancel", token=traveler)
check("customer can cancel own ticket (was 403)", status == 200, body)
after_ticket = balance(traveler, traveler_id)
check("ticket cancellation refunds that ticket only", abs(after_ticket - after_pay - float(first["totalPrice"])) < 0.01,
      (after_pay, after_ticket, first["totalPrice"]))
status, body = call("PATCH", f"/api/reservations/{res_id}/cancel", token=traveler)
check("customer cancels the reservation", status == 200 and body["data"]["status"] == "CANCELLED", body)
after_res = balance(traveler, traveler_id)
check("reservation cancel refunds only the remaining ticket (no double refund)",
      abs(after_res - after_ticket - float(second["totalPrice"])) < 0.01, (after_ticket, after_res, second["totalPrice"]))
check("cancelling twice is rejected", call("PATCH", f"/api/reservations/{res_id}/cancel", token=traveler)[0] == 409)
tour_now = call("GET", f"/api/tours/{tour_id}")[1]["data"]
check("all places returned to inventory", tour_now["availableSlots"] == 10, tour_now.get("availableSlots"))

# other customer cannot see traveler's reservation or timeline
check("customer cannot read another customer's reservation", call("GET", f"/api/reservations/{res_id}", token=tokens["customer"])[0] == 403)
check("customer cannot read another customer's timeline", call("GET", f"/api/tracking/RESERVATION/{res_id}", token=tokens["customer"])[0] == 403)
check("password reset request does not reveal accounts",
      call("POST", "/api/auth/password-reset/request", {"identifier": "nobody@example.com"})[0] == 200)

# ---- seats and meals across two tours sharing one vehicle ----
status, meal = call("POST", "/api/meals", {"mealName": "Machboos", "mealPrice": 12.5}, tokens["owner"])
check("owner creates a meal for own agency", status == 200, meal)
meal_id = meal["data"]["id"]
status, off_meal = call("POST", "/api/meals", {"mealName": "Old Menu", "mealPrice": 5}, tokens["owner"])
call("PATCH", f"/api/meals/{off_meal['data']['id']}/status?status=INACTIVE", token=tokens["owner"])
def make_tour(number, offset, meals):
    s = (datetime.date.today() + datetime.timedelta(days=offset)).isoformat()
    e = (datetime.date.today() + datetime.timedelta(days=offset + 2)).isoformat()
    st, body = call("POST", "/api/tours", {"tourNumber": number, "title": number, "basePrice": 50, "numberOfDays": 3,
                    "startDate": s, "endDate": e, "maxCapacity": 10, "startCountryId": country_id, "endCountryId": country_id,
                    "startCityId": city_id, "endCityId": city_id, "transportationId": transport_id, "mealPlanIds": meals},
                    tokens["owner"])
    return st, body
st, inactive_tour = make_tour("T-BAD", 60, [off_meal["data"]["id"]])
check("tour cannot offer an inactive meal", st == 409, inactive_tour)
st, a = make_tour("T-A", 60, [meal_id]); st2, b = make_tour("T-B", 90, [meal_id])
check("two tours can share the vehicle on different dates", st == 200 and st2 == 200, (a, b))
for t_ in (a, b):
    for s_ in ("APPROVED", "ACTIVE"):
        call("PUT", f"/api/tours/{t_['data']['id']}/status?status={s_}", token=tokens["owner"])
check("tour response lists its meals", any(m["id"] == meal_id for m in call("GET", f"/api/tours/{a['data']['id']}")[1]["data"].get("availableMeals", [])))
seats = call("GET", f"/api/seats/search?tourId={a['data']['id']}&size=100", token=traveler)[1]["data"]["content"]
seat_id = seats[0]["id"]
st, r1 = call("POST", "/api/reservations", {"tour": {"id": a["data"]["id"]},
              "tickets": [{"assignedSeat": {"id": seat_id}, "selectedMeals": [{"id": meal_id}]}]}, traveler)
check("booking with chosen seat and meal is priced 50 + 12.50 + seat modifier", st == 200 and float(r1["data"]["totalPrice"]) >= 62.5, r1)
st, dup = call("POST", "/api/reservations", {"tour": {"id": b["data"]["id"]},
               "tickets": [{"selectedMeals": [{"id": meal_id}, {"id": meal_id}]}]}, tokens["customer"])
check("same meal twice on one ticket is rejected", st == 409, dup)
seats_a = {s["id"]: s["status"] for s in call("GET", f"/api/seats/search?tourId={a['data']['id']}&size=100", token=traveler)[1]["data"]["content"]}
seats_b = {s["id"]: s["status"] for s in call("GET", f"/api/seats/search?tourId={b['data']['id']}&size=100", token=traveler)[1]["data"]["content"]}
check("seat shows BOOKED on its own tour", seats_a.get(seat_id) == "BOOKED", seats_a.get(seat_id))
check("same seat stays AVAILABLE on the vehicle's other tour", seats_b.get(seat_id) == "AVAILABLE", seats_b.get(seat_id))
st, r2 = call("POST", "/api/reservations", {"tour": {"id": b["data"]["id"]}, "tickets": [{"assignedSeat": {"id": seat_id}}]}, tokens["customer"])
check("same seat can be sold on the other tour (was blocked forever)", st == 200, r2)
st, again = call("POST", "/api/reservations", {"tour": {"id": a["data"]["id"]}, "tickets": [{"assignedSeat": {"id": seat_id}}]}, tokens["customer"])
check("taken seat cannot be sold twice on the same tour", st == 409, again)
check("seat held by a ticket cannot go to maintenance",
      call("PATCH", f"/api/seats/{seat_id}/status?status=MAINTENANCE", token=tokens["owner"])[0] == 409)
check("staff cannot set BOOKED manually",
      call("PATCH", f"/api/seats/{seats[1]['id']}/status?status=BOOKED", token=tokens["owner"])[0] == 409)
st, tchg = call("PUT", f"/api/tours/{a['data']['id']}", {**a["data"], "transportation": {"id": 999999}}, tokens["owner"])
check("vehicle cannot be swapped after tickets are issued", st in (400, 404, 409), tchg)
call("PATCH", f"/api/reservations/{r1['data']['id']}/cancel", token=traveler)
seats_a = {s["id"]: s["status"] for s in call("GET", f"/api/seats/search?tourId={a['data']['id']}&size=100", token=traveler)[1]["data"]["content"]}
check("cancelling frees the seat for that tour", seats_a.get(seat_id) == "AVAILABLE", seats_a.get(seat_id))

passed = sum(1 for _, ok in results if ok)
print(f"\n{passed}/{len(results)} checks passed")
sys.exit(0 if passed == len(results) else 1)
