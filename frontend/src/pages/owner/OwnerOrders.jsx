import { useEffect, useState } from "react";
import apiClient from "../../api/client";
import { formatMoney } from "../../utils/format";
import "./Owner.css";

const COLUMNS = [
  { key: "PAYMENT_COMPLETED", title: "New & paid", actions: ["accept", "reject"] },
  { key: "ACCEPTED", title: "Accepted", actions: ["advance:PREPARING"] },
  { key: "PREPARING", title: "Preparing", actions: ["advance:OUT_FOR_DELIVERY"] },
  { key: "OUT_FOR_DELIVERY", title: "Out for delivery", actions: ["advance:DELIVERED"] },
];

export default function OwnerOrders() {
  const [restaurantId, setRestaurantId] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    init();
    const interval = setInterval(() => refreshOrders(false), 15000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function init() {
    try {
      const { data } = await apiClient.get("/restaurants/owner/me");
      setRestaurantId(data.id);
      await refreshOrders(true, data.id);
    } catch {
      setError("Set up your restaurant first before you can see orders.");
      setLoading(false);
    }
  }

  async function refreshOrders(showSpinner, idOverride) {
    const id = idOverride || restaurantId;
    if (!id) return;
    if (showSpinner) setLoading(true);
    try {
      const { data } = await apiClient.get(`/orders/restaurant/${id}`);
      setOrders(data);
    } catch (err) {
      setError(err.message);
    } finally {
      if (showSpinner) setLoading(false);
    }
  }

  async function runAction(orderId, action) {
    try {
      if (action === "accept") {
        await apiClient.put(`/orders/${orderId}/accept`);
      } else if (action === "reject") {
        const reason = window.prompt("Reason for rejecting (optional):") || "";
        await apiClient.put(`/orders/${orderId}/reject`, { reason });
      } else if (action.startsWith("advance:")) {
        const target = action.split(":")[1];
        await apiClient.put(`/orders/${orderId}/status`, null, { params: { target } });
      }
      refreshOrders(false);
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) {
    return <div className="empty-state"><div className="spinner" style={{ margin: "0 auto" }} /></div>;
  }

  return (
    <div className="container owner-page">
      <h1>Incoming orders</h1>
      <p className="owner-sub">Updates automatically every 15 seconds.</p>
      {error && <div className="error-banner">{error}</div>}

      <div className="kanban">
        {COLUMNS.map((column) => {
          const columnOrders = orders.filter((o) => o.status === column.key);
          return (
            <div key={column.key} className="kanban-column">
              <h3>
                <span>{column.title}</span>
                <span className="kanban-count">{columnOrders.length}</span>
              </h3>
              {columnOrders.map((order) => (
                <div key={order.id} className="ticket">
                  <p className="ticket-id">#{order.id}</p>
                  <p className="ticket-time">{new Date(order.createdAt).toLocaleTimeString()}</p>
                  <ul className="ticket-items">
                    {order.items.map((item) => (
                      <li key={item.menuItemId}>{item.quantity} × {item.itemName}</li>
                    ))}
                  </ul>
                  <p className="ticket-total">{formatMoney(order.totalAmount)}</p>
                  <div className="ticket-actions">
                    {column.actions.map((action) => (
                      <button
                        key={action}
                        className={`btn btn-sm ${action === "reject" ? "btn-danger" : "btn-success"}`}
                        onClick={() => runAction(order.id, action)}
                      >
                        {action === "accept" && "Accept"}
                        {action === "reject" && "Reject"}
                        {action.startsWith("advance:") && `Move to ${action.split(":")[1].replaceAll("_", " ").toLowerCase()}`}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
              {columnOrders.length === 0 && <p className="owner-empty">Nothing here.</p>}
            </div>
          );
        })}
      </div>
    </div>
  );
}
