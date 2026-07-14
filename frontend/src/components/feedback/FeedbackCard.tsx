import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { sendFeedback } from "@/lib/api/feedback-api";
import { isApiClientError } from "@/lib/api/types";

const MIN_LENGTH = 20;
const MAX_LENGTH = 1000;

export function FeedbackCard() {
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  const length = content.trim().length;
  const canSubmit = length >= MIN_LENGTH && length <= MAX_LENGTH && !submitting;

  async function handleSubmit() {
    if (!canSubmit) return;

    setSubmitting(true);
    setError(null);
    try {
      await sendFeedback(content.trim());
      setContent("");
      setSent(true);
    } catch (e) {
      setError(isApiClientError(e) ? e.message : "피드백 전송에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="lg:col-span-2">
      <CardHeader>
        <CardTitle className="text-base">피드백 보내기</CardTitle>
        <p className="text-sm text-muted-foreground">
          사용하면서 느낀 점이나 개선했으면 하는 부분을 알려주세요.
        </p>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <textarea
          value={content}
          onChange={(e) => {
            setContent(e.target.value);
            setSent(false);
          }}
          maxLength={MAX_LENGTH}
          rows={5}
          placeholder="어떤 점이 좋았고, 무엇이 아쉬웠나요?"
          className="w-full resize-none rounded-md border border-input bg-transparent px-3 py-2 text-sm outline-none transition-colors placeholder:text-muted-foreground/40 focus:border-primary/40 focus:ring-1 focus:ring-ring/50"
        />

        <div className="flex items-center justify-between gap-3">
          <p className="text-xs text-muted-foreground">
            {length < MIN_LENGTH
              ? `최소 ${MIN_LENGTH}자 이상 입력해주세요.`
              : `${length} / ${MAX_LENGTH}자`}
          </p>
          <Button size="sm" onClick={handleSubmit} disabled={!canSubmit}>
            {submitting ? "보내는 중..." : "보내기"}
          </Button>
        </div>

        {error && <p className="text-xs text-destructive">{error}</p>}
        {sent && (
          <p className="text-xs text-muted-foreground">
            피드백이 접수되었습니다. 소중한 의견 감사합니다.
          </p>
        )}
      </CardContent>
    </Card>
  );
}
