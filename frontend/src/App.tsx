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
      {/* 인증 여부와 무관하게 누구나 접근. 남의 것을 보기만 하는 화면은 열어 두고,
          내 것을 건드리는 순간(주문·랭커 포트폴리오)에 각 화면이 로그인을 묻는다 */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/market" element={<MarketPage />} />
      <Route path="/ranking" element={<RankingPage />} />

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

      {/* Protected: 인증 필요. 라운드를 한 번도 시작한 적 없으면 라운드 생성부터 하게 한다.
          내 자산·내 기록만 담는 화면이라 라운드 없이는 보여줄 것이 없다 */}
      <Route element={<ProtectedRoute />}>
        <Route path="/portfolio" element={<PortfolioPage />} />
        <Route path="/wallet" element={<WalletPage />} />
        <Route path="/regret" element={<RegretPage />} />
        <Route path="/mypage" element={<MyPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
