export function InlineLoading({ message }: { message: string }) {
  return <p className="muted">{message}</p>;
}

export function EmptyState({ message }: { message: string }) {
  return <p className="empty-state">{message}</p>;
}

export function InlineError({ message }: { message: string }) {
  return <p className="error-banner">{message}</p>;
}

export function InlineSuccess({ message }: { message: string }) {
  return <p className="success-banner">{message}</p>;
}
