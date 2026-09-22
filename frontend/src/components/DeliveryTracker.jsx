import { useEffect, useState } from "react";
import apiClient from "../api/client";
import "./DeliveryTracker.css";

const POLL_INTERVAL_MS = 4000;

/** Projects real lat/lng onto a 0-100 square so we can draw a simple,
 *  dependency-free SVG "map" without needing a tiles/maps API key. */
function project(lat, lng, bounds) {
  const latSpan = Math.max(bounds.maxLat - bounds.minLat, 0.001);
  const lngSpan = Math.max(bounds.maxLng - bounds.minLng, 0.001);
  const pad = 0.25;

  const x = ((lng - bounds.minLng) / lngSpan) * (100 - 200 * pad) + 100 * pad;
  const y = 100 - (((lat - bounds.minLat) / latSpan) * (100 - 200 * pad) + 100 * pad);
  return { x, y };
}

export default function DeliveryTracker({ orderId }) {
  const [tracking, setTracking] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function poll() {
      try {
        const { data } = await apiClient.get(`/orders/${orderId}/tracking`);
        if (!cancelled) {
          setTracking(data);
          setError("");
        }
      } catch (err) {
        if (!cancelled) setError(err.message);
      }
    }

    poll();
    const interval = setInterval(poll, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [orderId]);

  if (error) return <p className="tracker-error">{error}</p>;
  if (!tracking) return <p className="tracker-loading">Loading tracking…</p>;

  const bounds = {
    minLat: Math.min(tracking.restaurantLat, tracking.destinationLat),
    maxLat: Math.max(tracking.restaurantLat, tracking.destinationLat),
    minLng: Math.min(tracking.restaurantLng, tracking.destinationLng),
    maxLng: Math.max(tracking.restaurantLng, tracking.destinationLng),
  };

  const restaurantPoint = project(tracking.restaurantLat, tracking.restaurantLng, bounds);
  const destinationPoint = project(tracking.destinationLat, tracking.destinationLng, bounds);
  const currentPoint = project(tracking.currentLat, tracking.currentLng, bounds);

  return (
    <div className="tracker">
      <div className="tracker-map-wrap">
        <svg className="tracker-map" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid meet">
          <line
            x1={restaurantPoint.x} y1={restaurantPoint.y}
            x2={destinationPoint.x} y2={destinationPoint.y}
            stroke="#e7e2d6" strokeWidth="1.5" strokeDasharray="3,2"
          />
          <circle cx={restaurantPoint.x} cy={restaurantPoint.y} r="2.6" fill="#e14434" />
          <circle cx={destinationPoint.x} cy={destinationPoint.y} r="2.6" fill="#3f7a4e" />
          <circle cx={currentPoint.x} cy={currentPoint.y} r="3.2" fill="#f0a836" stroke="#fff" strokeWidth="1">
            {tracking.status === "OUT_FOR_DELIVERY" && (
              <animate attributeName="r" values="3.2;4;3.2" dur="1.4s" repeatCount="indefinite" />
            )}
          </circle>
        </svg>
        <div className="tracker-legend">
          <span><i className="dot dot-restaurant" /> Restaurant</span>
          <span><i className="dot dot-rider" /> Rider</span>
          <span><i className="dot dot-home" /> You</span>
        </div>
      </div>

      <div className="tracker-info">
        <div className="tracker-progress-track">
          <div className="tracker-progress-fill" style={{ width: `${tracking.progressPercent}%` }} />
        </div>
        <div className="tracker-stats">
          <span>{tracking.progressPercent}% of the way there</span>
          <span>{tracking.etaMinutesRemaining > 0 ? `${tracking.etaMinutesRemaining} min left` : "Arriving now"}</span>
        </div>
        {tracking.deliveryPartnerName && (
          <p className="tracker-partner">
            🛵 {tracking.deliveryPartnerName} · {tracking.deliveryPartnerPhone}
          </p>
        )}
        <p className="tracker-disclaimer">Simulated live tracking for demo purposes.</p>
      </div>
    </div>
  );
}
