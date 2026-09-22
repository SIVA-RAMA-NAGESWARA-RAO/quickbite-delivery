import { createContext, useContext, useEffect, useMemo, useState } from "react";
import apiClient from "../api/client";

const AuthContext = createContext(null);

const STORAGE_TOKEN = "quickbite_token";
const STORAGE_USER = "quickbite_user";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem(STORAGE_USER);
    return raw ? JSON.parse(raw) : null;
  });

  useEffect(() => {
    if (user) {
      localStorage.setItem(STORAGE_USER, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_USER);
    }
  }, [user]);

  async function login(email, password) {
    const { data } = await apiClient.post("/auth/login", { email, password });
    applySession(data);
    return data;
  }

  async function register(payload) {
    const { data } = await apiClient.post("/auth/register", payload);
    applySession(data);
    return data;
  }

  function applySession(data) {
    localStorage.setItem(STORAGE_TOKEN, data.token);
    const sessionUser = {
      id: data.userId,
      fullName: data.fullName,
      email: data.email,
      role: data.role,
    };
    setUser(sessionUser);
  }

  function logout() {
    localStorage.removeItem(STORAGE_TOKEN);
    setUser(null);
  }

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!user,
      isCustomer: user?.role === "CUSTOMER",
      isRestaurantOwner: user?.role === "RESTAURANT_OWNER",
      login,
      register,
      logout,
    }),
    [user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
