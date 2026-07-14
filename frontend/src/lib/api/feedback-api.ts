import { apiPost } from "./client";

export function sendFeedback(content: string): Promise<void> {
  return apiPost<void>("/api/feedbacks", { content });
}
