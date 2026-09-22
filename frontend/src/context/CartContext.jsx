import { createContext, useContext, useMemo, useState } from "react";

const CartContext = createContext(null);

/**
 * The cart is scoped to a single restaurant at a time — QuickBite doesn't
 * merge items from different kitchens into one order, same as most
 * real-world delivery apps. Adding from a new restaurant clears the old cart.
 */
export function CartProvider({ children }) {
  const [restaurant, setRestaurant] = useState(null); // { id, name }
  const [items, setItems] = useState([]); // [{ menuItemId, name, price, quantity }]

  function addItem(restaurantInfo, menuItem) {
    if (restaurant && restaurant.id !== restaurantInfo.id) {
      const confirmed = window.confirm(
        `Your cart has items from ${restaurant.name}. Start a new order from ${restaurantInfo.name} instead?`
      );
      if (!confirmed) return;
      setItems([]);
    }
    setRestaurant(restaurantInfo);
    setItems((prev) => {
      const existing = prev.find((i) => i.menuItemId === menuItem.id);
      if (existing) {
        return prev.map((i) =>
          i.menuItemId === menuItem.id ? { ...i, quantity: i.quantity + 1 } : i
        );
      }
      return [
        ...prev,
        { menuItemId: menuItem.id, name: menuItem.name, price: menuItem.price, quantity: 1 },
      ];
    });
  }

  function updateQuantity(menuItemId, quantity) {
    if (quantity <= 0) {
      setItems((prev) => prev.filter((i) => i.menuItemId !== menuItemId));
      return;
    }
    setItems((prev) => prev.map((i) => (i.menuItemId === menuItemId ? { ...i, quantity } : i)));
  }

  function clearCart() {
    setItems([]);
    setRestaurant(null);
  }

  const total = useMemo(
    () => items.reduce((sum, i) => sum + i.price * i.quantity, 0),
    [items]
  );

  const itemCount = useMemo(() => items.reduce((sum, i) => sum + i.quantity, 0), [items]);

  const value = { restaurant, items, addItem, updateQuantity, clearCart, total, itemCount };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart must be used within a CartProvider");
  return ctx;
}
