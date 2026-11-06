"""Business-rule checks against a running server. Usage: python -I business_check.py <origin> <seed_password>"""
import datetime
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
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f" -> {str(detail)[:400]}"))


def token(identifier, password=PW):
    return call("POST", "/auth/login", {"identifier": identifier, "password": password})[1]["data"]["token"]


def money(v):
    return round(float(v), 2)


admin, owner = token("admin"), token("owner")
owner_agency = call("POST", "/auth/login", {"identifier": "owner", "password": PW})[1]["data"]["user"]["agency"]["id"]
stamp = str(int(time.time()))[-6:]


def new_customer(tag):
    name = f"{tag}{stamp}"
    st, b = call("POST", "/auth/register", {"gender": "MALE", "userName": name, "name": tag.title(), "email": f"{name}@example.com",
                                            "password": "secret12", "mobileNumber": "+9733" + str("abc".index(tag[0]) + 4) + stamp})
    uid = b["data"]["id"]
    call("POST", f"/transactions/manual-credit/{uid}?amount=5000&description=test", token=admin)
    return token(name, "secret12"), uid


def balance(tok, uid):
    return money(call("GET", f"/accounts/user/{uid}/balance", token=tok)[1]["data"]["balance"])


countries = call("GET", "/countries")[1]["data"]
countries = countries if isinstance(countries, list) else countries["content"]
bh = next(c for c in countries if c["countryCode"] == "BH")
st, city = call("POST", "/cities", {"name": "Muharraq " + stamp, "country": {"id": bh["id"]}}, admin)
city_id = city["data"]["id"]

# vehicle: 6 seats, 2 kids chairs (created first), 1 premium
st, veh = call("POST", "/transportations", {"transportationNumber": "BUS-" + stamp, "code": "BIZ-" + stamp, "type": "BUS",
                "providerName": "Gulf Bus", "totalCapacity": 6, "agencyId": owner_agency, "seatConfig": {"KIDS_CHAIR": 2, "PREMIUM_RECLINER": 1}}, owner)
check("vehicle with 6 seats (2 kids, 1 premium) is created", st == 201, veh)
veh_id = veh["data"]["id"]
seats = call("GET", f"/seats/search?transportId={veh_id}&size=50", token=owner)[1]["data"]["content"]
by_type = {}
for s in seats:
    by_type.setdefault(s["chairType"], []).append(s)
check("seat layout matches the configuration", len(seats) == 6 and len(by_type.get("KIDS_CHAIR", [])) == 2
      and len(by_type.get("PREMIUM_RECLINER", [])) == 1 and len(by_type.get("STANDARD", [])) == 3, by_type.keys())
premium = by_type["PREMIUM_RECLINER"][0]
st, _ = call("PATCH", f"/seats/{premium['id']}", {"seatPriceModifier": 20}, owner)
check("owner sets a +20 premium seat price", st == 200, _)

st, meal = call("POST", "/meals", {"mealName": "Biz Lunch " + stamp, "mealPrice": 12.5}, owner)
meal_id = meal["data"]["id"]


def tour(number, start_in, days=3, capacity=6, price=100, activate=True, vehicle=None):
    s = (datetime.date.today() + datetime.timedelta(days=start_in)).isoformat()
    e = (datetime.date.today() + datetime.timedelta(days=start_in + days - 1)).isoformat()
    st, b = call("POST", "/tours", {"tourNumber": number + stamp, "title": number, "basePrice": price, "numberOfDays": days,
                                    "startDate": s, "endDate": e, "maxCapacity": capacity, "startCountryId": bh["id"],
                                    "endCountryId": bh["id"], "startCityId": city_id, "endCityId": city_id,
                                    "transportationId": vehicle or veh_id, "mealPlanIds": [meal_id]}, owner)
    if st != 200:
        return st, b
    if activate:
        for status in ("APPROVED", "ACTIVE"):
            call("PUT", f"/tours/{b['data']['id']}/status?status={status}", token=owner)
    return st, b


def book(tok, tour_id, tickets):
    return call("POST", "/reservations", {"tour": {"id": tour_id}, "tickets": tickets}, tok)


def pay(tok, res_id):
    return call("POST", f"/payments/execute/{res_id}?method=WALLET", token=tok)


def tickets_of(tok, res_id):
    b = call("GET", f"/tickets/search?reservationId={res_id}&size=50", token=tok)[1]["data"]
    return b if isinstance(b, list) else b.get("content", [])


alice, alice_id = new_customer("alice")
bob, bob_id = new_customer("bob")
carol, carol_id = new_customer("carol")

