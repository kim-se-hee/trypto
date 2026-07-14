import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from "react";
import type { AuthUser } from "@/lib/types/user";
import { logout as logoutApi } from "@/lib/api/auth-api";
import { getUserProfile } from "@/lib/api/user-api";

interface SocialLoginUser {
  userId: number;
  nickname: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isAuthLoading: boolean;
  loginWithSocial: (result: SocialLoginUser) => void;
  logout: () => Promise<void>;
  updateUser: (updates: Partial<Pick<AuthUser, "nickname">>) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isAuthLoading, setIsAuthLoading] = useState(true);

  // 새로고침하면 메모리의 인증 상태는 사라지지만 세션 쿠키는 남아 있다.
  // 그 쿠키로 내 정보를 되물어 로그인 상태를 복구한다.
  useEffect(() => {
    let cancelled = false;

    void (async () => {
      try {
        const profile = await getUserProfile();
        if (!cancelled) setUser({ userId: profile.userId, nickname: profile.nickname });
      } catch {
        if (!cancelled) setUser(null);
      } finally {
        if (!cancelled) setIsAuthLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

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
      value={{
        user,
        isAuthenticated: user !== null,
        isAuthLoading,
        loginWithSocial,
        logout,
        updateUser,
      }}
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
