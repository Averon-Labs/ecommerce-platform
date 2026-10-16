import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { jsonResponse, mockApi } from '../../test/apiMock';
import type { MockRoute } from '../../test/apiMock';
import { category, pagination, product, sessionRoute } from '../../test/adminMock';

function categoryListRoute(): MockRoute {
  return {
    method: 'GET',
    path: '/admin/categories',
    handler: () => jsonResponse(200, { data: [category()] }),
  };
}

beforeEach(() => {
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('ProductFormPage (A-03)', () => {
  it('create_form_blocks_submit_with_client_validation', async () => {
    const fetchMock = mockApi([sessionRoute(), categoryListRoute()]);
    window.history.pushState({}, '', '/products/new');

    render(<App />);

    await screen.findByRole('heading', { level: 1, name: 'Create product' });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Name is required.')).toBeInTheDocument();
    expect(screen.getByText('Price is required.')).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.filter(
        ([, init]) => (init as RequestInit | undefined)?.method === 'POST',
      ),
    ).toHaveLength(0);

    fireEvent.change(screen.getByLabelText('Price (USD)'), { target: { value: '19.9' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(
      await screen.findByText(
        'Enter the price as digits with exactly two decimals, for example 19.99.',
      ),
    ).toBeInTheDocument();
  });

  it('create_form_submits_the_contract_payload_and_returns_with_feedback', async () => {
    const fetchMock = mockApi([
      sessionRoute(),
      categoryListRoute(),
      {
        method: 'POST',
        path: '/admin/products',
        handler: () => jsonResponse(201, { data: product({ name: 'New Lamp' }) }),
      },
      {
        method: 'GET',
        path: '/admin/products',
        handler: () => jsonResponse(200, { data: [], pagination: pagination() }),
      },
    ]);
    window.history.pushState({}, '', '/products/new');

    render(<App />);

    await screen.findByRole('heading', { level: 1, name: 'Create product' });
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'New Lamp' } });
    fireEvent.change(screen.getByLabelText('Price (USD)'), { target: { value: '19.99' } });
    fireEvent.change(screen.getByLabelText(/^SKU/), { target: { value: 'NEW-1' } });
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add image URL' }));
    fireEvent.change(screen.getByLabelText('Image URL 1'), {
      target: { value: 'https://cdn.example.test/a.jpg' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(
      await screen.findByText('New Lamp was created. Set its stock in Inventory.'),
    ).toBeInTheDocument();

    const postCall = fetchMock.mock.calls.find(
      ([, init]) => (init as RequestInit | undefined)?.method === 'POST',
    );
    expect(postCall).toBeDefined();
    const body = JSON.parse(String((postCall?.[1] as RequestInit).body));
    expect(body).toEqual({
      name: 'New Lamp',
      description: null,
      price: '19.99',
      sku: 'NEW-1',
      categoryId: 1,
      status: 'active',
      imageUrls: ['https://cdn.example.test/a.jpg'],
    });
  });

  it('edit_form_loads_values_and_maps_sku_conflict_to_the_field', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(),
      {
        method: 'GET',
        path: '/admin/products/7',
        handler: () =>
          jsonResponse(200, {
            data: product({
              id: 7,
              status: 'inactive',
              images: [{ url: 'https://cdn.example.test/a.jpg', sortOrder: 0 }],
            }),
          }),
      },
      {
        method: 'PUT',
        path: '/admin/products/7',
        handler: () =>
          jsonResponse(409, {
            error: {
              code: 'SKU_TAKEN',
              message: 'The SKU is already used by another product.',
            },
          }),
      },
    ]);
    window.history.pushState({}, '', '/products/7/edit');

    render(<App />);

    await screen.findByRole('heading', { level: 1, name: 'Edit product' });
    await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Aurora Desk Lamp'));
    expect(screen.getByLabelText(/^SKU/)).toHaveValue('AUR-1');
    expect(screen.getByLabelText('Image URL 1')).toHaveValue('https://cdn.example.test/a.jpg');

    fireEvent.change(screen.getByLabelText(/^SKU/), { target: { value: 'DUP-1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(
      await screen.findByText('The SKU is already used by another product.'),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/^SKU/)).toHaveValue('DUP-1');
  });

  it('edit_form_shows_not_found_for_a_missing_product', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(),
      {
        method: 'GET',
        path: '/admin/products/99',
        handler: () =>
          jsonResponse(404, {
            error: { code: 'NOT_FOUND', message: 'The requested resource does not exist.' },
          }),
      },
    ]);
    window.history.pushState({}, '', '/products/99/edit');

    render(<App />);

    expect(await screen.findByText('Product not found')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to products' })).toHaveAttribute(
      'href',
      '/products',
    );
  });

  it('unsaved_changes_guard_blocks_sidebar_navigation_and_cancel', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(),
      {
        method: 'GET',
        path: '/admin/products',
        handler: () => jsonResponse(200, { data: [], pagination: pagination() }),
      },
    ]);
    window.history.pushState({}, '', '/products/new');

    render(<App />);

    await screen.findByRole('heading', { level: 1, name: 'Create product' });
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Dirty Draft' } });

    fireEvent.click(screen.getByRole('link', { name: 'Categories' }));

    const guardDialog = await screen.findByRole('dialog', { name: 'Discard unsaved changes?' });
    expect(screen.getByRole('heading', { level: 1, name: 'Create product' })).toBeInTheDocument();
    fireEvent.click(within(guardDialog).getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Name')).toHaveValue('Dirty Draft');

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    const cancelDialog = await screen.findByRole('dialog', { name: 'Discard unsaved changes?' });
    fireEvent.click(within(cancelDialog).getByRole('button', { name: 'Discard changes' }));

    expect(await screen.findByText('No products yet')).toBeInTheDocument();
  });

  it('server_validation_maps_422_details_to_fields_and_preserves_values', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(),
      {
        method: 'GET',
        path: '/admin/products/7',
        handler: () => jsonResponse(200, { data: product({ id: 7 }) }),
      },
      {
        method: 'PUT',
        path: '/admin/products/7',
        handler: () =>
          jsonResponse(422, {
            error: {
              code: 'VALIDATION_ERROR',
              message: 'One or more request fields are invalid.',
              details: [
                {
                  field: 'name',
                  message: 'The name field must not be greater than 160 characters.',
                },
                { field: 'price', message: 'The price field must be a valid amount.' },
              ],
            },
          }),
      },
    ]);
    window.history.pushState({}, '', '/products/7/edit');

    render(<App />);

    await screen.findByRole('heading', { level: 1, name: 'Edit product' });
    await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Aurora Desk Lamp'));

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Renamed Lamp' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(
      await screen.findByText('The name field must not be greater than 160 characters.'),
    ).toBeInTheDocument();
    expect(screen.getByText('The price field must be a valid amount.')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Check the highlighted fields and try again.',
    );
    // The failure is recoverable: entered values are preserved for the retry.
    expect(screen.getByLabelText('Name')).toHaveValue('Renamed Lamp');
  });
});