# ---------------------------------------------------------------- pricing
st, t1 = tour("BIZ-PRICE-", 40)
t1_id = t1["data"]["id"]
kid = by_type["KIDS_CHAIR"][0]
std = by_type["STANDARD"][0]
st, r = book(alice, t1_id, [
    {"assignedSeat": {"id": std["id"]}},                                          # adult, standard: 100
    {"assignedSeat": {"id": kid["id"]}},                                          # child: 100 - 50% = 50
    {"assignedSeat": {"id": premium["id"]}, "selectedMeals": [{"id": meal_id}]},  # premium + meal: 100 + 20 + 12.50
])
check("mixed booking of 3 tickets is accepted", st == 200, r)
res1 = r["data"]
check("booking total = 100 + 50 + 132.50 = 282.50", money(res1["totalPrice"]) == 282.50, res1.get("totalPrice"))
tk = {t["assignedSeat"]["id"] if t.get("assignedSeat") else None: t for t in tickets_of(alice, res1["id"])}
check("kids chair ticket: base 100, discount 50, total 50",
      tk.get(kid["id"]) and money(tk[kid["id"]]["basePrice"]) == 100 and money(tk[kid["id"]]["discountPrice"]) == 50
      and money(tk[kid["id"]]["totalPrice"]) == 50, tk.get(kid["id"]))
check("standard ticket costs the full 100", tk.get(std["id"]) and money(tk[std["id"]]["totalPrice"]) == 100, tk.get(std["id"]))
check("premium seat + meal ticket costs 132.50", tk.get(premium["id"]) and money(tk[premium["id"]]["totalPrice"]) == 132.50,
      tk.get(premium["id"]))
check("the client cannot set a price", book(carol, t1_id, [{"totalPrice": 1, "basePrice": 1}])[1].get("data", {}).get("totalPrice") not in (1, "1", 1.0), "")
# clean up carol's probe so it does not hold a place
for res in (call("GET", "/reservations?size=50", token=carol)[1]["data"].get("content", [])):
    call("PATCH", f"/reservations/{res['id']}/cancel", token=carol)

before = balance(alice, alice_id)
st, p = pay(alice, res1["id"])
check("paying debits exactly the total", st == 200 and round(before - balance(alice, alice_id), 2) == 282.50, (st, p))
check("all 3 tickets are paid", all(t.get("paid") for t in tickets_of(alice, res1["id"])), tickets_of(alice, res1["id"]))

# ---------------------------------------------------------------- automatic seats
st, t2 = tour("BIZ-AUTO-", 70)
t2_id = t2["data"]["id"]
st, r = book(bob, t2_id, [{}, {}])
auto = tickets_of(bob, r["data"]["id"]) if st == 200 else []
check("two travelers without a chosen seat get two different seats",
      st == 200 and len({t["assignedSeat"]["id"] for t in auto}) == 2, (st, r))
check("automatic seats are standard seats (no accidental kids discount)",
      auto and all(t["assignedSeat"]["chairType"] == "STANDARD" for t in auto), [t["assignedSeat"] for t in auto])
# when only special seats are left, travelers must choose
st, t4 = tour("BIZ-SPECIAL-", 120, days=1, capacity=6)
if st == 200:
    book(carol, t4["data"]["id"], [{}, {}, {}])
    st_, b_ = book(bob, t4["data"]["id"], [{}])
    check("no standard seats left: automatic booking asks for a seat choice (SEAT_CHOICE_REQUIRED)",
          st_ == 409 and b_.get("code") == "SEAT_CHOICE_REQUIRED", b_)
check("automatic booking of 2 costs 200", st == 200 and money(r["data"]["totalPrice"]) == 200, r.get("data", {}).get("totalPrice"))
if st == 200:
    call("PATCH", f"/reservations/{r['data']['id']}/cancel", token=bob)

# ---------------------------------------------------------------- seats and capacity
st, b = book(bob, t1_id, [{"assignedSeat": {"id": by_type["STANDARD"][1]["id"]}}, {"assignedSeat": {"id": by_type["STANDARD"][1]["id"]}}])
check("the same seat twice in one booking is rejected (DUPLICATE_SEAT)", st == 409 and b.get("code") == "DUPLICATE_SEAT", b)
st, b = book(bob, t1_id, [{"assignedSeat": {"id": kid["id"]}}])
check("a seat sold to someone else is rejected (SEAT_NOT_AVAILABLE)", st == 409 and b.get("code") == "SEAT_NOT_AVAILABLE", b)
st, b = book(bob, t1_id, [{}, {}, {}, {}])
check("more travelers than places left is rejected (INSUFFICIENT_CAPACITY)", st == 409 and b.get("code") == "INSUFFICIENT_CAPACITY", b)
st, tour1 = call("GET", f"/tours/{t1_id}")
check("places left = 6 - 3 sold", st == 200 and tour1["data"].get("availableSlots") == 3,
      {k: v for k, v in tour1.get("data", {}).items() if "vail" in k})

