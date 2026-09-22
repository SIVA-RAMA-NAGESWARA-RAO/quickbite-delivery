import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import apiClient from "../api/client";
import DeliveryTracker from "../components/DeliveryTracker";
import StatusBadge from "../components/StatusBadge";
import { formatMoney } from "../utils/format";
import "./MyOrders.css";

const CANCELLABLE = new Set(["CREATED", "PAYMENT_COMPLETED"]);

function RateOrderForm({ order, onSubmit }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (rating === 0) return;
    setSubmitting(true);
    try {
      await onSubmit(order.id, rating, comment);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="rate-order" onSubmit={handleSubmit}>
      <p className="rate-order-label">How was it?</p>
      <div className="rate-stars">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            type="button"
            key={n}
            className={`rate-star ${n <= rating ? "rate-star-filled" : ""}`}
            onClick={() => setRating(n)}
            aria-label={`${n} star${n > 1 ? "s" : ""}`}
          >
            ★
          </button>
        ))}
      </div>
      <input
        className="rate-comment"
        placeholder="Add a comment (optional)"
        value={comment}
        onChange={(e) => setComment(e.target.value)}
      />
      <button className="btn btn-secondary btn-sm" disabled={rating === 0 || submitting}>
        {submitting ? "Submitting…" : "Submit rating"}
      </button>
    </form>
  );
}

export default function MyOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchParams] = useSearchParams();
  const justPlaced = searchParams.get("justPlaced");

  useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const { data } = await apiClient.get("/orders/customer/me");
      setOrders(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleCancel(orderId) {
    if (!window.confirm("Cancel this order?")) return;
    try {
      await apiClient.put(`/orders/${orderId}/cancel`);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleRate(orderId, rating, comment) {
    try {
      await apiClient.post(`/orders/${orderId}/rating`, { rating, comment });
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="container orders-page">
      <h1>My orders</h1>

      {justPlaced && (
        <div className="success-banner">
          Order #{justPlaced} placed! Track its status below.
        </div>
      )}
      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <div className="empty-state"><div className="spinner" style={{ margin: "0 auto" }} /></div>
      ) : orders.length === 0 ? (
        <div className="empty-state">
          <h2>No orders yet</h2>
          <p>Once you place an order, you'll be able to track it here.</p>
        </div>
      ) : (
        <div className="order-list">
          {orders.map((order) => (
            <div key={order.id} className="order-card card">
              <div className="order-card-header">
                <div>
                  <p className="order-id">Order #{order.id}</p>
                  <p className="order-date">{new Date(order.createdAt).toLocaleString()}</p>
                </div>
                <StatusBadge status={order.status} />
              </div>
              <ul className="order-items">
                {order.items.map((item) => (
                  <li key={item.menuItemId}>
                    {item.quantity} × {item.itemName}
                    <span>{formatMoney(item.lineTotal)}</span>
                  </li>
                ))}
              </ul>
              <div className="order-card-footer">
                <span className="order-total">Total: {formatMoney(order.totalAmount)}</span>
                {CANCELLABLE.has(order.status) && (
                  <button className="btn btn-danger btn-sm" onClick={() => handleCancel(order.id)}>
                    Cancel order
                  </button>
                )}
              </div>
              {order.surgeReason && (
                <p className="order-surge">
                  ⚡ Includes a demand-based surcharge — {order.surgeReason.toLowerCase()}
                  {" "}(base price {formatMoney(order.baseAmount)})
                </p>
              )}
              {(order.status === "OUT_FOR_DELIVERY" || order.status === "DELIVERED") && (
                <DeliveryTracker orderId={order.id} />
              )}
              {order.status === "REJECTED" && order.rejectionReason && (
                <p className="order-rejection">Reason: {order.rejectionReason}</p>
              )}
              {order.status === "DELIVERED" && order.customerRating == null && (
                <RateOrderForm order={order} onSubmit={handleRate} />
              )}
              {order.customerRating != null && (
                <p className="order-rated">
                  You rated this order {"★".repeat(order.customerRating)}
                  {"☆".repeat(5 - order.customerRating)}
                  {order.ratingComment ? ` — "${order.ratingComment}"` : ""}
                </p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
