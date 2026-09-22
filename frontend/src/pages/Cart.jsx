import { useState } from "react";
import { useNavigate } from "react-router-dom";
import apiClient from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import { formatMoney } from "../utils/format";
import "./Cart.css";

const PAYMENT_METHODS = [
  { value: "CARD", label: "Credit / debit card" },
  { value: "UPI", label: "UPI" },
  { value: "WALLET", label: "Wallet" },
  { value: "CASH_ON_DELIVERY", label: "Cash on delivery" },
];

export default function Cart() {
  const { restaurant, items, updateQuantity, total, clearCart } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [address, setAddress] = useState("");
  const [method, setMethod] = useState("UPI");
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState("");

  async function handlePlaceOrder(e) {
    e.preventDefault();
    if (!isAuthenticated) {
      navigate("/login", { state: { from: { pathname: "/cart" } } });
      return;
    }
    setPlacing(true);
    setError("");
    try {
      const { data } = await apiClient.post("/orders", {
        restaurantId: restaurant.id,
        items: items.map((i) => ({ menuItemId: i.menuItemId, quantity: i.quantity })),
        deliveryAddress: address,
        paymentMethod: method,
      });
      clearCart();
      navigate(`/orders?justPlaced=${data.id}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setPlacing(false);
    }
  }

  if (items.length === 0) {
    return (
      <div className="container">
        <div className="empty-state">
          <h2>Your cart is empty</h2>
          <p>Browse restaurants and add a few dishes to get started.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container cart-page">
      <h1>Your order</h1>
      <p className="cart-restaurant">From {restaurant?.name}</p>

      <div className="cart-layout">
        <div className="cart-items card">
          {items.map((item) => (
            <div key={item.menuItemId} className="cart-line">
              <div>
                <p className="cart-line-name">{item.name}</p>
                <p className="cart-line-price">{formatMoney(item.price)} each</p>
              </div>
              <div className="qty-stepper">
                <button onClick={() => updateQuantity(item.menuItemId, item.quantity - 1)} aria-label="Decrease quantity">−</button>
                <span>{item.quantity}</span>
                <button onClick={() => updateQuantity(item.menuItemId, item.quantity + 1)} aria-label="Increase quantity">+</button>
              </div>
              <p className="cart-line-total">{formatMoney(item.price * item.quantity)}</p>
            </div>
          ))}
          <div className="cart-total-row">
            <span>Total</span>
            <span>{formatMoney(total)}</span>
          </div>
        </div>

        <form className="cart-checkout card" onSubmit={handlePlaceOrder}>
          <h2>Delivery details</h2>
          {error && <div className="error-banner">{error}</div>}
          <div className="field">
            <label htmlFor="address">Delivery address</label>
            <textarea
              id="address"
              required
              rows={3}
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="Flat, street, landmark, city"
            />
          </div>
          <div className="field">
            <label htmlFor="method">Payment method</label>
            <select id="method" value={method} onChange={(e) => setMethod(e.target.value)}>
              {PAYMENT_METHODS.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </select>
          </div>
          <button className="btn btn-primary btn-block" disabled={placing}>
            {placing ? "Placing order…" : `Pay ${formatMoney(total)} & place order`}
          </button>
          <p className="cart-surge-note">
            ⚡ During peak hours or when a restaurant is very busy, a small
            demand-based surcharge may apply — you'll see the exact total on
            your order confirmation.
          </p>
          {!isAuthenticated && (
            <p className="cart-login-hint">You'll be asked to log in first.</p>
          )}
        </form>
      </div>
    </div>
  );
}
