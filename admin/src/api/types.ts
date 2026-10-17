/**
 * Admin user record returned by the session endpoints.
 *
 * Mirrors the `User` schema in docs/openapi.yaml (the same user shape the
 * customer API documents); `role` is `admin` for dashboard accounts.
 */
export interface AdminUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  status: string;
  createdAt?: string;
}

/** Page metadata shared by the paged admin lists (`Pagination` schema). */
export interface Pagination {
  currentPage: number;
  perPage: number;
  totalItems: number;
  totalPages: number;
}

export interface Paged<T> {
  data: T[];
  pagination: Pagination;
}

export type ProductStatus = 'active' | 'inactive' | 'archived';
export type CategoryStatus = 'active' | 'archived';

export type OrderStatus =
  | 'pending_payment'
  | 'confirmed'
  | 'processing'
  | 'shipped'
  | 'delivered'
  | 'cancelled';
export type PaymentStatus = 'pending' | 'completed' | 'failed';
export type PaymentMethod = 'demo';

export interface ProductImage {
  url: string;
  sortOrder: number;
}

/** `AdminProduct` — the administration view of a product. */
export interface AdminProduct {
  id: number;
  name: string;
  price: string;
  currency: string;
  categoryId: number | null;
  categoryName: string | null;
  imageUrl: string | null;
  inStock: boolean;
  status: ProductStatus;
  description: string | null;
  sku: string | null;
  stockQuantity: number;
  images: ProductImage[];
  updatedAt: string;
}

/**
 * `AdminProductInput` — create/PUT payload. `archived` is not assignable:
 * archiving goes through `POST /admin/products/{id}/archive`.
 */
export interface AdminProductInput {
  name: string;
  description: string | null;
  price: string;
  sku: string | null;
  categoryId: number | null;
  status: 'active' | 'inactive';
  imageUrls: string[];
}

/** `AdminCategory` — includes the non-archived product count. */
export interface AdminCategory {
  id: number;
  name: string;
  sortOrder: number;
  status: CategoryStatus;
  productCount: number;
}

export interface CategoryInput {
  name: string;
  sortOrder: number;
}

/** `InventoryRow` — absolute stock for one product. */
export interface InventoryRow {
  productId: number;
  name: string;
  sku: string | null;
  quantity: number;
  inStock: boolean;
  updatedAt: string | null;
}

export interface AdminProductQuery {
  page?: number;
  perPage?: number;
  q?: string;
  categoryId?: number;
  status?: ProductStatus;
}

export interface InventoryQuery {
  page?: number;
  perPage?: number;
  q?: string;
  inStock?: boolean;
}

/** `OrderCustomer` — the owning customer attached to admin order rows. */
export interface OrderCustomer {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
}

/** `OrderItem` — immutable product/price snapshot captured at purchase time. */
export interface OrderItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: string;
  lineTotal: string;
}

/** `OrderStatusEvent` — one recorded status transition. */
export interface OrderStatusEvent {
  status: OrderStatus;
  at: string;
}

/** `OrderPayment` — the simulated payment outcome. */
export interface OrderPayment {
  method: PaymentMethod;
  status: PaymentStatus;
  amount: string;
  currency: string;
  transactionId: string | null;
  createdAt: string;
}

/** `ShippingAddress` — the immutable address snapshot of an order. */
export interface ShippingAddress {
  recipientName: string;
  line1: string;
  line2?: string | null;
  city: string;
  region: string;
  postalCode: string;
  country: string;
}

/** `AdminOrderSummary` — one row of the admin order list. */
export interface AdminOrderSummary {
  id: number;
  reference: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  total: string;
  currency: string;
  itemCount: number;
  createdAt: string;
  customer: OrderCustomer;
}

/**
 * `AdminOrder` — full order with snapshots, payment, timeline, and the
 * transitions the server currently permits.
 */
export interface AdminOrder {
  id: number;
  reference: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  items: OrderItem[];
  subtotal: string;
  shipping: string;
  total: string;
  currency: string;
  shippingAddress: ShippingAddress;
  notes: string | null;
  payment: OrderPayment | null;
  timeline: OrderStatusEvent[];
  createdAt: string;
  updatedAt: string;
  customer: OrderCustomer;
  allowedTransitions: OrderStatus[];
}

export interface AdminOrderQuery {
  page?: number;
  perPage?: number;
  q?: string;
  status?: OrderStatus;
  from?: string;
  to?: string;
}
