import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import type { RuleType } from "@/lib/types/round";

const RULE_LABEL: Record<RuleType, { label: string; unit: string }> = {
  STOP_LOSS: { label: "손절", unit: "%" },
  TAKE_PROFIT: { label: "익절", unit: "%" },
  NO_CHASE_BUY: { label: "추격 매수 금지", unit: "%" },
  AVERAGING_LIMIT: { label: "물타기 제한", unit: "회" },
  OVERTRADE_LIMIT: { label: "과매매 제한", unit: "회/일" },
};

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  ACTIVE: "default",
  BANKRUPT: "destructive",
  ENDED: "secondary",
};

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "진행중",
  BANKRUPT: "파산",
  ENDED: "종료",
};

function formatKRW(value: number): string {
  return value.toLocaleString("ko-KR") + "원";
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

export function MyPage() {
  const { user, updateUser } = useAuth();
  const { activeRound, clearRound } = useRound();
  const navigate = useNavigate();

  const [editingNickname, setEditingNickname] = useState(false);
  const [nicknameInput, setNicknameInput] = useState(user?.nickname ?? "");
  const [endDialogOpen, setEndDialogOpen] = useState(false);

  function handleNicknameSave() {
    const trimmed = nicknameInput.trim();
    if (trimmed && trimmed !== user?.nickname) {
      updateUser({ nickname: trimmed });
    }
    setEditingNickname(false);
  }

  function handleEndRound() {
    clearRound();
    setEndDialogOpen(false);
    navigate("/round/new", { replace: true });
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      {/* Hero */}
      <section className="bg-gradient-to-r from-primary/8 via-chart-2/6 to-primary/4 pb-8 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <h1 className="text-3xl font-extrabold tracking-tight">마이페이지</h1>
          <p className="mt-1.5 text-sm font-medium text-muted-foreground">
            프로필 관리 및 투자 라운드 현황
          </p>
        </div>
      </section>

      <main className="mx-auto max-w-6xl px-4 py-6">
        <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-2">
          {/* 프로필 카드 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">프로필</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-5">
              {/* 닉네임 */}
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm text-muted-foreground">닉네임</span>
                {editingNickname ? (
                  <div className="flex items-center gap-2">
                    <Input
                      value={nicknameInput}
                      onChange={(e) => setNicknameInput(e.target.value)}
                      className="h-8 w-36 text-sm"
                      onKeyDown={(e) => e.key === "Enter" && handleNicknameSave()}
                      autoFocus
                    />
                    <Button size="sm" variant="outline" className="h-8" onClick={handleNicknameSave}>
                      저장
                    </Button>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold">{user?.nickname}</span>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-7 px-2 text-xs"
                      onClick={() => {
                        setNicknameInput(user?.nickname ?? "");
                        setEditingNickname(true);
                      }}
                    >
                      수정
                    </Button>
                  </div>
                )}
              </div>

              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">이메일</span>
                <span className="text-sm">{user?.email}</span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">가입일</span>
                <span className="text-sm">
                  {user?.createdAt ? formatDate(user.createdAt) : "-"}
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">포트폴리오 공개</span>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-muted-foreground">
                    {user?.portfolioPublic ? "공개" : "비공개"}
                  </span>
                  <Switch
                    checked={user?.portfolioPublic ?? false}
                    onCheckedChange={(checked) => updateUser({ portfolioPublic: checked })}
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 현재 라운드 정보 */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">현재 라운드</CardTitle>
            </CardHeader>
            <CardContent>
              {activeRound ? (
                <div className="flex flex-col gap-5">
                  {/* 라운드 기본 정보 */}
                  <div className="flex items-center gap-3">
                    <span className="text-lg font-bold">라운드 {activeRound.roundNumber}</span>
                    <Badge variant={STATUS_VARIANT[activeRound.status] ?? "outline"}>
                      {STATUS_LABEL[activeRound.status] ?? activeRound.status}
                    </Badge>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    시작일: {formatDate(activeRound.startedAt)}
                  </p>

                  {/* Stat grid */}
                  <div className="grid grid-cols-3 gap-3">
                    <div className="rounded-xl bg-secondary/40 p-3 text-center">
                      <p className="text-[11px] text-muted-foreground">시드머니</p>
                      <p className="mt-1 text-sm font-bold">{formatKRW(activeRound.initialSeed)}</p>
                    </div>
                    <div className="rounded-xl bg-secondary/40 p-3 text-center">
                      <p className="text-[11px] text-muted-foreground">긴급자금 상한</p>
                      <p className="mt-1 text-sm font-bold">
                        {formatKRW(activeRound.emergencyFundingLimit)}
                      </p>
                    </div>
                    <div className="rounded-xl bg-secondary/40 p-3 text-center">
                      <p className="text-[11px] text-muted-foreground">남은 충전</p>
                      <p className="mt-1 text-sm font-bold">
                        {activeRound.emergencyChargeCount}회
                      </p>
                    </div>
                  </div>

                  <Separator />

                  {/* 투자 원칙 */}
                  <div>
                    <h3 className="mb-2 text-sm font-semibold">투자 원칙</h3>
                    {activeRound.rules.length > 0 ? (
                      <div className="flex flex-col gap-1.5">
                        {activeRound.rules.map((rule) => {
                          const cfg = RULE_LABEL[rule.ruleType];
                          return (
                            <div
                              key={rule.ruleId}
                              className="flex items-center justify-between rounded-lg bg-secondary/30 px-3 py-2"
                            >
                              <span className="text-sm">{cfg.label}</span>
                              <span className="text-sm font-semibold">
                                {rule.thresholdValue}
                                {cfg.unit}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    ) : (
                      <p className="text-xs text-muted-foreground">설정된 원칙이 없습니다.</p>
                    )}
                  </div>

                  <Separator />

                  {/* 라운드 종료 */}
                  <Dialog open={endDialogOpen} onOpenChange={setEndDialogOpen}>
                    <DialogTrigger asChild>
                      <Button variant="destructive" className="w-full">
                        라운드 종료
                      </Button>
                    </DialogTrigger>
                    <DialogContent>
                      <DialogHeader>
                        <DialogTitle>라운드를 종료하시겠습니까?</DialogTitle>
                        <DialogDescription>
                          종료된 라운드는 복구할 수 없습니다. 현재 보유한 모든 자산과 주문 내역이
                          초기화됩니다.
                        </DialogDescription>
                      </DialogHeader>
                      <DialogFooter className="gap-2 sm:gap-0">
                        <Button variant="outline" onClick={() => setEndDialogOpen(false)}>
                          취소
                        </Button>
                        <Button variant="destructive" onClick={handleEndRound}>
                          종료
                        </Button>
                      </DialogFooter>
                    </DialogContent>
                  </Dialog>
                </div>
              ) : (
                <div className="flex flex-col items-center gap-3 py-8 text-center">
                  <p className="text-sm text-muted-foreground">
                    진행 중인 라운드가 없습니다.
                  </p>
                  <Button variant="outline" onClick={() => navigate("/round/new")}>
                    새 라운드 시작
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}
