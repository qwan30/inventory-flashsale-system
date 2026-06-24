import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../state/auth";

/**
 * The main application shell/layout for authenticated routes.
 * Renders a persistent sidebar navigation and a main content area where child views are injected.
 * Adapts available navigation links dynamically based on the current user's role (e.g. Campaigns only for ADMIN).
 */
export function ShellLayout() {
  const { session, logout } = useAuth();

  return (
    <div className="shell">
      <aside className="shell-sidebar">
        <div>
          <p className="eyebrow">Inventory + Flash Sale</p>
          <h1 className="shell-title">Control Tower</h1>
        </div>
        <nav className="shell-nav">
          {session?.role === "ADMIN" ? <NavLink to="/campaigns">Campaigns</NavLink> : null}
          <NavLink to="/ops">Ops</NavLink>
          <NavLink to="/benchmarks">Benchmarks</NavLink>
        </nav>
        <div className="shell-user">
          <div>{session?.displayName}</div>
          <div className="muted">{session?.role}</div>
          <button className="ghost-button" onClick={() => void logout()}>
            Sign out
          </button>
        </div>
      </aside>
      <main className="shell-content">
        <Outlet />
      </main>
    </div>
  );
}
