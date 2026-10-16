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
