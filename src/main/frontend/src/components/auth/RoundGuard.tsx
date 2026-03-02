import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";

export function RoundGuard() {
  const { isAuthenticated } = useAuth();
  const { hasActiveRound, isRoundLoading } = useRound();

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (isRoundLoading) return null;
  if (hasActiveRound) return <Navigate to="/market" replace />;

  return <Outlet />;
}
