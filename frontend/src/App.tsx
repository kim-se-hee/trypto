import { Routes, Route, Navigate } from "react-router-dom";
import { PublicRoute } from "@/components/auth/PublicRoute";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { RoundGuard } from "@/components/auth/RoundGuard";
import { LandingPage } from "@/pages/LandingPage";
import { LoginPage } from "@/pages/LoginPage";
import { SocialCallbackPage } from "@/pages/SocialCallbackPage";
import { RoundCreatePage } from "@/pages/RoundCreatePage";
import { MarketPage } from "@/pages/MarketPage";
import { PortfolioPage } from "@/pages/PortfolioPage";
import { WalletPage } from "@/pages/WalletPage";
import { RankingPage } from "@/pages/RankingPage";
import { RegretPage } from "@/pages/RegretPage";
import { MyPage } from "@/pages/MyPage";

function App() {
  return (
    <Routes>
      {/* 랜딩: 인증 여부와 무관하게 누구나 접근 */}
      <Route path="/" element={<LandingPage />} />

      {/* Public: 미인증 사용자만 접근 */}
      <Route element={<PublicRoute />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      {/* 소셜 인가 콜백: 인증 여부와 무관하게 항상 처리 */}
      <Route path="/auth/:provider/callback" element={<SocialCallbackPage />} />

      {/* Round guard: 인증됨 + 라운드 없을 때만 접근 */}
      <Route element={<RoundGuard />}>
        <Route path="/round/new" element={<RoundCreatePage />} />
      </Route>

      {/* Protected: 인증 필요. 라운드를 한 번도 시작한 적 없으면 라운드 생성부터 하게 한다 */}
      <Route element={<ProtectedRoute />}>
        <Route path="/market" element={<MarketPage />} />
        <Route path="/portfolio" element={<PortfolioPage />} />
        <Route path="/wallet" element={<WalletPage />} />
        <Route path="/ranking" element={<RankingPage />} />
        <Route path="/regret" element={<RegretPage />} />
        <Route path="/mypage" element={<MyPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
