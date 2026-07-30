import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { SocialLoginButtons } from "./SocialLoginButtons";

interface LoginPromptDialogProps {
  open: boolean;
  onClose: () => void;
}

export function LoginPromptDialog({ open, onClose }: LoginPromptDialogProps) {
  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) onClose();
      }}
    >
      {/* 설명문을 두지 않으므로 참조도 비운다. 비워 두지 않으면 라딕스가 콘솔에 경고를 남긴다 */}
      <DialogContent className="sm:max-w-[380px]" aria-describedby={undefined}>
        <DialogHeader>
          <div className="flex items-center gap-2.5">
            <img src="/favicon.png" alt="" className="h-8 w-8 rounded-xl" />
            <DialogTitle>로그인하고 이어서 하기</DialogTitle>
          </div>
        </DialogHeader>

        {/* 로그인에 성공하면 모달은 제 할 일을 마쳤으므로 스스로 물러난다.
            인증 상태를 지켜보다 닫으면, 나중에 로그아웃할 때 다시 되살아난다 */}
        <SocialLoginButtons onSuccess={onClose} />
      </DialogContent>
    </Dialog>
  );
}
