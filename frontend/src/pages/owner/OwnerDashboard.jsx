import { useEffect, useState } from "react";
import apiClient from "../../api/client";
import { formatMoney, resolveImageUrl } from "../../utils/format";
import "./Owner.css";

const emptyRestaurantForm = {
  name: "",
  description: "",
  cuisineType: "",
  address: "",
  city: "",
  contactPhone: "",
  imageUrl: "",
};

const emptyMenuForm = { name: "", description: "", price: "", category: "", available: true };

export default function OwnerDashboard() {
  const [restaurant, setRestaurant] = useState(null);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [restaurantForm, setRestaurantForm] = useState(emptyRestaurantForm);
  const [savingRestaurant, setSavingRestaurant] = useState(false);

  const [menuForm, setMenuForm] = useState(emptyMenuForm);
  const [savingItem, setSavingItem] = useState(false);
  const [editingItemId, setEditingItemId] = useState(null);

  useEffect(() => {
    loadRestaurant();
  }, []);

  async function loadRestaurant() {
    setLoading(true);
    setError("");
    try {
      const { data } = await apiClient.get("/restaurants/owner/me");
      setRestaurant(data);
      loadSummary(data.id);
    } catch {
      // A brand-new restaurant owner won't have a restaurant yet — that's expected.
      setRestaurant(null);
    } finally {
      setLoading(false);
    }
  }

  async function loadSummary(restaurantId) {
    try {
      const { data } = await apiClient.get(`/orders/restaurant/${restaurantId}/summary`);
      setSummary(data);
    } catch {
      // Non-critical — the dashboard still works without the stats strip.
    }
  }

  async function handleCreateRestaurant(e) {
    e.preventDefault();
    setSavingRestaurant(true);
    setError("");
    try {
      const { data } = await apiClient.post("/restaurants", restaurantForm);
      setRestaurant(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingRestaurant(false);
    }
  }

  async function toggleOpen() {
    try {
      const { data } = await apiClient.patch(`/restaurants/${restaurant.id}/status`, null, {
        params: { open: !restaurant.open },
      });
      setRestaurant(data);
    } catch (err) {
      setError(err.message);
    }
  }

  function startEditItem(item) {
    setEditingItemId(item.id);
    setMenuForm({
      name: item.name,
      description: item.description || "",
      price: item.price,
      category: item.category || "",
      available: item.available,
    });
  }

  function resetMenuForm() {
    setEditingItemId(null);
    setMenuForm(emptyMenuForm);
  }

  async function handleSaveMenuItem(e) {
    e.preventDefault();
    setSavingItem(true);
    setError("");
    const payload = { ...menuForm, price: Number(menuForm.price) };
    try {
      if (editingItemId) {
        await apiClient.put(`/restaurants/${restaurant.id}/menu/${editingItemId}`, payload);
      } else {
        await apiClient.post(`/restaurants/${restaurant.id}/menu`, payload);
      }
      const { data } = await apiClient.get(`/restaurants/${restaurant.id}`);
      setRestaurant(data);
      resetMenuForm();
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingItem(false);
    }
  }

  async function toggleItemAvailability(item) {
    try {
      await apiClient.put(`/restaurants/${restaurant.id}/menu/${item.id}`, {
        name: item.name,
        description: item.description,
        price: item.price,
        category: item.category,
        available: !item.available,
      });
      const { data } = await apiClient.get(`/restaurants/${restaurant.id}`);
      setRestaurant(data);
    } catch (err) {
      setError(err.message);
    }
  }

  async function deleteItem(itemId) {
    if (!window.confirm("Remove this item from your menu?")) return;
    try {
      await apiClient.delete(`/restaurants/${restaurant.id}/menu/${itemId}`);
      const { data } = await apiClient.get(`/restaurants/${restaurant.id}`);
      setRestaurant(data);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleRestaurantImageChange(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError("");
    const formData = new FormData();
    formData.append("file", file);
    try {
      const { data } = await apiClient.post(`/restaurants/${restaurant.id}/image`, formData);
      setRestaurant(data);
    } catch (err) {
      setError(err.message);
    } finally {
      e.target.value = "";
    }
  }

  async function handleMenuItemImageChange(itemId, e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError("");
    const formData = new FormData();
    formData.append("file", file);
    try {
      await apiClient.post(`/restaurants/${restaurant.id}/menu/${itemId}/image`, formData);
      const { data } = await apiClient.get(`/restaurants/${restaurant.id}`);
      setRestaurant(data);
    } catch (err) {
      setError(err.message);
    } finally {
      e.target.value = "";
    }
  }

  if (loading) {
    return <div className="empty-state"><div className="spinner" style={{ margin: "0 auto" }} /></div>;
  }

  if (!restaurant) {
    return (
      <div className="container owner-page">
        <h1>Set up your restaurant</h1>
        <p className="owner-sub">This only takes a minute — you can edit it any time.</p>
        {error && <div className="error-banner">{error}</div>}
        <form className="card owner-form" onSubmit={handleCreateRestaurant}>
          <div className="field">
            <label>Restaurant name</label>
            <input required value={restaurantForm.name}
              onChange={(e) => setRestaurantForm({ ...restaurantForm, name: e.target.value })} />
          </div>
          <div className="field">
            <label>Description</label>
            <textarea rows={2} value={restaurantForm.description}
              onChange={(e) => setRestaurantForm({ ...restaurantForm, description: e.target.value })} />
          </div>
          <div className="field-row">
            <div className="field">
              <label>Cuisine type</label>
              <input value={restaurantForm.cuisineType}
                onChange={(e) => setRestaurantForm({ ...restaurantForm, cuisineType: e.target.value })}
                placeholder="Indian, Italian…" />
            </div>
            <div className="field">
              <label>City</label>
              <input required value={restaurantForm.city}
                onChange={(e) => setRestaurantForm({ ...restaurantForm, city: e.target.value })} />
            </div>
          </div>
          <div className="field">
            <label>Address</label>
            <input required value={restaurantForm.address}
              onChange={(e) => setRestaurantForm({ ...restaurantForm, address: e.target.value })} />
          </div>
          <div className="field">
            <label>Contact phone</label>
            <input value={restaurantForm.contactPhone}
              onChange={(e) => setRestaurantForm({ ...restaurantForm, contactPhone: e.target.value })} />
          </div>
          <button className="btn btn-primary btn-block" disabled={savingRestaurant}>
            {savingRestaurant ? "Creating…" : "Create restaurant"}
          </button>
        </form>
      </div>
    );
  }

  return (
    <div className="container owner-page">
      <div className="owner-header">
        <div className="owner-header-identity">
          <label className="owner-cover-upload">
            {restaurant.imageUrl ? (
              <img src={resolveImageUrl(restaurant.imageUrl)} alt={restaurant.name} className="owner-cover-thumb" />
            ) : (
              <span className="owner-cover-placeholder">Add photo</span>
            )}
            <input type="file" accept="image/*" hidden onChange={handleRestaurantImageChange} />
          </label>
          <div>
            <h1>{restaurant.name}</h1>
            <p className="owner-sub">{restaurant.cuisineType} · {restaurant.city}</p>
          </div>
        </div>
        <button className={`btn ${restaurant.open ? "btn-secondary" : "btn-success"}`} onClick={toggleOpen}>
          {restaurant.open ? "Mark as closed" : "Reopen for orders"}
        </button>
      </div>

      {summary && (
        <div className="stats-strip">
          <div className="stat-tile">
            <span className="stat-value">{summary.todayOrders}</span>
            <span className="stat-label">Orders today</span>
          </div>
          <div className="stat-tile">
            <span className="stat-value">{formatMoney(summary.todayRevenue)}</span>
            <span className="stat-label">Revenue today</span>
          </div>
          <div className="stat-tile">
            <span className="stat-value">{summary.totalOrders}</span>
            <span className="stat-label">All-time orders</span>
          </div>
          <div className="stat-tile">
            <span className="stat-value">{formatMoney(summary.allTimeRevenue)}</span>
            <span className="stat-label">All-time revenue</span>
          </div>
          <div className="stat-tile">
            <span className="stat-value">★ {restaurant.rating?.toFixed(1) ?? "4.0"}</span>
            <span className="stat-label">{restaurant.ratingCount ?? 0} ratings</span>
          </div>
        </div>
      )}

      {error && <div className="error-banner">{error}</div>}

      <div className="owner-layout">
        <div className="card owner-menu-list">
          <h2>Menu items</h2>
          {restaurant.menuItems.length === 0 && (
            <p className="owner-empty">No menu items yet — add your first dish.</p>
          )}
          {restaurant.menuItems.map((item) => (
            <div key={item.id} className="owner-menu-item">
              <label className="owner-item-thumb-upload">
                {item.imageUrl ? (
                  <img src={resolveImageUrl(item.imageUrl)} alt={item.name} className="owner-item-thumb" />
                ) : (
                  <span className="owner-item-thumb-placeholder">+</span>
                )}
                <input type="file" accept="image/*" hidden onChange={(e) => handleMenuItemImageChange(item.id, e)} />
              </label>
              <div className="owner-menu-item-info">
                <p className="owner-menu-name">{item.name}</p>
                <p className="owner-menu-meta">
                  {item.category || "Uncategorized"} · {formatMoney(item.price)}
                </p>
              </div>
              <div className="owner-menu-actions">
                <button
                  className={`btn btn-sm ${item.available ? "btn-secondary" : "btn-success"}`}
                  onClick={() => toggleItemAvailability(item)}
                >
                  {item.available ? "Mark sold out" : "Mark available"}
                </button>
                <button className="btn btn-secondary btn-sm" onClick={() => startEditItem(item)}>Edit</button>
                <button className="btn btn-danger btn-sm" onClick={() => deleteItem(item.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>

        <form className="card owner-form" onSubmit={handleSaveMenuItem}>
          <h2>{editingItemId ? "Edit item" : "Add a menu item"}</h2>
          <div className="field">
            <label>Name</label>
            <input required value={menuForm.name} onChange={(e) => setMenuForm({ ...menuForm, name: e.target.value })} />
          </div>
          <div className="field">
            <label>Description</label>
            <textarea rows={2} value={menuForm.description}
              onChange={(e) => setMenuForm({ ...menuForm, description: e.target.value })} />
          </div>
          <div className="field-row">
            <div className="field">
              <label>Price (₹)</label>
              <input required type="number" min="1" step="0.5" value={menuForm.price}
                onChange={(e) => setMenuForm({ ...menuForm, price: e.target.value })} />
            </div>
            <div className="field">
              <label>Category</label>
              <input value={menuForm.category} placeholder="Starters, Mains…"
                onChange={(e) => setMenuForm({ ...menuForm, category: e.target.value })} />
            </div>
          </div>
          <div className="owner-form-actions">
            <button className="btn btn-primary" disabled={savingItem}>
              {savingItem ? "Saving…" : editingItemId ? "Save changes" : "Add item"}
            </button>
            {editingItemId && (
              <button type="button" className="btn btn-secondary" onClick={resetMenuForm}>Cancel</button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
