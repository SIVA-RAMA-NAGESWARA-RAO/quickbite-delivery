const currencyFormatter = new Intl.NumberFormat("en-IN", {style:"currency",currency:"INR",maximumFractionDigits:2});
export function formatMoney(amount){return currencyFormatter.format(Number(amount)||0);}
export function formatStatus(status){return status.toLowerCase().split("_").map((w)=>w[0].toUpperCase()+w.slice(1)).join(" ");}
export const STATUS_TONE={CREATED:"muted",PAYMENT_FAILED:"danger",PAYMENT_COMPLETED:"info",ACCEPTED:"success",REJECTED:"danger",PREPARING:"info",OUT_FOR_DELIVERY:"info",DELIVERED:"success",CANCELLED:"muted"};
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";
export function resolveImageUrl(path){if(!path)return null;if(path.startsWith("http://")||path.startsWith("https://"))return path;return `${API_BASE}${path}`;}
