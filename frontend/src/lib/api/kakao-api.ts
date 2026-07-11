import { apiPost } from "./client";

export interface KakaoLoginResponse {
  userId: number;
  nickname: string;
  newUser: boolean;
}

/**
 * 백엔드에 인가 코드 + PKCE 검증값을 넘겨 로그인한다.
 * 성공하면 백엔드가 세션 쿠키(Set-Cookie)를 내려주고 회원 정보를 반환한다.
 */
export function kakaoLogin(code: string, codeVerifier: string): Promise<KakaoLoginResponse> {
  return apiPost<KakaoLoginResponse>("/api/auth/kakao/login", { code, codeVerifier });
}
