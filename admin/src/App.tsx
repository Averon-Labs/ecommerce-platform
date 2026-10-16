import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { RequireAdmin } from './auth/RequireAdmin';
import { AppShell } from './components/AppShell';
import LoginPage from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import CategoriesPage from './pages/categories/CategoriesPage';
import InventoryPage from './pages/inventory/InventoryPage';
import ProductFormPage from './pages/products/ProductFormPage';
import ProductsPage from './pages/products/ProductsPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<RequireAdmin />}>
            <Route element={<AppShell />}>
              <Route index element={<Navigate to="/products" replace />} />
              <Route path="products" element={<ProductsPage />} />
              <Route path="products/new" element={<ProductFormPage />} />
              <Route path="products/:productId/edit" element={<ProductFormPage />} />
              <Route path="categories" element={<CategoriesPage />} />
              <Route path="inventory" element={<InventoryPage />} />
            </Route>
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
