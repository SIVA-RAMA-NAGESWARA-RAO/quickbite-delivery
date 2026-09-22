import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const apiClient = axios.create({ baseURL });

// Attach the JWT (if we have one) to every outgoing request.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("quickbite_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Normalize error responses so components can just read err.message.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const backendMessage = error.response?.data?.message;
    const message =
      backendMessage ||
      (error.response
        ? `Request failed with status ${error.response.status}`
        : "Could not reach the server. Is the API Gateway running?");
    return Promise.reject(new Error(message));
  }
);

export default apiClient;
