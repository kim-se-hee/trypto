import { apiPost } from "./client";

/**
 * 백엔드에 로그아웃을 요청한다.
 * 소셜 provider 종류와 무관한 단일 엔드포인트로, 서버 세션을 무효화하고
 * 세션 쿠키를 만료시킨다. 세션이 이미 없어도 성공(멱등)한다.
 */
export function logout(): Promise<void> {
  return apiPost<void>("/api/auth/logout");
}
