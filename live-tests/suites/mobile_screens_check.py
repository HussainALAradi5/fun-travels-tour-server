"""Calls every endpoint the new mobile screens use, in app order, and checks the fields they read.
Usage: python -I mobile_screens_check.py <origin> <seed_password>"""
import base64
import json
import sys
import time
import urllib.error
import urllib.request

ORIGIN, PW = sys.argv[1], sys.argv[2]
BASE = ORIGIN + "/api"
ok = []


def call(method, path, body=None, token=None, raw=False, absolute=False):
    req = urllib.request.Request((path if absolute else BASE + path), data=None if body is None else json.dumps(body).encode(), method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            data = r.read()
            return r.status, (data if raw else json.loads(data or b"{}"))
    except urllib.error.HTTPError as e:
        data = e.read()
        try:
            return e.code, json.loads(data or b"{}")
        except ValueError:
            return e.code, {}


def check(name, cond, detail=""):
    ok.append(bool(cond))
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f" -> {str(detail)[:300]}"))


def has(obj, *keys):
    return isinstance(obj, dict) and all(k in obj for k in keys)


tok = call("POST", "/auth/login", {"identifier": "customer", "password": PW})[1]["data"]["token"]
stamp = str(int(time.time()))[-6:]

# catalog with the web's filters (tours tab)
st, countries = call("GET", "/countries")
check("countries for pickers (public)", st == 200 and has(countries["data"][0], "id", "famousName", "dialCode", "countryCode"))
bh = next(c for c in countries["data"] if c["countryCode"] == "BH")
st, b = call("GET", f"/tours/catalog?startCountryId={bh['id']}&startDate=2026-01-01&endDate=2030-12-31&size=20", token=tok)
check("catalog with country and date filters", st == 200 and "content" in b["data"], b)

# profile tab + edit
st, p = call("GET", "/users/me/profile", token=tok)
check("profile tab fields", st == 200 and has(p["data"], "name", "userName", "userType", "email", "mobileNumber", "memberSince", "wallet", "profileImageUrl"), p)
png = base64.b64encode(bytes.fromhex("89504e470d0a1a0a0000000d4948445200000001000000010806000000"
                                     "1f15c4890000000d49444154789c6360000002000154a24f5d0000000049454e44ae426082")).decode()
st, u = call("PUT", "/users/me/profile", {"name": "John Customer", "mobileNumber": "+973 1000 0005", "age": 33,
                                          "base64Image": "data:image/png;base64," + png, "removeImage": False}, tok)
check("profile edit with photo and spaced phone", st == 200 and u["data"]["age"] == 33 and u["data"]["mobileNumber"] == "+97310000005"
      and (u["data"]["profileImageUrl"] or "").startswith("/uploads/"), (st, u))
st, img = call("GET", ORIGIN + u["data"]["profileImageUrl"], raw=True, absolute=True)
check("avatar image loads from API_ORIGIN + path", st == 200 and img[:4] == b"\x89PNG")
call("PUT", "/users/me/profile", {"removeImage": True}, tok)

# bookings -> boarding pass
st, tickets = call("GET", "/tickets/search?size=5&sortDir=desc", token=tok)
t0 = tickets["data"]["content"][0]
st, t = call("GET", f"/tickets/{t0['id']}", token=tok)
check("ticket detail fields", st == 200 and has(t["data"], "ticketNumber", "ticketStatus", "paid", "totalPrice", "bookingDate", "customer", "assignedSeat", "tour"), t)
st, tour = call("GET", f"/tours/{t['data']['tour']['id']}", token=tok)
check("boarding pass tour fields", st == 200 and has(tour["data"], "startCity", "endCity", "startCountry", "endCountry", "transportation", "startDate", "endDate"), tour)

# wallet -> transactions, payments
uid = p["data"]["id"]
call("POST", f"/transactions/manual-credit/{uid}?amount=25&description=mobile-check",
     token=call("POST", "/auth/login", {"identifier": "admin", "password": PW})[1]["data"]["token"])
st, hist = call("GET", f"/accounts/user/{uid}/history", token=tok)
tx = hist["data"][0]
st, d = call("GET", f"/transactions/{tx['id']}", token=tok)
check("transaction detail fields", st == 200 and has(d["data"], "amount", "transactionType", "timestamp", "description", "account"), d)
st, pays = call("GET", "/payments?size=50&sortDir=desc", token=tok)
check("payments list", st == 200 and "content" in pays["data"], pays)
if pays["data"]["content"]:
    pay = pays["data"]["content"][0]
    st, pd = call("GET", f"/payments/{pay['id']}", token=tok)
    check("payment detail fields", st == 200 and has(pd["data"], "amount", "method", "status", "paymentDate", "transactionId", "reservation"), pd)

# support requests
st, r = call("POST", "/user-requests", {"type": "SUPPORT", "title": "Seat question " + stamp, "description": "Can I change my seat?"}, tok)
check("create support request", st in (200, 201) and has(r["data"], "id", "status", "title", "type", "createdAt"), (st, r))
rid = r["data"]["id"]
st, lst = call("GET", "/user-requests?size=50&sortDir=desc", token=tok)
check("support request list has it", st == 200 and any(x["id"] == rid for x in lst["data"]["content"]), lst)
st, c = call("POST", f"/tracking/USER_REQUEST/{rid}/comments", {"content": "Adding details"}, tok)
check("reply on request", st in (200, 201), (st, c))
st, tl = call("GET", f"/tracking/USER_REQUEST/{rid}", token=tok)
check("timeline comments fields", st == 200 and tl["data"]["comments"] and has(tl["data"]["comments"][0], "content", "author", "createdAt"), tl)
check("delete pending request", call("DELETE", f"/user-requests/{rid}", token=tok)[0] == 200)

# reports: generate, download with token, list, delete
for rtype in ("TICKETS", "TRANSACTIONS", "TOUR_CATALOG"):
    st, m = call("POST", "/reports", {"type": rtype, "format": "PDF"}, tok)
    good = st == 201 and has(m["data"], "id", "fileName", "contentType", "downloadPath", "recordCount", "sizeBytes", "reportFormat")
    check(f"export {rtype}", good, (st, m))
    if good:
        st, data = call("GET", ORIGIN + "/api" + m["data"]["downloadPath"], token=tok, raw=True, absolute=True)
        check(f"download {rtype} with bearer token", st == 200 and data[:4] == b"%PDF")
st, mine = call("GET", "/media?category=REPORT&size=50", token=tok)
check("my reports list", st == 200 and len(mine["data"]["content"]) >= 3, mine)
check("delete a report", call("DELETE", f"/media/{mine['data']['content'][0]['id']}", token=tok)[0] == 200)

# password reset with a wrong code
st, b = call("POST", "/auth/password-reset/confirm", {"identifier": "customer@funtravel.com", "token": "wrong", "newPassword": "secret12"})
check("reset with wrong code rejected", st in (400, 409), (st, b))

# notifications point at screens the app can open
st, notes = call("GET", f"/notifications/user/{uid}", token=tok)
routable = {"RESERVATION", "TICKET", "TOUR", "USER_REQUEST"}
check("notification references are routable", st == 200 and all(n.get("referenceType") in routable | {None, "USER"} for n in notes["data"]), notes)
print(f"\n{sum(ok)}/{len(ok)} mobile checks passed")
