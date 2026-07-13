import { apiPost } from "./client";
import type { SocialProvider } from "@/lib/auth/social";

export interface SocialLoginResponse {
  userId: number;
  nickname: string;
  newUser: boolean;
}

/**
 * 백엔드에 인가 코드 + PKCE 검증값을 넘겨 로그인한다.
 * 성공하면 백엔드가 세션 쿠키(Set-Cookie)를 내려주고 회원 정보를 반환한다.
 */
export function socialLogin(
  provider: SocialProvider,
  code: string,
  codeVerifier: string,
): Promise<SocialLoginResponse> {
  return apiPost<SocialLoginResponse>(`/api/auth/${provider}/login`, { code, codeVerifier });
}

/**
 * 백엔드에 로그아웃을 요청한다.
 * 소셜 provider 종류와 무관한 단일 엔드포인트로, 서버 세션을 무효화하고
 * 세션 쿠키를 만료시킨다. 세션이 이미 없어도 성공(멱등)한다.
 */
export function logout(): Promise<void> {
  return apiPost<void>("/api/auth/logout");
}
