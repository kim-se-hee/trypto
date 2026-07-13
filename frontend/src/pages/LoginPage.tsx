import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Activity, ArrowRight, MessageCircle } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { Input } from "@/components/ui/input";
import {
  beginSocialLogin,
  isSocialConfigured,
  providerLabel,
  type SocialProvider,
} from "@/lib/auth/social";

/** 목 이메일 로그인은 개발 환경 전용이다. 프로덕션 빌드에서는 이 블록 전체가 제거된다. */
const IS_DEV = import.meta.env.DEV;

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loadingProvider, setLoadingProvider] = useState<SocialProvider | null>(null);

  const kakaoReady = isSocialConfigured("kakao");
  const googleReady = isSocialConfigured("google");

  async function handleSocialLogin(provider: SocialProvider) {
    setError("");
    setLoadingProvider(provider);
    try {
      await beginSocialLogin(provider);
      // 성공 시 제공자로 리다이렉트되므로 이 아래는 실행되지 않는다.
    } catch {
      setError(`${providerLabel(provider)} 로그인 설정이 완료되지 않았습니다.`);
      setLoadingProvider(null);
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");

    if (!email.trim()) {
      setError("이메일을 입력해주세요.");
      return;
    }

    const success = login(email.trim());
    if (success) {
      navigate("/market", { replace: true });
    } else {
      setError("등록되지 않은 이메일입니다.");
    }
  }

  return (
    <div className="flex min-h-dvh items-center justify-center bg-background px-4">
      <div className="w-full max-w-[380px] animate-enter">
        {/* Logo */}
        <div className="mb-10 text-center">
          <div className="inline-flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary">
              <Activity className="h-4.5 w-4.5 text-white" />
            </div>
            <span className="text-2xl font-extrabold tracking-tight">Trypto</span>
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
            onClick={() => handleSocialLogin("kakao")}
            disabled={!kakaoReady || loadingProvider !== null}
            className="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-[#FEE500] text-sm font-semibold text-[#191600] transition-all duration-150 hover:brightness-95 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
          >
            <MessageCircle className="h-4 w-4 fill-current" />
            {loadingProvider === "kakao" ? "카카오로 이동 중…" : "카카오로 로그인"}
          </button>
          <button
            type="button"
            onClick={() => handleSocialLogin("google")}
            disabled={!googleReady || loadingProvider !== null}
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
            {loadingProvider === "google" ? "구글로 이동 중…" : "구글로 로그인"}
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

          {IS_DEV && (
            <>
              {/* 구분선 */}
              <div className="my-5 flex items-center gap-3">
                <div className="h-px flex-1 bg-border" />
                <span className="text-xs text-muted-foreground">개발용 로그인</span>
                <div className="h-px flex-1 bg-border" />
              </div>

              {/* 목 이메일 로그인 (개발용) */}
              <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="email" className="text-xs font-medium text-muted-foreground">
                    이메일
                  </label>
                  <Input
                    id="email"
                    type="email"
                    placeholder="test@trypto.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    autoComplete="email"
                    className="h-11 rounded-lg bg-secondary/40 text-sm"
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label htmlFor="password" className="text-xs font-medium text-muted-foreground">
                    비밀번호
                  </label>
                  <Input
                    id="password"
                    type="password"
                    placeholder="아무 값이나 입력"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    autoComplete="current-password"
                    className="h-11 rounded-lg bg-secondary/40 text-sm"
                  />
                </div>

                <button
                  type="submit"
                  className="mt-1 flex h-11 items-center justify-center gap-2 rounded-lg bg-primary text-sm font-semibold text-white transition-all duration-150 hover:bg-primary/90 active:scale-[0.98]"
                >
                  로그인
                  <ArrowRight className="h-4 w-4" />
                </button>
              </form>
            </>
          )}
        </div>

        {/* Test account hint (개발용) */}
        {IS_DEV && (
          <div className="mt-4 rounded-lg border border-dashed border-border px-4 py-3 text-center">
            <p className="text-xs text-muted-foreground">
              테스트 계정 · <span className="font-medium text-foreground">test@trypto.com</span>
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
