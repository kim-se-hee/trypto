import { Routes, Route, Navigate } from "react-router-dom";
import { PublicRoute } from "@/components/auth/PublicRoute";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { RoundGuard } from "@/components/auth/RoundGuard";
import { LoginPage } from "@/pages/LoginPage";
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
      {/* Public: 미인증 사용자만 접근 */}
      <Route element={<PublicRoute />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      {/* Round guard: 인증됨 + 라운드 없을 때만 접근 */}
      <Route element={<RoundGuard />}>
        <Route path="/round/new" element={<RoundCreatePage />} />
      </Route>

      {/* Protected: 인증 + 활성 라운드 필요 */}
      <Route element={<ProtectedRoute />}>
        <Route path="/market" element={<MarketPage />} />
        <Route path="/portfolio" element={<PortfolioPage />} />
        <Route path="/wallet" element={<WalletPage />} />
        <Route path="/ranking" element={<RankingPage />} />
        <Route path="/regret" element={<RegretPage />} />
        <Route path="/mypage" element={<MyPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
