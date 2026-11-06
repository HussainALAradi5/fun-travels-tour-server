// Usage: node websocket_check.mjs <origin> <seed_password>   (after npm ci in live-tests)
import { Client } from "@stomp/stompjs";

const [, , origin, password] = process.argv;
const results = [];

async function login(identifier) {
  const res = await fetch(`${origin}/api/auth/login`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier, password }),
  });
  return (await res.json()).data;
}

/** Connects, subscribes, and resolves "connected", "rejected:<msg>", or "timeout". */
function attempt(token, topic) {
  return new Promise((resolve) => {
    const client = new Client({
      brokerURL: origin.replace(/^http/, "ws") + "/ws-notifications",
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 0,
      onConnect: () => {
        client.subscribe(topic, () => {});
        setTimeout(() => { resolve("subscribed"); client.deactivate(); }, 1500);
      },
      onStompError: (frame) => { resolve("rejected:" + (frame.headers.message || "")); client.deactivate(); },
      onWebSocketClose: () => resolve("closed"),
    });
    client.activate();
    setTimeout(() => { resolve("timeout"); client.deactivate(); }, 6000);
  });
}

const customer = await login("customer");
const other = await login("owner");
const own = `/topic/notifications/${customer.user.id}`;
const foreign = `/topic/notifications/${other.user.id}`;

const check = (name, ok, detail) => { results.push(ok); console.log(`${ok ? "PASS" : "FAIL"} ${name}${ok ? "" : " -> " + detail}`); };
let r = await attempt(null, own);
check("connect without token is rejected", r !== "subscribed", r);
r = await attempt("not.a.token", own);
check("connect with invalid token is rejected", r !== "subscribed", r);
r = await attempt(customer.token, own);
check("user can subscribe to own notification topic", r === "subscribed", r);
r = await attempt(customer.token, foreign);
check("user cannot subscribe to another user's topic", r !== "subscribed", r);
console.log(`\n${results.filter(Boolean).length}/${results.length} websocket checks passed`);
process.exit(0);
