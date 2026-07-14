import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";

export function ProtectedRoute() {
  const { isAuthenticated, isAuthLoading } = useAuth();
  const { hasActiveRound, hasEverStartedRound, isRoundLoading } = useRound();

  if (isAuthLoading) return null;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (isRoundLoading) return null;
  if (!hasActiveRound && !hasEverStartedRound) return <Navigate to="/round/new" replace />;

  return <Outlet />;
}
