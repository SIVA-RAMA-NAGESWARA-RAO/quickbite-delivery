import { useEffect, useState } from "react";
import apiClient from "../api/client";
import RestaurantCard from "../components/RestaurantCard";
import "./Home.css";

const CUISINE_CHIPS = ["All", "Indian", "Italian", "Chinese", "Mexican", "Bakery", "Healthy"];

export default function Home() {
  const [restaurants, setRestaurants] = useState([]);
  const [city, setCity] = useState("");
  const [cuisine, setCuisine] = useState("All");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchRestaurants(city);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function fetchRestaurants(cityFilter) {
    setLoading(true);
    setError("");
    try {
      const params = cityFilter ? { city: cityFilter } : {};
      const { data } = await apiClient.get("/restaurants", { params });
      setRestaurants(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function handleSearch(e) {
    e.preventDefault();
    fetchRestaurants(city);
  }

  const visible =
    cuisine === "All"
      ? restaurants
      : restaurants.filter((r) => (r.cuisineType || "").toLowerCase() === cuisine.toLowerCase());

  return (
    <div>
      <section className="hero">
        <div className="container hero-grid">
          <div>
            <h1 className="hero-title">
              Hungry?
              <br />
              Order now.
            </h1>
            <p className="hero-sub">
              Real kitchens near you, ready in minutes flat — even during the
              lunch rush.
            </p>
            <form className="hero-search" onSubmit={handleSearch}>
              <input
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder="Search by city, e.g. Hyderabad"
                aria-label="Search restaurants by city"
              />
              <button className="btn btn-primary" type="submit">Find food</button>
            </form>
          </div>
          <div className="hero-panel" aria-hidden="true">
            <div className="hero-panel-row">
              <span className="hero-tag tone-chili">🌶️ Spicy</span>
              <span className="hero-tag tone-turmeric">🍛 Comfort</span>
            </div>
            <div className="hero-panel-row">
              <span className="hero-tag tone-basil">🥗 Fresh</span>
              <span className="hero-tag tone-chili">🔥 Trending</span>
            </div>
          </div>
        </div>
      </section>

      <section className="container">
        <div className="chip-row">
          {CUISINE_CHIPS.map((c) => (
            <button
              key={c}
              className={`chip ${cuisine === c ? "chip-active" : ""}`}
              onClick={() => setCuisine(c)}
            >
              {c}
            </button>
          ))}
        </div>

        {error && <div className="error-banner">{error}</div>}

        {loading ? (
          <div className="empty-state"><div className="spinner" style={{ margin: "0 auto" }} /></div>
        ) : visible.length === 0 ? (
          <div className="empty-state">
            <p>No restaurants match that search yet.</p>
          </div>
        ) : (
          <div className="restaurant-grid">
            {visible.map((r) => (
              <RestaurantCard key={r.id} restaurant={r} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
