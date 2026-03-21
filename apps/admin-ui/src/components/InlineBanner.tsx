import type { ReactNode } from "react";

export function InlineBanner({
  children,
  variant = "empty",
}: {
  children: ReactNode;
  variant?: "empty" | "error" | "success";
}) {
  const className =
    variant === "error"
      ? "error-banner"
      : variant === "success"
        ? "success-banner"
        : "empty-state";

  return <p className={className}>{children}</p>;
}
