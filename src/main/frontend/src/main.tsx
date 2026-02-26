import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { RoundProvider } from './contexts/RoundContext'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <RoundProvider>
          <App />
        </RoundProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
