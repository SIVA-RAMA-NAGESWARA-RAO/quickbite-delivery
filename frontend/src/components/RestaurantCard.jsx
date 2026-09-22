import { Link } from "react-router-dom";
import { resolveImageUrl } from "../utils/format";
import "./RestaurantCard.css";

const PLACEHOLDER_TONES = ["tone-chili", "tone-turmeric", "tone-basil"];

export default function RestaurantCard({ restaurant }) {
  const tone = PLACEHOLDER_TONES[restaurant.id % PLACEHOLDER_TONES.length];

  return (
    <Link to={`/restaurants/${restaurant.id}`} className="r-card card">
      <div className={`r-card-media ${tone}`}>
        {restaurant.imageUrl ? (
          <img src={resolveImageUrl(restaurant.imageUrl)} alt={restaurant.name} loading="lazy" />
        ) : (
          <span className="r-card-initial">{restaurant.name[0]}</span>
        )}
        {!restaurant.open && <span className="r-card-closed">Closed</span>}
      </div>
      <div className="r-card-body">
        <div className="r-card-top">
          <h3>{restaurant.name}</h3>
          <span className="r-card-rating">★ {restaurant.rating?.toFixed(1) ?? "4.0"}</span>
        </div>
        <p className="r-card-meta">
          {restaurant.cuisineType || "Multi-cuisine"} · {restaurant.city}
        </p>
      </div>
    </Link>
  );
}
