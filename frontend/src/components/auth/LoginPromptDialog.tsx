import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { SocialLoginButtons } from "./SocialLoginButtons";

interface LoginPromptDialogProps {
  /** 로그인이 왜 필요한지 알리는 한 줄. null 이면 닫힌 상태다. */
  reason: string | null;
  onClose: () => void;
}

export function LoginPromptDialog({ reason, onClose }: LoginPromptDialogProps) {
  return (
    <Dialog
      open={reason !== null}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
    >
      <DialogContent className="sm:max-w-[380px]">
        <DialogHeader>
          <div className="flex items-center gap-2.5">
            <img src="/favicon.png" alt="" className="h-8 w-8 rounded-xl" />
            <DialogTitle>로그인하고 이어서 하기</DialogTitle>
          </div>
          <DialogDescription>{reason}</DialogDescription>
        </DialogHeader>

        {/* 로그인에 성공하면 모달은 제 할 일을 마쳤으므로 스스로 물러난다.
            인증 상태를 지켜보다 닫으면, 나중에 로그아웃할 때 남은 문구를 들고 되살아난다 */}
        <SocialLoginButtons onSuccess={onClose} />

        <p className="text-center text-xs text-muted-foreground">
          보던 화면 그대로 이어집니다. 따로 가입할 것은 없습니다.
        </p>
      </DialogContent>
    </Dialog>
  );
}
