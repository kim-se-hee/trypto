import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Activity, ArrowRight } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { Input } from "@/components/ui/input";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

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
    <div className="relative flex min-h-dvh items-center justify-center overflow-hidden bg-background px-4">
      {/* Background decorations */}
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute -left-32 -top-32 h-96 w-96 rounded-full bg-primary/6 blur-3xl" />
        <div className="absolute -bottom-24 -right-24 h-80 w-80 rounded-full bg-chart-2/8 blur-3xl" />
        <div className="absolute left-1/2 top-1/3 h-64 w-64 -translate-x-1/2 rounded-full bg-chart-4/5 blur-3xl" />
      </div>

      <div className="relative w-full max-w-[380px]">
        {/* Logo */}
        <div className="mb-8 text-center">
          <div className="inline-flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-[#9A6AFF] shadow-md">
              <Activity className="h-5 w-5 text-white" />
            </div>
            <span className="text-2xl font-extrabold tracking-tight">Trypto</span>
          </div>
          <p className="mt-2 text-sm font-medium text-muted-foreground">
            큰 돈 잃을 걱정 없이 해보는 실전 리허설
          </p>
        </div>

        {/* Login card */}
        <div className="rounded-2xl bg-card p-6 shadow-card">
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="email" className="text-xs font-semibold text-muted-foreground">
                이메일
              </label>
              <Input
                id="email"
                type="email"
                placeholder="test@trypto.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                className="h-11 rounded-xl bg-secondary/40 text-sm"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="password" className="text-xs font-semibold text-muted-foreground">
                비밀번호
              </label>
              <Input
                id="password"
                type="password"
                placeholder="아무 값이나 입력"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                className="h-11 rounded-xl bg-secondary/40 text-sm"
              />
            </div>

            {error && (
              <p className="rounded-lg bg-destructive/8 px-3 py-2 text-xs font-medium text-destructive">
                {error}
              </p>
            )}

            <button
              type="submit"
              className="mt-1 flex h-11 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-primary to-[#9A6AFF] text-sm font-bold text-white shadow-md transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg active:translate-y-0"
            >
              로그인
              <ArrowRight className="h-4 w-4" />
            </button>
          </form>
        </div>

        {/* Test account hint */}
        <div className="mt-4 rounded-xl bg-card/60 px-4 py-3 text-center backdrop-blur-sm">
          <p className="text-xs text-muted-foreground">
            테스트 계정 &middot;{" "}
            <span className="font-semibold text-foreground">test@trypto.com</span>
          </p>
        </div>
      </div>
    </div>
  );
}
