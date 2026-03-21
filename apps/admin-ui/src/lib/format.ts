const dateTimeFormatter = new Intl.DateTimeFormat("en-GB", {
  dateStyle: "medium",
  timeStyle: "short",
});

const numberFormatter = new Intl.NumberFormat("en-US");

const percentFormatter = new Intl.NumberFormat("en-US", {
  style: "percent",
  maximumFractionDigits: 2,
});

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "Not available";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return dateTimeFormatter.format(date);
}

export function formatNumber(value: number | null | undefined) {
  if (value == null) {
    return "Not available";
  }

  return numberFormatter.format(value);
}

export function formatPercent(value: number | null | undefined) {
  if (value == null) {
    return "Not available";
  }

  return percentFormatter.format(value);
}

export function formatDelta(
  value: number | null | undefined,
  suffix = "",
) {
  if (value == null) {
    return "Not available";
  }

  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(2)}${suffix}`;
}
