import { generateCodeChallenge, generateCodeVerifier, generateState } from "./pkce";

/**
 * 카카오 인가 시작. PKCE 값과 state 를 sessionStorage 에 저장한 뒤 카카오 인가 URL 로 브라우저를 보낸다.
 * 콜백 페이지에서 state 를 대조하고 code_verifier 를 백엔드로 넘긴다.
 */

const CLIENT_ID = import.meta.env.VITE_KAKAO_CLIENT_ID as string | undefined;
const REDIRECT_URI = import.meta.env.VITE_KAKAO_REDIRECT_URI as string | undefined;
const AUTH_URL =
  (import.meta.env.VITE_KAKAO_AUTH_URL as string | undefined) ??
  "https://kauth.kakao.com/oauth/authorize";

export const KAKAO_VERIFIER_KEY = "kakao_code_verifier";
export const KAKAO_STATE_KEY = "kakao_oauth_state";

/** 환경변수(REST API 키·redirect URI)가 채워져 있는지 */
export function isKakaoConfigured(): boolean {
  return Boolean(CLIENT_ID && REDIRECT_URI);
}

export async function beginKakaoLogin(): Promise<void> {
  if (!CLIENT_ID || !REDIRECT_URI) {
    throw new Error(
      "카카오 로그인 환경변수(VITE_KAKAO_CLIENT_ID / VITE_KAKAO_REDIRECT_URI)가 설정되지 않았습니다.",
    );
  }

  const verifier = generateCodeVerifier();
  const challenge = await generateCodeChallenge(verifier);
  const state = generateState();

  sessionStorage.setItem(KAKAO_VERIFIER_KEY, verifier);
  sessionStorage.setItem(KAKAO_STATE_KEY, state);

  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    response_type: "code",
    state,
    code_challenge: challenge,
    code_challenge_method: "S256",
  });

  window.location.href = `${AUTH_URL}?${params.toString()}`;
}
