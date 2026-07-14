import { createContext, useContext, useState, useCallback, type ReactNode } from "react";
import type { AuthUser } from "@/lib/types/user";
import { logout as logoutApi } from "@/lib/api/auth-api";

interface SocialLoginUser {
  userId: number;
  nickname: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  loginWithSocial: (result: SocialLoginUser) => void;
  logout: () => Promise<void>;
  updateUser: (updates: Partial<Pick<AuthUser, "nickname">>) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);

  const loginWithSocial = useCallback((result: SocialLoginUser) => {
    setUser({ userId: result.userId, nickname: result.nickname });
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutApi();
    } catch {
      // 서버 세션 정리 실패와 무관하게 클라이언트 인증 상태는 반드시 비운다.
    }
    setUser(null);
  }, []);

  const updateUser = useCallback((updates: Partial<Pick<AuthUser, "nickname">>) => {
    setUser((prev) => (prev ? { ...prev, ...updates } : prev));
  }, []);

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated: user !== null, loginWithSocial, logout, updateUser }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
