import { SocialLoginButtons } from "@/components/auth/SocialLoginButtons";

export function LoginPage() {
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
            기록으로 배우는 코인 모의투자
          </p>
        </div>

        {/* Login card */}
        <div className="rounded-xl border border-border bg-card p-6">
          <SocialLoginButtons />
        </div>
      </div>
    </div>
  );
}
