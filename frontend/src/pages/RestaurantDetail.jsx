import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import apiClient from "../api/client";
import { useCart } from "../context/CartContext";
import { formatMoney, resolveImageUrl } from "../utils/format";
import "./RestaurantDetail.css";

export default function RestaurantDetail() {
  const { id } = useParams();
  const { addItem, items } = useCart();
  const [restaurant, setRestaurant] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [addedFlash, setAddedFlash] = useState(null);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    apiClient
      .get(`/restaurants/${id}`)
      .then(({ data }) => !ignore && setRestaurant(data))
      .catch((err) => !ignore && setError(err.message))
      .finally(() => !ignore && setLoading(false));
    return () => {
      ignore = true;
    };
  }, [id]);

  function quantityInCart(menuItemId) {
    return items.find((i) => i.menuItemId === menuItemId)?.quantity || 0;
  }

  function handleAdd(item) {
    addItem({ id: Number(id), name: restaurant.name }, item);
    setAddedFlash(item.id);
    setTimeout(() => setAddedFlash(null), 900);
  }

  if (loading) {
    return <div className="empty-state"><div className="spinner" style={{ margin: "0 auto" }} /></div>;
  }

  if (error) {
    return (
      <div className="container">
        <div className="error-banner">{error}</div>
      </div>
    );
  }

  const grouped = restaurant.menuItems.reduce((acc, item) => {
    const category = item.category || "Menu";
    (acc[category] ||= []).push(item);
    return acc;
  }, {});

  return (
    <div className="container r-detail">
      {restaurant.imageUrl && (
        <img className="r-detail-banner" src={resolveImageUrl(restaurant.imageUrl)} alt={restaurant.name} />
      )}
      <div className="r-detail-header">
        <div>
          <h1>{restaurant.name}</h1>
          <p className="r-detail-meta">
            {restaurant.cuisineType || "Multi-cuisine"} · {restaurant.city}
            {!restaurant.open && <span className="r-detail-closed"> · Currently closed</span>}
          </p>
          {restaurant.description && <p className="r-detail-desc">{restaurant.description}</p>}
        </div>
        <Link to="/cart" className="btn btn-secondary">View cart</Link>
      </div>

      {Object.entries(grouped).map(([category, categoryItems]) => (
        <div key={category} className="menu-section">
          <h2>{category}</h2>
          <div className="menu-list">
            {categoryItems.map((item) => (
              <div key={item.id} className="menu-item card">
                {item.imageUrl && (
                  <img className="menu-item-thumb" src={resolveImageUrl(item.imageUrl)} alt={item.name} />
                )}
                <div className="menu-item-info">
                  <h3>{item.name}</h3>
                  {item.description && <p className="menu-item-desc">{item.description}</p>}
                  <p className="menu-item-price">{formatMoney(item.price)}</p>
                </div>
                <button
                  className="btn btn-primary btn-sm"
                  disabled={!item.available || !restaurant.open}
                  onClick={() => handleAdd(item)}
                >
                  {!item.available
                    ? "Sold out"
                    : addedFlash === item.id
                    ? "Added ✓"
                    : quantityInCart(item.id) > 0
                    ? `Add another (${quantityInCart(item.id)} in cart)`
                    : "Add"}
                </button>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
