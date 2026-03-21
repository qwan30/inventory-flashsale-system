function toTone(value: string) {
  const normalized = value.toUpperCase();

  if (["ACTIVE", "COMPLETED", "SUCCESS", "PUBLISHED", "PROMOTED"].includes(normalized)) {
    return "positive";
  }

  if (["WARN", "WARNING", "PENDING", "OPEN", "IN_PROGRESS", "MANUAL"].includes(normalized)) {
    return "warning";
  }

  if (["FAILED", "ERROR", "ENDED", "INACTIVE", "RESOLVED"].includes(normalized)) {
    return "critical";
  }

  return "neutral";
}

export function StatusBadge({ value }: { value: string }) {
  return <span className={`status-badge status-badge-${toTone(value)}`}>{value}</span>;
}
