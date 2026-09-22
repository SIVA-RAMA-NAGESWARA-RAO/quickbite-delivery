import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import "./Navbar.css";

export default function Navbar() {
  const { user, isAuthenticated, isRestaurantOwner, logout } = useAuth();
  const { itemCount } = useCart();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/");
  }

  return (
    <header className="nav">
      <div className="container nav-inner">
        <Link to="/" className="nav-logo">
          Quick<span>Bite</span>
        </Link>

        <nav className="nav-links">
          {isRestaurantOwner ? (
            <>
              <Link to="/owner">My restaurant</Link>
              <Link to="/owner/orders">Incoming orders</Link>
            </>
          ) : (
            <>
              <Link to="/">Browse</Link>
              {isAuthenticated && <Link to="/orders">My orders</Link>}
            </>
          )}
        </nav>

        <div className="nav-actions">
          {!isRestaurantOwner && (
            <Link to="/cart" className="nav-cart" aria-label={`Cart, ${itemCount} items`}>
              🛒
              {itemCount > 0 && <span className="nav-cart-count">{itemCount}</span>}
            </Link>
          )}

          {isAuthenticated ? (
            <div className="nav-user">
              <span className="nav-user-name">{user.fullName.split(" ")[0]}</span>
              <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
                Log out
              </button>
            </div>
          ) : (
            <>
              <Link to="/login" className="btn btn-secondary btn-sm">Log in</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Sign up</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
