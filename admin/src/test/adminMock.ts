import { jsonResponse } from './apiMock';
import type { MockRoute } from './apiMock';
import type { AdminCategory, AdminProduct, InventoryRow, Pagination } from '../api/types';

export const ADMIN_USER = {
  id: 2,
  email: 'demo-admin@eshopverse.test',
  firstName: 'Demo',
  lastName: 'Administrator',
  role: 'admin',
  status: 'active',
};

/** Signed-in session for pages that render inside the admin shell. */
export function sessionRoute(): MockRoute {
  return {
    method: 'GET',
    path: '/admin/auth/session',
    handler: () => jsonResponse(200, { data: { user: ADMIN_USER } }),
  };
}

export function pagination(
  currentPage = 1,
  perPage = 20,
  totalItems = 0,
  totalPages = 0,
): Pagination {
  return { currentPage, perPage, totalItems, totalPages };
}

export function product(overrides: Partial<AdminProduct> = {}): AdminProduct {
  return {
    id: 5,
    name: 'Aurora Desk Lamp',
    price: '39.99',
    currency: 'USD',
    categoryId: 1,
    categoryName: 'Lighting',
    imageUrl: null,
    inStock: true,
    status: 'active',
    description: 'Warm reading light.',
    sku: 'AUR-1',
    stockQuantity: 7,
    images: [],
    updatedAt: '2026-10-01T10:00:00+00:00',
    ...overrides,
  };
}

export function category(overrides: Partial<AdminCategory> = {}): AdminCategory {
  return {
    id: 1,
    name: 'Lighting',
    sortOrder: 0,
    status: 'active',
    productCount: 3,
    ...overrides,
  };
}

export function inventoryRow(overrides: Partial<InventoryRow> = {}): InventoryRow {
  return {
    productId: 5,
    name: 'Aurora Desk Lamp',
    sku: 'AUR-1',
    quantity: 7,
    inStock: true,
    updatedAt: '2026-10-01T10:00:00+00:00',
    ...overrides,
  };
}
