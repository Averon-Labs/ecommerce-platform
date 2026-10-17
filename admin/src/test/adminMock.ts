import { jsonResponse } from './apiMock';
import type { MockRoute } from './apiMock';
import type {
  AdminCategory,
  AdminOrder,
  AdminOrderSummary,
  AdminProduct,
  InventoryRow,
  OrderCustomer,
  Pagination,
} from '../api/types';

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

export const ORDER_CUSTOMER: OrderCustomer = {
  id: 41,
  email: 'sara@example.com',
  firstName: 'Sara',
  lastName: 'Nouri',
};

export function orderSummary(overrides: Partial<AdminOrderSummary> = {}): AdminOrderSummary {
  return {
    id: 17,
    reference: 'ESV-000017',
    status: 'confirmed',
    paymentStatus: 'completed',
    total: '84.98',
    currency: 'USD',
    itemCount: 2,
    createdAt: '2026-10-11T09:41:00Z',
    customer: ORDER_CUSTOMER,
    ...overrides,
  };
}

export function orderDetail(overrides: Partial<AdminOrder> = {}): AdminOrder {
  return {
    id: 17,
    reference: 'ESV-000017',
    status: 'confirmed',
    paymentStatus: 'completed',
    items: [
      {
        productId: 1,
        productName: 'Aurora Desk Lamp',
        quantity: 2,
        unitPrice: '39.99',
        lineTotal: '79.98',
      },
    ],
    subtotal: '79.98',
    shipping: '5.00',
    total: '84.98',
    currency: 'USD',
    shippingAddress: {
      recipientName: 'Sara Nouri',
      line1: '14 Maple Street',
      line2: null,
      city: 'Seattle',
      region: 'WA',
      postalCode: '98101',
      country: 'US',
    },
    notes: 'Please leave at the front desk.',
    payment: {
      method: 'demo',
      status: 'completed',
      amount: '84.98',
      currency: 'USD',
      transactionId: 'DEMO-8HZK4M2QW7PR',
      createdAt: '2026-10-11T09:41:05Z',
    },
    timeline: [
      { status: 'pending_payment', at: '2026-10-11T09:41:00Z' },
      { status: 'confirmed', at: '2026-10-11T09:41:05Z' },
    ],
    createdAt: '2026-10-11T09:41:00Z',
    updatedAt: '2026-10-11T09:41:05Z',
    customer: ORDER_CUSTOMER,
    allowedTransitions: ['processing', 'cancelled'],
    ...overrides,
  };
}
