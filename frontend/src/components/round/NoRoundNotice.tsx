import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";

interface NoRoundNoticeProps {
  description?: string;
}

export function NoRoundNotice({
  description = "진행 중인 라운드가 없습니다.",
}: NoRoundNoticeProps) {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center gap-3 py-8 text-center">
      <p className="text-sm text-muted-foreground">{description}</p>
      <Button variant="outline" onClick={() => navigate("/round/new")}>
        새 라운드 시작
      </Button>
    </div>
  );
}
