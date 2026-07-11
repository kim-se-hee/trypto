import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Activity, Loader2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { kakaoLogin } from "@/lib/api/kakao-api";
import { KAKAO_STATE_KEY, KAKAO_VERIFIER_KEY } from "@/lib/auth/kakao";
import { isApiClientError } from "@/lib/api/types";

/**
 * 카카오 인가 콜백. 카카오가 ?code=&state= 를 붙여 되돌린 페이지.
 * state 대조 → 백엔드 로그인 호출 → 성공 시 /market 이동.
 */
export function KakaoCallbackPage() {
  const navigate = useNavigate();
  const { loginWithKakao } = useAuth();
  const [error, setError] = useState<string | null>(null);
  // 인가 코드는 일회용이라 StrictMode 이중 실행 시 두 번째 교환이 실패한다. ref 로 한 번만 실행.
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    const params = new URLSearchParams(window.location.search);
    const errorParam = params.get("error");
    const code = params.get("code");
    const returnedState = params.get("state");

    const savedState = sessionStorage.getItem(KAKAO_STATE_KEY);
    const verifier = sessionStorage.getItem(KAKAO_VERIFIER_KEY);
    sessionStorage.removeItem(KAKAO_STATE_KEY);
    sessionStorage.removeItem(KAKAO_VERIFIER_KEY);

    if (errorParam) {
      setError("카카오 로그인이 취소되었거나 실패했습니다.");
      return;
    }
    if (!code || !returnedState) {
      setError("인가 정보가 올바르지 않습니다.");
      return;
    }
    if (!savedState || savedState !== returnedState) {
      setError("보안 검증(state)에 실패했습니다. 다시 시도해주세요.");
      return;
    }
    if (!verifier) {
      setError("로그인 검증값이 없습니다. 다시 시도해주세요.");
      return;
    }

    kakaoLogin(code, verifier)
      .then((result) => {
        loginWithKakao({ userId: result.userId, nickname: result.nickname });
        navigate("/market", { replace: true });
      })
      .catch((e: unknown) => {
        setError(
          isApiClientError(e) ? e.message : "로그인 처리 중 오류가 발생했습니다.",
        );
      });
  }, [navigate, loginWithKakao]);

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
            <p className="text-sm">카카오 로그인 처리 중…</p>
          </div>
        )}
      </div>
    </div>
  );
}
