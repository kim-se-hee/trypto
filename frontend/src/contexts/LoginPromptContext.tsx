import { createContext, useContext } from "react";

// 컴포넌트(LoginPromptProvider)는 같은 파일에 두지 않는다. AuthContext 와 같은 이유다.

export interface LoginPromptContextValue {
  /**
   * 로그인이 필요한 동작을 눌렀을 때 부른다.
   * reason 은 왜 로그인이 필요한지 알리는 한 줄로, 모달 설명에 그대로 쓰인다.
   */
  promptLogin: (reason?: string) => void;
}

export const LoginPromptContext = createContext<LoginPromptContextValue | null>(null);

export function useLoginPrompt(): LoginPromptContextValue {
  const ctx = useContext(LoginPromptContext);
  if (!ctx) throw new Error("useLoginPrompt must be used within LoginPromptProvider");
  return ctx;
}
