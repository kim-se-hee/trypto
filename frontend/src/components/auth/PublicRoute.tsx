import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";

export function PublicRoute() {
  const { isAuthenticated, isAuthLoading } = useAuth();

  if (isAuthLoading) return null;
  if (isAuthenticated) return <Navigate to="/market" replace />;

  return <Outlet />;
}
