import { useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../state/auth";

export function LoginPage() {
  const { session, login } = useAuth();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("Admin123!");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (session) {
    return <Navigate to="/campaigns" replace />;
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login({ username, password });
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Login failed");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-screen">
      <form className="login-card" onSubmit={handleSubmit}>
        <p className="eyebrow">Admin Surface</p>
        <h1>Inventory Control Tower</h1>
        <label>
          Username
          <input value={username} onChange={(event) => setUsername(event.target.value)} />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        {error ? <p className="error-banner">{error}</p> : null}
        <button type="submit" disabled={submitting}>
          {submitting ? "Signing in..." : "Sign in"}
        </button>
      </form>
    </div>
  );
}
