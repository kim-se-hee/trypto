import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { socialLogin } from "@/lib/api/auth-api";
import { isApiClientError } from "@/lib/api/types";
import {
  beginSocialLogin,
  clearSocialSecrets,
  openSocialPopup,
  providerLabel,
  sendSocialPopup,
  subscribeSocialCallback,
  verifySocialCallback,
  type SocialCallbackMessage,
  type SocialProvider,
} from "@/lib/auth/social";

/** 사용자가 팝업을 그냥 닫았는지 확인하는 주기. 닫힘은 이벤트로 알 수 없어 들여다보는 수밖에 없다. */
const POPUP_POLL_INTERVAL_MS = 500;

export interface SocialLogin {
  /** 인가가 진행 중인 제공자. null 이면 진행 중이 아니다. */
  pendingProvider: SocialProvider | null;
  error: string;
  start: (provider: SocialProvider) => void;
}

/**
 * 소셜 로그인 진행 상태.
 *
 * 인가는 팝업에 맡기고 주 창은 제자리에 남는다. 팝업이 code 를 넘겨주면 주 창이 state 를 대조하고
 * 백엔드와 교환한다. 로그인에 성공하면 인증 상태가 바뀌므로 화면 이동은 PublicRoute 가 맡는다.
 * 팝업이 차단되면 주 창을 통째로 제공자에게 보내는 예전 방식으로 물러선다.
 */
export function useSocialLogin(): SocialLogin {
  const { loginWithSocial } = useAuth();
  const [pendingProvider, setPendingProvider] = useState<SocialProvider | null>(null);
  const [error, setError] = useState("");
  const popupRef = useRef<Window | null>(null);
  // 팝업이 결과를 넘기고 닫힌 것인지, 사용자가 그냥 닫은 것인지 가른다.
  const answeredRef = useRef(false);

  const finish = useCallback(
    async (message: SocialCallbackMessage) => {
      // 콜백은 한 번만 처리한다. 팝업이 StrictMode 로 결과를 두 번 발행하거나 다른 탭이
      // 같은 메시지를 중복 전달하면, 두 번째부터는 이미 state·verifier 를 지운 뒤라
      // 검증이 실패해 "보안 검증 실패" 가 순간 뜬다. 먼저 온 것만 받고 나머지는 버린다.
      if (answeredRef.current) return;
      answeredRef.current = true;
      popupRef.current?.close();
      popupRef.current = null;

      const result = verifySocialCallback(message);
      clearSocialSecrets();

      if (!result.ok) {
        setError(result.message);
        setPendingProvider(null);
        return;
      }

      try {
        const login = await socialLogin(result.provider, result.code, result.verifier);
        // pendingProvider 는 비우지 않는다. 인증 상태가 바뀌면 곧 이 화면이 사라지므로,
        // 그 사이에 버튼이 다시 눌리지 않게 잠가둔 채로 둔다.
        loginWithSocial({ userId: login.userId, nickname: login.nickname });
      } catch (e: unknown) {
        setError(isApiClientError(e) ? e.message : "로그인 처리 중 오류가 발생했습니다.");
        setPendingProvider(null);
      }
    },
    [loginWithSocial],
  );

  useEffect(() => subscribeSocialCallback((message) => void finish(message)), [finish]);

  // 사용자가 인가를 마치지 않고 팝업을 닫으면 버튼을 되돌려 다시 시도할 수 있게 한다.
  useEffect(() => {
    const popup = popupRef.current;
    if (!pendingProvider || !popup) return;

    const timer = window.setInterval(() => {
      if (!popup.closed) return;
      window.clearInterval(timer);
      if (answeredRef.current) return;

      popupRef.current = null;
      setPendingProvider(null);
    }, POPUP_POLL_INTERVAL_MS);

    return () => window.clearInterval(timer);
  }, [pendingProvider]);

  // 로그인을 마치거나 화면을 떠날 때 열어둔 팝업을 남기지 않는다.
  useEffect(() => () => popupRef.current?.close(), []);

  const start = useCallback((provider: SocialProvider) => {
    setError("");
    answeredRef.current = false;

    const popup = openSocialPopup();
    setPendingProvider(provider);

    if (!popup) {
      // 팝업이 차단됐다. 주 창을 통째로 보낸다. 성공하면 제공자로 떠나므로 이 아래는 실행되지 않는다.
      beginSocialLogin(provider).catch(() => {
        setError(`${providerLabel(provider)} 로그인 설정이 완료되지 않았습니다.`);
        setPendingProvider(null);
      });
      return;
    }

    popupRef.current = popup;
    sendSocialPopup(popup, provider).catch(() => {
      popup.close();
      popupRef.current = null;
      setError(`${providerLabel(provider)} 로그인 설정이 완료되지 않았습니다.`);
      setPendingProvider(null);
    });
  }, []);

  return { pendingProvider, error, start };
}
