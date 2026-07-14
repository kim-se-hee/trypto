import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthProvider'
import { RoundProvider } from './contexts/RoundProvider'
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
