import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";

export function PublicRoute() {
  const { isAuthenticated } = useAuth();
  const { hasActiveRound } = useRound();

  if (isAuthenticated && hasActiveRound) return <Navigate to="/market" replace />;
  if (isAuthenticated && !hasActiveRound) return <Navigate to="/round/new" replace />;

  return <Outlet />;
}
