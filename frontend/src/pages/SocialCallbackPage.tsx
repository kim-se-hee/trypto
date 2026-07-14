import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Activity, Loader2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { socialLogin } from "@/lib/api/auth-api";
import {
  OAUTH_STATE_KEY,
  OAUTH_VERIFIER_KEY,
  isSocialProvider,
  providerLabel,
  type SocialProvider,
} from "@/lib/auth/social";
import { isApiClientError } from "@/lib/api/types";

type AuthAttempt =
  | { ok: true; provider: SocialProvider; code: string; verifier: string }
  | { ok: false; message: string };

/**
 * 되돌아온 인가 정보를 검사한다. URL 과 세션 저장소를 읽기만 하므로 렌더 중에 불러도 안전하다.
 * 세션 값을 지우는 것은 부수 효과라 이펙트에서 따로 한다.
 */
function readAuthAttempt(provider: string | undefined): AuthAttempt {
  if (!isSocialProvider(provider)) {
    return { ok: false, message: "지원하지 않는 소셜 제공자입니다." };
  }

  const params = new URLSearchParams(window.location.search);
  const code = params.get("code");
  const returnedState = params.get("state");
  const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);
  const verifier = sessionStorage.getItem(OAUTH_VERIFIER_KEY);

  if (params.get("error")) {
    return { ok: false, message: `${providerLabel(provider)} 로그인이 취소되었거나 실패했습니다.` };
  }
  if (!code || !returnedState) {
    return { ok: false, message: "인가 정보가 올바르지 않습니다." };
  }
  if (!savedState || savedState !== returnedState) {
    return { ok: false, message: "보안 검증(state)에 실패했습니다. 다시 시도해주세요." };
  }
  if (!verifier) {
    return { ok: false, message: "로그인 검증값이 없습니다. 다시 시도해주세요." };
  }

  return { ok: true, provider, code, verifier };
}

/**
 * 소셜 인가 콜백. 제공자가 ?code=&state= 를 붙여 되돌린 페이지 (/auth/:provider/callback).
 * state 대조 → 백엔드 로그인 호출 → 성공 시 /market 이동.
 */
export function SocialCallbackPage() {
  const navigate = useNavigate();
  const { provider } = useParams();
  const { loginWithSocial } = useAuth();
  // 검증은 첫 렌더에 한 번만 한다. 읽기만 하므로 StrictMode 가 두 번 불러도 결과가 같다.
  const [attempt] = useState(() => readAuthAttempt(provider));
  const [submitError, setSubmitError] = useState<string | null>(null);
  // 인가 코드는 일회용이라 StrictMode 이중 실행 시 두 번째 교환이 실패한다. ref 로 한 번만 실행.
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    // 한 번 쓴 state·verifier 는 성공이든 실패든 남겨두지 않는다.
    sessionStorage.removeItem(OAUTH_STATE_KEY);
    sessionStorage.removeItem(OAUTH_VERIFIER_KEY);

    if (!attempt.ok) return;

    socialLogin(attempt.provider, attempt.code, attempt.verifier)
      .then((result) => {
        loginWithSocial({ userId: result.userId, nickname: result.nickname });
        navigate("/market", { replace: true });
      })
      .catch((e: unknown) => {
        setSubmitError(
          isApiClientError(e) ? e.message : "로그인 처리 중 오류가 발생했습니다.",
        );
      });
  }, [attempt, navigate, loginWithSocial]);

  const error = attempt.ok ? submitError : attempt.message;

  return (
    <div className="flex min-h-dvh items-center justify-center bg-background px-4">
      <div className="w-full max-w-[380px] text-center">
        <div className="mb-8 inline-flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary">
            <Activity className="h-4.5 w-4.5 text-white" />
          </div>
          <span className="text-2xl font-extrabold tracking-tight">Trypto</span>
        </div>

        {error ? (
          <div className="rounded-xl border border-border bg-card p-6">
            <p className="text-sm font-medium text-destructive">{error}</p>
            <button
              type="button"
              onClick={() => navigate("/login", { replace: true })}
              className="mt-4 h-10 w-full rounded-lg bg-primary text-sm font-semibold text-white transition-all duration-150 hover:bg-primary/90 active:scale-[0.98]"
            >
              로그인 화면으로 돌아가기
            </button>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-3 text-muted-foreground">
            <Loader2 className="h-6 w-6 animate-spin" />
            <p className="text-sm">
              {isSocialProvider(provider) ? `${providerLabel(provider)} 로그인 처리 중…` : "로그인 처리 중…"}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
