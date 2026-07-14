import { useEffect, useRef, useState, type ReactNode } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Activity, Loader2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { socialLogin } from "@/lib/api/auth-api";
import {
  OAUTH_POPUP_KEY,
  clearSocialSecrets,
  isSocialProvider,
  providerLabel,
  publishSocialCallback,
  readSocialCallbackParams,
  verifySocialCallback,
  type SocialProvider,
} from "@/lib/auth/social";
import { isApiClientError } from "@/lib/api/types";

/**
 * 소셜 인가 콜백. 제공자가 ?code=&state= 를 붙여 되돌린 페이지 (/auth/:provider/callback).
 *
 * 이 페이지가 열리는 창은 둘 중 하나다.
 * - 팝업: 결과를 주 창에 넘기고 스스로 닫는다. 검증도 교환도 하지 않는다 (state·verifier 는 주 창에 있다).
 * - 주 창: 팝업이 차단되어 주 창이 제공자를 다녀온 경우. 여기서 검증하고 교환한다.
 */
export function SocialCallbackPage() {
  const { provider } = useParams();
  const [isPopup] = useState(isPopupWindow);

  if (!isSocialProvider(provider)) {
    return (
      <CallbackShell>
        <CallbackError message="지원하지 않는 소셜 제공자입니다." />
      </CallbackShell>
    );
  }

  return (
    <CallbackShell>
      {isPopup ? <PopupRelay provider={provider} /> : <MainWindowExchange provider={provider} />}
    </CallbackShell>
  );
}

/**
 * 이 창이 인가용 팝업인지 판별한다.
 *
 * 팝업은 열릴 때 주 창의 세션 저장소를 복사해 오므로 열기 직전에 남긴 표시가 들어 있다.
 * 제공자 페이지가 COOP 헤더로 창 사이의 연결을 끊어버리면 window.opener 가 null 이 되므로
 * 그것만으로는 판별할 수 없다. 표시를 먼저 보고, 없으면 opener 로 확인한다.
 */
function isPopupWindow(): boolean {
  if (sessionStorage.getItem(OAUTH_POPUP_KEY) === "1") return true;
  return window.opener !== null && window.opener !== window;
}

/** 팝업. 받아온 인가 결과를 주 창에 넘기고 닫는다. */
function PopupRelay({ provider }: { provider: SocialProvider }) {
  useEffect(() => {
    sessionStorage.removeItem(OAUTH_POPUP_KEY);
    publishSocialCallback(readSocialCallbackParams(provider, window.location.search));
    window.close();
  }, [provider]);

  // 창을 닫는 데 실패하더라도 사용자가 막히지 않도록 안내를 남긴다.
  return (
    <div className="flex flex-col items-center gap-3 text-muted-foreground">
      <Loader2 className="h-6 w-6 animate-spin" />
      <p className="text-sm">로그인 처리 중… 이 창은 닫으셔도 됩니다.</p>
    </div>
  );
}

/** 주 창. 팝업이 차단되어 주 창이 제공자를 다녀온 경우로, 검증과 교환을 직접 한다. */
function MainWindowExchange({ provider }: { provider: SocialProvider }) {
  const navigate = useNavigate();
  const { loginWithSocial } = useAuth();
  // 검증은 첫 렌더에 한 번만 한다. 읽기만 하므로 StrictMode 가 두 번 불러도 결과가 같다.
  const [attempt] = useState(() =>
    verifySocialCallback(readSocialCallbackParams(provider, window.location.search)),
  );
  const [submitError, setSubmitError] = useState<string | null>(null);
  // 인가 코드는 일회용이라 StrictMode 이중 실행 시 두 번째 교환이 실패한다. ref 로 한 번만 실행.
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    clearSocialSecrets();
    if (!attempt.ok) return;

    socialLogin(attempt.provider, attempt.code, attempt.verifier)
      .then((result) => {
        loginWithSocial({ userId: result.userId, nickname: result.nickname });
        navigate("/market", { replace: true });
      })
      .catch((e: unknown) => {
        setSubmitError(isApiClientError(e) ? e.message : "로그인 처리 중 오류가 발생했습니다.");
      });
  }, [attempt, navigate, loginWithSocial]);

  const error = attempt.ok ? submitError : attempt.message;
  if (error) return <CallbackError message={error} />;

  return (
    <div className="flex flex-col items-center gap-3 text-muted-foreground">
      <Loader2 className="h-6 w-6 animate-spin" />
      <p className="text-sm">{providerLabel(provider)} 로그인 처리 중…</p>
    </div>
  );
}

function CallbackError({ message }: { message: string }) {
  const navigate = useNavigate();

  return (
    <div className="rounded-xl border border-border bg-card p-6">
      <p className="text-sm font-medium text-destructive">{message}</p>
      <button
        type="button"
        onClick={() => navigate("/login", { replace: true })}
        className="mt-4 h-10 w-full rounded-lg bg-primary text-sm font-semibold text-white transition-all duration-150 hover:bg-primary/90 active:scale-[0.98]"
      >
        로그인 화면으로 돌아가기
      </button>
    </div>
  );
}

function CallbackShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-dvh items-center justify-center bg-background px-4">
      <div className="w-full max-w-[380px] text-center">
        <div className="mb-8 inline-flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary">
            <Activity className="h-4.5 w-4.5 text-white" />
          </div>
          <span className="text-2xl font-extrabold tracking-tight">Trypto</span>
        </div>
        {children}
      </div>
    </div>
  );
}
