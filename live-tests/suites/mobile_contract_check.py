"""Calls every endpoint the mobile app uses, in its order, and checks the fields its interfaces read.
Usage: python -I mobile_contract.py <origin> <seed_password>"""
import json
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1] + "/api"
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
        return e.code, json.loads(e.read() or b"{}")


def check(name, cond, detail=""):
    ok.append(bool(cond))
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f" -> {detail}"))


def has(obj, *keys):
    return all(k in obj for k in keys)


# register + login (useRegisterForm -> register then signIn by email)
st, b = call("POST", "/auth/register", {"gender": "MALE", "userName": "mobile3", "name": "Mo Bile", "email": "mobile3@example.com",
             "password": "secret12", "mobileNumber": "+97339995557"})
check("register", st == 201, b)
st, b = call("POST", "/auth/login", {"identifier": "mobile3@example.com", "password": "secret12"})
check("login returns token + user", st == 200 and has(b["data"], "token", "user") and has(b["data"]["user"], "id", "name", "email", "userType"), b)
token, uid = b["data"]["token"], b["data"]["user"]["id"]
admin = call("POST", "/auth/login", {"identifier": "admin", "password": sys.argv[2]})[1]["data"]["token"]
call("POST", f"/transactions/manual-credit/{uid}?amount=500&description=mobile-test", token=admin)

st, b = call("GET", "/tours/catalog?size=20", token=token)
tours = b["data"]["content"]
check("catalog page", st == 200 and has(b["data"], "content", "totalPages") and tours, b)
tour = next(t for t in tours if t["hasTransportation"] and t["availableSlots"] > 0)
check("tour card fields", has(tour, "id", "title", "totalPrice", "startDate", "availableSlots", "startCity", "endCity"), tour)
st, detail = call("GET", f"/tours/{tour['id']}", token=token)
check("tour detail with availableMeals", st == 200 and "availableMeals" in detail["data"], detail)
st, seats = call("GET", f"/seats/search?tourId={tour['id']}&size=100", token=token)
free = [s for s in seats["data"]["content"] if s["status"] == "AVAILABLE"]
check("per-tour seats", st == 200 and free and has(free[0], "id", "seatCode", "status"), seats)
meals = [{"id": m["id"]} for m in detail["data"]["availableMeals"][:1]]
st, res = call("POST", "/reservations", {"tour": {"id": tour["id"]}, "tickets": [
    {"assignedSeat": {"id": free[0]["id"]}, "selectedMeals": meals}, {}]}, token)
check("create reservation", st == 200 and has(res["data"], "id", "reservationNumber", "status", "totalPrice", "holdExpiresAt"), res)
rid = res["data"]["id"]
check("reservation list", call("GET", "/reservations?sortDir=desc&size=50", token=token)[1]["data"]["content"][0]["id"] == rid)
st, tickets = call("GET", f"/tickets/search?reservationId={rid}&size=50", token=token)
tl = tickets["data"]["content"]
check("tickets by reservation", st == 200 and len(tl) == 2 and has(tl[0], "ticketNumber", "ticketStatus", "paid", "totalPrice"), tickets)
st, paid = call("POST", f"/payments/execute/{rid}?method=WALLET", token=token)
check("pay with wallet", st == 200 and paid["data"]["status"] == "CONFIRMED", paid)
tl = call("GET", f"/tickets/search?reservationId={rid}&size=50", token=token)[1]["data"]["content"]
check("paid tickets expose QR", all(t["paid"] and t.get("qrCode", "").startswith("data:image/png") for t in tl), tl)
st, cancelled = call("PUT", f"/tickets/{tl[0]['id']}/cancel", token=token)
check("cancel one ticket", st == 200 and cancelled["data"]["ticketStatus"] == "CANCELLED", cancelled)
st, acct = call("GET", f"/accounts/user/{uid}/balance", token=token)
check("wallet balance", st == 200 and has(acct["data"], "balance", "accountNumber"), acct)
st, hist = call("GET", f"/accounts/user/{uid}/history", token=token)
check("wallet history", st == 200 and hist["data"] and has(hist["data"][0], "amount", "transactionType", "timestamp"), hist)
st, notes = call("GET", f"/notifications/user/{uid}", token=token)
check("notifications", st == 200 and isinstance(notes["data"], list), notes)
if notes["data"]:
    n = notes["data"][0]
    check("notification fields", has(n, "id", "title", "message", "isRead", "createdAt"), n)
    check("mark read", call("PUT", f"/notifications/{n['id']}/read", token=token)[0] == 200)
check("counts", call("GET", f"/notifications/user/{uid}/counts", token=token)[1]["data"].get("unreadCount") is not None)
check("cancel reservation", call("PATCH", f"/reservations/{rid}/cancel", token=token)[1]["data"]["status"] == "CANCELLED")
check("expired token -> 401 (app signs out)", call("GET", "/reservations", token="bad.token.value")[0] == 401)
print(f"\n{sum(ok)}/{len(ok)} mobile contract checks passed")
