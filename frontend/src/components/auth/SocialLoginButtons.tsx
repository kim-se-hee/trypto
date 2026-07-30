import { useSocialLogin } from "@/hooks/useSocialLogin";
import { isSocialConfigured } from "@/lib/auth/social";

const IS_DEV = import.meta.env.DEV;

/**
 * 카카오·구글 로그인 버튼 묶음.
 *
 * 로그인 화면과 로그인 유도 모달이 같은 버튼을 쓴다. 인가는 팝업이 다녀오고 주 창은 제자리에
 * 남으므로, 이 묶음을 어디에 놓든 로그인을 마친 사용자는 보던 자리에 그대로 있게 된다.
 */
export function SocialLoginButtons({ onSuccess }: { onSuccess?: () => void }) {
  const { pendingProvider, error, start } = useSocialLogin({ onSuccess });

  const kakaoReady = isSocialConfigured("kakao");
  const googleReady = isSocialConfigured("google");

  return (
    <div>
      <button
        type="button"
        onClick={() => start("kakao")}
        disabled={!kakaoReady || pendingProvider !== null}
        className="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-[#FEE500] text-sm font-semibold text-[#191600] transition-all duration-150 hover:brightness-95 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
      >
        <svg className="h-4 w-4" viewBox="0 0 24 24" fill="#191600" aria-hidden="true">
          <path d="M12 3C6.477 3 2 6.463 2 10.735c0 2.724 1.822 5.116 4.575 6.485-.202.735-.732 2.664-.838 3.077-.13.513.188.506.396.368.163-.108 2.596-1.762 3.65-2.48.717.106 1.46.162 2.217.162 5.523 0 10-3.463 10-7.735C22 6.463 17.523 3 12 3Z" />
        </svg>
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
  );
}
