import { generateCodeChallenge, generateCodeVerifier, generateState } from "./pkce";

/**
 * 소셜 인가 시작. PKCE 값과 state 를 sessionStorage 에 저장한 뒤 제공자 인가 URL 로 브라우저를 보낸다.
 * 콜백 페이지에서 state 를 대조하고 code_verifier 를 백엔드로 넘긴다.
 * 제공자별로 다른 것은 인가 URL·클라이언트 ID·추가 파라미터뿐이고 절차는 동일하다.
 */

export type SocialProvider = "kakao" | "google";

export const OAUTH_VERIFIER_KEY = "oauth_code_verifier";
export const OAUTH_STATE_KEY = "oauth_state";

interface ProviderConfig {
  /** 사용자에게 보여줄 이름 */
  label: string;
  clientId: string | undefined;
  redirectUri: string | undefined;
  authUrl: string;
  /** 인가 URL 에 추가로 붙는 제공자 고유 파라미터 */
  extraParams?: Record<string, string>;
}

const PROVIDER_CONFIGS: Record<SocialProvider, ProviderConfig> = {
  kakao: {
    label: "카카오",
    clientId: import.meta.env.VITE_KAKAO_CLIENT_ID as string | undefined,
    redirectUri: import.meta.env.VITE_KAKAO_REDIRECT_URI as string | undefined,
    authUrl:
      (import.meta.env.VITE_KAKAO_AUTH_URL as string | undefined) ??
      "https://kauth.kakao.com/oauth/authorize",
  },
  google: {
    label: "구글",
    clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined,
    redirectUri: import.meta.env.VITE_GOOGLE_REDIRECT_URI as string | undefined,
    authUrl:
      (import.meta.env.VITE_GOOGLE_AUTH_URL as string | undefined) ??
      "https://accounts.google.com/o/oauth2/v2/auth",
    extraParams: { scope: "openid" },
  },
};

export function isSocialProvider(value: string | undefined): value is SocialProvider {
  return value === "kakao" || value === "google";
}

export function providerLabel(provider: SocialProvider): string {
  return PROVIDER_CONFIGS[provider].label;
}

/** 해당 제공자의 환경변수(클라이언트 ID·redirect URI)가 채워져 있는지 */
export function isSocialConfigured(provider: SocialProvider): boolean {
  const config = PROVIDER_CONFIGS[provider];
  return Boolean(config.clientId && config.redirectUri);
}

export async function beginSocialLogin(provider: SocialProvider): Promise<void> {
  const config = PROVIDER_CONFIGS[provider];
  if (!config.clientId || !config.redirectUri) {
    throw new Error(`${config.label} 로그인 환경변수(클라이언트 ID / redirect URI)가 설정되지 않았습니다.`);
  }

  const verifier = generateCodeVerifier();
  const challenge = await generateCodeChallenge(verifier);
  const state = generateState();

  sessionStorage.setItem(OAUTH_VERIFIER_KEY, verifier);
  sessionStorage.setItem(OAUTH_STATE_KEY, state);

  const params = new URLSearchParams({
    client_id: config.clientId,
    redirect_uri: config.redirectUri,
    response_type: "code",
    state,
    code_challenge: challenge,
    code_challenge_method: "S256",
    ...config.extraParams,
  });

  window.location.href = `${config.authUrl}?${params.toString()}`;
}
