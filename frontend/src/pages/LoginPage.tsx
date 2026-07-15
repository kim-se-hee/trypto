import { useEffect } from "react";
import { MessageCircle } from "lucide-react";
import { useSocialLogin } from "@/hooks/useSocialLogin";
import { isSocialConfigured } from "@/lib/auth/social";

const IS_DEV = import.meta.env.DEV;

export function LoginPage() {
  const { pendingProvider, error, start } = useSocialLogin();

  const kakaoReady = isSocialConfigured("kakao");
  const googleReady = isSocialConfigured("google");

  // 팝업이 차단되어 주 창이 제공자를 다녀오는 경우에만 해당한다. 그렇게 떠난 화면이 뒤로 가기로
  // 되살아나면(bfcache) "로그인 중…"에 멈춘 버튼과 로그인 여부를 모르는 인증 상태를 그대로 들고
  // 온다. 새로 부팅시켜 둘 다 다시 정하게 한다. 팝업으로 로그인하면 이 화면은 떠나지 않는다.
  useEffect(() => {
    function handlePageShow(event: PageTransitionEvent) {
      if (event.persisted) window.location.reload();
    }

    window.addEventListener("pageshow", handlePageShow);
    return () => window.removeEventListener("pageshow", handlePageShow);
  }, []);

  return (
    <div className="flex min-h-dvh items-center justify-center bg-background px-4">
      <div className="w-full max-w-[380px] animate-enter">
        {/* Logo */}
        <div className="mb-10 text-center">
          <div className="inline-flex items-center gap-2.5">
            <img src="/favicon.png" alt="trypto" className="h-9 w-9 rounded-xl" />
            <span className="text-2xl font-extrabold tracking-tight">trypto</span>
          </div>
          <p className="mt-3 text-sm text-muted-foreground">
            큰 돈 잃을 걱정 없이 해보는 실전 리허설
          </p>
        </div>

        {/* Login card */}
        <div className="rounded-xl border border-border bg-card p-6">
          {/* 소셜 로그인 (주 로그인) */}
          <button
            type="button"
            onClick={() => start("kakao")}
            disabled={!kakaoReady || pendingProvider !== null}
            className="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-[#FEE500] text-sm font-semibold text-[#191600] transition-all duration-150 hover:brightness-95 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
          >
            <MessageCircle className="h-4 w-4 fill-current" />
            {pendingProvider === "kakao" ? "카카오로 로그인 중…" : "카카오로 로그인"}
          </button>
          <button
            type="button"
            onClick={() => start("google")}
            disabled={!googleReady || pendingProvider !== null}
            className="mt-3 flex h-12 w-full items-center justify-center gap-2 rounded-lg border border-border bg-white text-sm font-semibold text-[#1f1f1f] transition-all duration-150 hover:bg-neutral-50 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
          >
            <svg className="h-4 w-4" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="#4285F4"
                d="M23.52 12.27c0-.85-.08-1.67-.22-2.45H12v4.64h6.46a5.52 5.52 0 0 1-2.4 3.62v3h3.88c2.27-2.09 3.58-5.17 3.58-8.81Z"
              />
              <path
                fill="#34A853"
                d="M12 24c3.24 0 5.96-1.07 7.94-2.91l-3.88-3.01c-1.07.72-2.45 1.15-4.06 1.15-3.13 0-5.78-2.11-6.72-4.95H1.27v3.11A12 12 0 0 0 12 24Z"
              />
              <path
                fill="#FBBC05"
                d="M5.28 14.28a7.21 7.21 0 0 1 0-4.56V6.61H1.27a12 12 0 0 0 0 10.78l4.01-3.11Z"
              />
              <path
                fill="#EA4335"
                d="M12 4.77c1.76 0 3.34.61 4.59 1.8l3.44-3.44C17.95 1.19 15.24 0 12 0A12 12 0 0 0 1.27 6.61l4.01 3.11C6.22 6.88 8.87 4.77 12 4.77Z"
              />
            </svg>
            {pendingProvider === "google" ? "구글로 로그인 중…" : "구글로 로그인"}
          </button>
          {IS_DEV && (!kakaoReady || !googleReady) && (
            <p className="mt-2 text-center text-xs text-muted-foreground">
              {[!kakaoReady && "카카오", !googleReady && "구글"].filter(Boolean).join("·")} 로그인
              설정(<span className="font-mono">.env.local</span>)이 필요합니다.
            </p>
          )}

          {error && (
            <p className="mt-3 rounded-lg bg-destructive/8 px-3 py-2 text-xs font-medium text-destructive">
              {error}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
