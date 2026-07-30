import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { LoginPromptDialog } from "@/components/auth/LoginPromptDialog";
import { useAuth } from "@/contexts/AuthContext";
import { LoginPromptContext } from "./LoginPromptContext";

const DEFAULT_REASON = "이 기능은 로그인한 뒤에 이용할 수 있습니다.";

/**
 * 로그인이 필요한 순간에 띄우는 모달을 화면 어디서나 부를 수 있게 한다.
 *
 * 로그인 화면으로 보내지 않고 모달로 묻는 이유는, 보던 시세나 차트를 잃지 않게 하기 위해서다.
 * 화면이 통째로 바뀌면 로그인은 넘어야 할 벽이 되지만, 모달이면 하려던 일에 딸린 한 단계로 남는다.
 */
export function LoginPromptProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const [reason, setReason] = useState<string | null>(null);

  const promptLogin = useCallback((next?: string) => {
    setReason(next ?? DEFAULT_REASON);
  }, []);

  // 로그인에 성공하면 인증 상태가 바뀐다. 물어볼 것이 없어졌으므로 모달은 스스로 물러난다.
  useEffect(() => {
    if (isAuthenticated) setReason(null);
  }, [isAuthenticated]);

  const value = useMemo(() => ({ promptLogin }), [promptLogin]);

  return (
    <LoginPromptContext.Provider value={value}>
      {children}
      <LoginPromptDialog reason={reason} onClose={() => setReason(null)} />
    </LoginPromptContext.Provider>
  );
}
