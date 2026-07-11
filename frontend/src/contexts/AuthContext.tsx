import { createContext, useContext, useState, useCallback, type ReactNode } from "react";
import type { MockUser } from "@/lib/types/user";
import { MOCK_USERS } from "@/mocks/auth";

/** true로 바꾸면 로그인 없이 바로 메인 진입 (로그인 화면을 보려면 false) */
const DEV_SKIP_AUTH = false;

interface KakaoLoginUser {
  userId: number;
  nickname: string;
}

interface AuthContextValue {
  user: MockUser | null;
  isAuthenticated: boolean;
  login: (email: string) => boolean;
  loginWithKakao: (result: KakaoLoginUser) => void;
  logout: () => void;
  updateUser: (updates: Partial<Pick<MockUser, "nickname" | "password" | "portfolioPublic">>) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MockUser | null>(DEV_SKIP_AUTH ? MOCK_USERS[0] : null);

  const login = useCallback((email: string): boolean => {
    const found = MOCK_USERS.find((u) => u.email === email);
    if (!found) return false;
    setUser(found);
    return true;
  }, []);

  const loginWithKakao = useCallback((result: KakaoLoginUser) => {
    setUser({
      userId: result.userId,
      nickname: result.nickname,
      email: "",
      password: "",
      portfolioPublic: true,
      createdAt: new Date().toISOString(),
    });
  }, []);

  const logout = useCallback(() => {
    setUser(null);
  }, []);

  const updateUser = useCallback(
    (updates: Partial<Pick<MockUser, "nickname" | "password" | "portfolioPublic">>) => {
      setUser((prev) => (prev ? { ...prev, ...updates } : prev));
    },
    [],
  );

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: user !== null, login, loginWithKakao, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
