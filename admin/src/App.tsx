import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { RequireAdmin } from './auth/RequireAdmin';
import { AppShell } from './components/AppShell';
import LoginPage from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { SectionPlaceholder } from './pages/SectionPlaceholder';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<RequireAdmin />}>
            <Route element={<AppShell />}>
              <Route index element={<Navigate to="/products" replace />} />
              <Route path="products" element={<SectionPlaceholder title="Products" />} />
              <Route path="categories" element={<SectionPlaceholder title="Categories" />} />
              <Route path="inventory" element={<SectionPlaceholder title="Inventory" />} />
            </Route>
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
