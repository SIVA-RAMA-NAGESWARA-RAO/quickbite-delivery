import axios from "axios";

const configuredBaseURL = import.meta.env.VITE_API_BASE_URL;\nconst baseURL = configuredBaseURL\n  ? `${configuredBaseURL.replace(/\\/$/, "")}/api`\n  : "http://localhost:8080/api";

const apiClient = axios.create({ baseURL });

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("quickbite_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use((response) => response,(error) => {
  const backendMessage = error.response?.data?.message;
  const message = backendMessage || (error.response ? `Request failed with status ${error.response.status}` : "Could not reach the server. Is the API running?");
  return Promise.reject(new Error(message));
});
export default apiClient;
