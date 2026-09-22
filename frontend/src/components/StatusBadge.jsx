import { formatStatus, STATUS_TONE } from "../utils/format";
import "./StatusBadge.css";

export default function StatusBadge({ status }) {
  const tone = STATUS_TONE[status] || "muted";
  return <span className={`badge badge-${tone}`}>{formatStatus(status)}</span>;
}
