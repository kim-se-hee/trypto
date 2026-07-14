import { createContext, useContext } from "react";
import type { AuthUser } from "@/lib/types/user";

// 컴포넌트(AuthProvider)는 같은 파일에 두지 않는다. 컴포넌트와 훅을 한 파일에서 함께 내보내면
// 개발 중 파일을 고칠 때마다 화면이 통째로 새로고침되어 상태가 날아간다.

export interface SocialLoginUser {
  userId: number;
  nickname: string;
}

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isAuthLoading: boolean;
  loginWithSocial: (result: SocialLoginUser) => void;
  logout: () => Promise<void>;
  updateUser: (updates: Partial<Pick<AuthUser, "nickname">>) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