# ---------------------------------------------------------------- holds and dates
st, r = book(bob, t1_id, [{}])
hold = r.get("data", {})
expires = hold.get("holdExpiresAt")
created = hold.get("bookingDate") or hold.get("createdAt")
check("an unpaid booking is a hold that expires (holdExpiresAt set, status PENDING)",
      st == 200 and expires and hold.get("status") == "PENDING", hold)
st, veh2 = call("POST", "/transportations", {"transportationNumber": "VAN-" + stamp, "code": "BIZV-" + stamp, "type": "BUS",
                 "providerName": "Gulf Bus", "totalCapacity": 6, "agencyId": owner_agency}, owner)
st, t3 = tour("BIZ-OVERLAP-", 41, days=2, vehicle=veh2["data"]["id"])
st, b = book(bob, t3["data"]["id"], [{}])
check("a second booking on overlapping dates is rejected (DATE_CONFLICT)", st == 409 and b.get("code") == "DATE_CONFLICT", b)
call("PATCH", f"/reservations/{hold.get('id')}/cancel", token=bob)

st, pending = tour("BIZ-PENDING-", 100, activate=False)
st, b = book(bob, pending["data"]["id"], [{}])
check("a tour that is not active cannot be booked", st in (404, 409), (st, b))

# ---------------------------------------------------------------- refunds by notice
def spare_vehicle(tag):
    return call("POST", "/transportations", {"transportationNumber": tag + stamp, "code": tag + "C" + stamp, "type": "BUS",
                "providerName": "Gulf Bus", "totalCapacity": 6, "agencyId": owner_agency}, owner)[1]["data"]["id"]


def refund_case(name, start_in, expected_ratio, who, who_id):
    st, t = tour(name, start_in, days=1, vehicle=spare_vehicle(name))
    if st != 200:
        check(f"{name} tour created", False, t)
        return
    st, r = book(who, t["data"]["id"], [{}])
    pay(who, r["data"]["id"])
    q = call("GET", f"/reservations/{r['data']['id']}/refund-quote", token=who)[1].get("data") or {}
    other = call("GET", f"/reservations/{r['data']['id']}/refund-quote", token=alice if who != alice else bob)[0]
    before = balance(who, who_id)
    st, c = call("PATCH", f"/reservations/{r['data']['id']}/cancel", token=who)
    got = round(balance(who, who_id) - before, 2)
    check(f"refund preview {start_in} days ahead says {q.get('refund')} ({q.get('percent')}%) and matches the refund",
          q.get("refund") is not None and round(float(q["refund"]), 2) == got and q.get("percent") == int(expected_ratio * 100), (q, got))
    check(f"another customer cannot read that refund preview (403)", other == 403, other)
    check(f"cancel {start_in} days ahead refunds {int(expected_ratio * 100)}% ({got} of 100)", st == 200 and got == 100 * expected_ratio, (st, c, got))


refund_case("BIZ-R30-", 30, 1.0, carol, carol_id)
refund_case("BIZ-R10-", 10, 0.5, carol, carol_id)
refund_case("BIZ-R3-", 3, 0.0, carol, carol_id)

# one ticket of the paid mixed booking: refund that ticket only, free its seat
kid_ticket = tk[kid["id"]]
before = balance(alice, alice_id)
st, c = call("PUT", f"/tickets/{kid_ticket['id']}/cancel", token=alice)
check("cancelling the kids ticket refunds its 50 only", st == 200 and round(balance(alice, alice_id) - before, 2) == 50,
      (st, round(balance(alice, alice_id) - before, 2)))
seat_now = {s["id"]: s["status"] for s in call("GET", f"/seats/search?tourId={t1_id}&size=50", token=owner)[1]["data"]["content"]}
check("its kids chair is free again on that tour", seat_now.get(kid["id"]) == "AVAILABLE", seat_now.get(kid["id"]))

