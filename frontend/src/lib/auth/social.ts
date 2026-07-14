import { generateCodeChallenge, generateCodeVerifier, generateState } from "./pkce";

/**
 * 소셜 인가 시작. PKCE 값과 state 를 sessionStorage 에 저장한 뒤 제공자 인가 화면을 띄운다.
 *
 * 인가 화면은 팝업으로 띄우는 것이 기본이다. 주 창을 통째로 제공자에게 보내면 제공자의 로그인·동의
 * 페이지가 주 창의 히스토리에 쌓이는데, 다른 출처가 남긴 히스토리 항목은 우리가 지울 수 없다.
 * 로그인을 마친 뒤 뒤로 가기를 누르면 카카오 화면이 나오는 이유가 이것이다. 팝업에서 왕복하면
 * 주 창의 문서와 히스토리는 손대지 않은 채로 남는다.
 *
 * 팝업이 차단되면 주 창을 보내는 방식(beginSocialLogin)으로 물러선다.
 */

export type SocialProvider = "kakao" | "google";

export const OAUTH_VERIFIER_KEY = "oauth_code_verifier";
export const OAUTH_STATE_KEY = "oauth_state";

/** 콜백 페이지가 팝업 안인지 판별하는 표시. 팝업을 열기 직전에 남긴다. */
export const OAUTH_POPUP_KEY = "oauth_popup";

const OAUTH_CHANNEL_NAME = "trypto-social-callback";
const POPUP_NAME = "trypto-social-login";
const POPUP_WIDTH = 480;
const POPUP_HEIGHT = 640;

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

/** 팝업이 주 창에 넘기는 인가 결과. 검증은 하지 않고 받아온 그대로 넘긴다. */
export interface SocialCallbackMessage {
  provider: SocialProvider;
  code: string | null;
  state: string | null;
  error: string | null;
}

export type SocialCallbackResult =
  | { ok: true; provider: SocialProvider; code: string; verifier: string }
  | { ok: false; message: string };

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

function isSocialCallbackMessage(value: unknown): value is SocialCallbackMessage {
  if (typeof value !== "object" || value === null) return false;
  const message = value as Record<string, unknown>;
  return typeof message.provider === "string" && isSocialProvider(message.provider);
}

async function buildAuthUrl(provider: SocialProvider): Promise<string> {
  const config = PROVIDER_CONFIGS[provider];
  if (!config.clientId || !config.redirectUri) {
    throw new Error(`${config.label} 로그인 환경변수(클라이언트 ID / redirect URI)가 설정되지 않았습니다.`);
  }

  const verifier = generateCodeVerifier();
  const challenge = await generateCodeChallenge(verifier);
  const state = generateState();

  // 팝업이 아니라 주 창에 저장한다. 팝업은 결과를 전달만 하고, 검증과 교환은 주 창이 한다.
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

  return `${config.authUrl}?${params.toString()}`;
}

/**
 * 빈 팝업을 연다. 차단되면 null.
 *
 * 인가 URL 을 만들려면 PKCE 해시 계산을 기다려야 하는데, 그 사이에 창을 열면 브라우저가 클릭과
 * 무관한 팝업으로 보고 막는다. 그래서 클릭 처리 안에서 빈 창을 먼저 열고 주소는 나중에 넣는다.
 */
export function openSocialPopup(): Window | null {
  const left = window.screenX + Math.max(0, (window.outerWidth - POPUP_WIDTH) / 2);
  const top = window.screenY + Math.max(0, (window.outerHeight - POPUP_HEIGHT) / 2);

  // 세션 저장소는 창이 열리는 시점에 복사되므로 표시를 먼저 남긴다.
  // 주 창에 남겨두면 팝업이 막혀 주 창이 콜백을 받을 때 자기를 팝업으로 착각하므로 곧바로 지운다.
  sessionStorage.setItem(OAUTH_POPUP_KEY, "1");
  const popup = window.open(
    "about:blank",
    POPUP_NAME,
    `popup=yes,width=${POPUP_WIDTH},height=${POPUP_HEIGHT},left=${Math.round(left)},top=${Math.round(top)}`,
  );
  sessionStorage.removeItem(OAUTH_POPUP_KEY);

  return popup;
}

/** 열어둔 팝업을 제공자 인가 화면으로 보낸다. */
export async function sendSocialPopup(popup: Window, provider: SocialProvider): Promise<void> {
  popup.location.href = await buildAuthUrl(provider);
}

/** 팝업이 막혔을 때의 폴백. 주 창을 통째로 제공자에게 보낸다. */
export async function beginSocialLogin(provider: SocialProvider): Promise<void> {
  window.location.replace(await buildAuthUrl(provider));
}

/**
 * 팝업이 받아온 인가 결과를 주 창으로 보낸다.
 *
 * opener 로 직접 보내지 않는다. 제공자 페이지가 COOP 헤더로 창 사이의 연결을 끊어버리면
 * 팝업에서 opener 가 null 이 되기 때문이다. BroadcastChannel 은 창 관계와 무관하게
 * 같은 출처끼리만 오가므로, 연결이 끊겨도 결과가 전달되고 다른 출처는 들을 수 없다.
 */
export function publishSocialCallback(message: SocialCallbackMessage): void {
  const channel = new BroadcastChannel(OAUTH_CHANNEL_NAME);
  channel.postMessage(message);
  channel.close();
}

/** 주 창에서 팝업이 보내는 인가 결과를 듣는다. 반환한 함수를 부르면 구독을 끊는다. */
export function subscribeSocialCallback(
  listener: (message: SocialCallbackMessage) => void,
): () => void {
  const channel = new BroadcastChannel(OAUTH_CHANNEL_NAME);
  channel.onmessage = (event: MessageEvent<unknown>) => {
    if (isSocialCallbackMessage(event.data)) listener(event.data);
  };
  return () => channel.close();
}

/** 콜백 URL 의 질의 문자열에서 인가 결과를 읽는다. */
export function readSocialCallbackParams(
  provider: SocialProvider,
  search: string,
): SocialCallbackMessage {
  const params = new URLSearchParams(search);
  return {
    provider,
    code: params.get("code"),
    state: params.get("state"),
    error: params.get("error"),
  };
}

/**
 * 되돌아온 인가 결과를 저장해둔 state·verifier 와 대조한다.
 * 읽기만 하므로 렌더 중에 불러도 안전하다. 쓴 값을 지우는 것은 clearSocialSecrets 로 따로 한다.
 */
export function verifySocialCallback(message: SocialCallbackMessage): SocialCallbackResult {
  const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);
  const verifier = sessionStorage.getItem(OAUTH_VERIFIER_KEY);

  if (message.error) {
    return {
      ok: false,
      message: `${providerLabel(message.provider)} 로그인이 취소되었거나 실패했습니다.`,
    };
  }
  if (!message.code || !message.state) {
    return { ok: false, message: "인가 정보가 올바르지 않습니다." };
  }
  if (!savedState || savedState !== message.state) {
    return { ok: false, message: "보안 검증(state)에 실패했습니다. 다시 시도해주세요." };
  }
  if (!verifier) {
    return { ok: false, message: "로그인 검증값이 없습니다. 다시 시도해주세요." };
  }

  return { ok: true, provider: message.provider, code: message.code, verifier };
}

/** 한 번 쓴 state·verifier 는 성공이든 실패든 남겨두지 않는다. */
export function clearSocialSecrets(): void {
  sessionStorage.removeItem(OAUTH_STATE_KEY);
  sessionStorage.removeItem(OAUTH_VERIFIER_KEY);
}
