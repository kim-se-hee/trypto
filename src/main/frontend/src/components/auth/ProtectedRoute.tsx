import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";

export function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  const { hasActiveRound } = useRound();

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (!hasActiveRound) return <Navigate to="/round/new" replace />;

  return <Outlet />;
}
