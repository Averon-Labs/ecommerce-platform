import { apiRequest } from './http';
import { buildQuery } from './query';
import type {
  AdminCategory,
  AdminProduct,
  AdminProductInput,
  AdminProductQuery,
  CategoryInput,
  InventoryQuery,
  InventoryRow,
  Paged,
  ProductImage,
} from './types';

/**
 * Admin catalog and inventory endpoints (docs/openapi.yaml — Admin catalog).
 *
 * Stock is owned by `PATCH /admin/inventory/{productId}` and is never part of
 * the product payload; archiving uses dedicated endpoints and is not a delete.
 * Image files are uploaded and deleted through their own operations — the
 * product payload only carries their order.
 */

/* ------------------------------------------------------------------ products */

export function listAdminProducts(
  query: AdminProductQuery = {},
  signal?: AbortSignal,
): Promise<Paged<AdminProduct>> {
  const search = buildQuery({
    page: query.page,
    perPage: query.perPage,
    q: query.q,
    categoryId: query.categoryId,
    status: query.status,
  });

  return apiRequest<Paged<AdminProduct>>(`/admin/products${search}`, { signal });
}

export async function getAdminProduct(
  productId: number,
  signal?: AbortSignal,
): Promise<AdminProduct> {
  const payload = await apiRequest<{ data: AdminProduct }>(`/admin/products/${productId}`, {
    signal,
  });
  return payload.data;
}

export async function createAdminProduct(input: AdminProductInput): Promise<AdminProduct> {
  const payload = await apiRequest<{ data: AdminProduct }>('/admin/products', {
    method: 'POST',
    body: input,
  });
  return payload.data;
}

export async function updateAdminProduct(
  productId: number,
  input: AdminProductInput,
): Promise<AdminProduct> {
  const payload = await apiRequest<{ data: AdminProduct }>(`/admin/products/${productId}`, {
    method: 'PUT',
    body: input,
  });
  return payload.data;
}

export async function archiveAdminProduct(productId: number): Promise<AdminProduct> {
  const payload = await apiRequest<{ data: AdminProduct }>(
    `/admin/products/${productId}/archive`,
    { method: 'POST' },
  );
  return payload.data;
}

/**
 * Hard-delete a product, its image rows, and the files stored for them.
 * Answers 409 `PRODUCT_HAS_ORDERS` when an order references the product.
 */
export async function deleteAdminProduct(productId: number): Promise<void> {
  await apiRequest<void>(`/admin/products/${productId}`, { method: 'DELETE' });
}

/** POST /admin/products/{id}/images — multipart upload of one image file. */
export async function uploadAdminProductImage(productId: number, file: File): Promise<ProductImage> {
  const data = new FormData();
  data.append('image', file);

  const payload = await apiRequest<{ data: ProductImage }>(`/admin/products/${productId}/images`, {
    method: 'POST',
    formData: data,
  });
  return payload.data;
}

/** DELETE /admin/products/{id}/images/{imageId} — also removes the file. */
export async function deleteAdminProductImage(productId: number, imageId: number): Promise<void> {
  await apiRequest<void>(`/admin/products/${productId}/images/${imageId}`, {
    method: 'DELETE',
  });
}

/* ---------------------------------------------------------------- categories */

export async function listAdminCategories(signal?: AbortSignal): Promise<AdminCategory[]> {
  const payload = await apiRequest<{ data: AdminCategory[] }>('/admin/categories', { signal });
  return payload.data;
}

export async function createAdminCategory(input: CategoryInput): Promise<AdminCategory> {
  const payload = await apiRequest<{ data: AdminCategory }>('/admin/categories', {
    method: 'POST',
    body: input,
  });
  return payload.data;
}

export async function updateAdminCategory(
  categoryId: number,
  input: CategoryInput,
): Promise<AdminCategory> {
  const payload = await apiRequest<{ data: AdminCategory }>(`/admin/categories/${categoryId}`, {
    method: 'PUT',
    body: input,
  });
  return payload.data;
}

export async function archiveAdminCategory(categoryId: number): Promise<AdminCategory> {
  const payload = await apiRequest<{ data: AdminCategory }>(
    `/admin/categories/${categoryId}/archive`,
    { method: 'POST' },
  );
  return payload.data;
}

export async function restoreAdminCategory(categoryId: number): Promise<AdminCategory> {
  const payload = await apiRequest<{ data: AdminCategory }>(
    `/admin/categories/${categoryId}/restore`,
    { method: 'POST' },
  );
  return payload.data;
}

/* ----------------------------------------------------------------- inventory */

export function listAdminInventory(
  query: InventoryQuery = {},
  signal?: AbortSignal,
): Promise<Paged<InventoryRow>> {
  const search = buildQuery({
    page: query.page,
    perPage: query.perPage,
    q: query.q,
    inStock: query.inStock,
  });

  return apiRequest<Paged<InventoryRow>>(`/admin/inventory${search}`, { signal });
}

/** Sets the absolute quantity (never a delta); zero means out of stock. */
export async function updateAdminInventory(
  productId: number,
  quantity: number,
): Promise<InventoryRow> {
  const payload = await apiRequest<{ data: InventoryRow }>(`/admin/inventory/${productId}`, {
    method: 'PATCH',
    body: { quantity },
  });
  return payload.data;
}