# ---------------------------------------------------------------- agency tour cancellation rules
st, soon = tour("BIZ-SOON-", 5, days=1, vehicle=spare_vehicle("BIZ-SOON-"))
st, b = call("PUT", f"/tours/{soon['data']['id']}/status?status=CANCELLED", token=owner)
check("agency cannot cancel a tour starting within 14 days", st == 409, (st, b))
st, full = tour("BIZ-FULL-", 50, days=1, capacity=6, vehicle=spare_vehicle("BIZ-FULL-"))
book(bob, full["data"]["id"], [{}, {}, {}])
st, b = call("PUT", f"/tours/{full['data']['id']}/status?status=CANCELLED", token=owner)
check("agency cannot cancel a tour more than a third booked", st == 409, (st, b))
st, light = tour("BIZ-LIGHT-", 60, days=1, capacity=6, vehicle=spare_vehicle("BIZ-LIGHT-"))
st, r = book(carol, light["data"]["id"], [{}])
pay(carol, r["data"]["id"])
before = balance(carol, carol_id)
st, b = call("PUT", f"/tours/{light['data']['id']}/status?status=CANCELLED", token=owner)
check("agency cancels a lightly booked tour and the customer gets a full refund",
      st == 200 and round(balance(carol, carol_id) - before, 2) == 100, (st, b, round(balance(carol, carol_id) - before, 2)))
st, b = call("PUT", f"/tours/{light['data']['id']}/status?status=ACTIVE", token=owner)
check("a cancelled tour cannot go straight back to ACTIVE", st == 409, (st, b))
st, b = call("PUT", f"/tours/{t1_id}/status?status=PENDING", token=owner)
check("an active tour cannot go back to PENDING", st == 409, (st, b))

# ---------------------------------------------------------------- agency policy (own discount and refund rules)
manager = token("manager")
pol_url = f"/agencies/{owner_agency}/policy"
st, pol = call("GET", pol_url)
check("anyone can read an agency's rules (defaults 50% kids, 14/7 days, 50%)",
      st == 200 and (pol["data"]["kidsDiscountPercent"], pol["data"]["fullRefundDays"], pol["data"]["partialRefundDays"],
                     pol["data"]["partialRefundPercent"]) == (50, 14, 7, 50), pol)
custom = {"kidsDiscountPercent": 30, "fullRefundDays": 30, "partialRefundDays": 10, "partialRefundPercent": 25}
check("a customer cannot change the rules", call("PUT", pol_url, custom, alice)[0] == 403)
check("a manager cannot change the rules", call("PUT", pol_url, custom, manager)[0] == 403)
st, b = call("PUT", pol_url, dict(custom, partialRefundDays=40), owner)
check("partial window after the full window is rejected (400)", st == 400, (st, b))
st, b = call("PUT", pol_url, dict(custom, kidsDiscountPercent=120), owner)
check("a discount over 100% is rejected (400)", st == 400 and "kidsDiscountPercent" in b.get("fieldErrors", {}), (st, b))
st, b = call("PUT", pol_url, custom, owner)
check("the owner saves custom rules (kids 30%, full >30 days, 25% from 10 days)",
      st == 200 and b["data"]["kidsDiscountPercent"] == 30, b)
pv = spare_vehicle("BIZ-POL-")
kid_seat = call("GET", f"/seats/search?transportId={pv}&size=1", token=owner)[1]["data"]["content"][0]
call("PATCH", f"/seats/{kid_seat['id']}", {"chairType": "KIDS_CHAIR"}, owner)
st, tp = tour("BIZ-POLICY-", 150, days=1, vehicle=pv)
tp_policy = (tp.get("data") or {}).get("policy") or {}
check("tour responses carry the agency rules for customers",
      tp_policy.get("kidsDiscountPercent") == 30 and "30%" in tp_policy.get("summary", ""), tp_policy)
st, r = book(bob, tp["data"]["id"], [{"assignedSeat": {"id": kid_seat["id"]}}])
check("the kids chair now costs 70 (30% off 100)", st == 200 and money(r["data"]["totalPrice"]) == 70,
      r.get("data", {}).get("totalPrice") if isinstance(r.get("data"), dict) else r)
if st == 200:
    call("PATCH", f"/reservations/{r['data']['id']}/cancel", token=bob)
refund_case("BIZ-PR20-", 20, 0.25, carol, carol_id)
refund_case("BIZ-PR35-", 35, 1.0, carol, carol_id)
st, b = call("PUT", pol_url, {"kidsDiscountPercent": 50, "fullRefundDays": 14, "partialRefundDays": 7,
                              "partialRefundPercent": 50}, owner)
check("the owner restores the default rules", st == 200, b)

print(f"\n{sum(ok)}/{len(ok)} business checks passed")
