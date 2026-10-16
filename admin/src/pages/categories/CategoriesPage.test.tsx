import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { jsonResponse, mockApi } from '../../test/apiMock';
import type { MockRoute } from '../../test/apiMock';
import { category, sessionRoute } from '../../test/adminMock';

function categoryListRoute(handler: MockRoute['handler']): MockRoute {
  return { method: 'GET', path: '/admin/categories', handler };
}

beforeEach(() => {
  window.history.pushState({}, '', '/categories');
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('CategoriesPage (A-04)', () => {
  it('categories_list_renders_counts_and_statuses', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(() =>
        jsonResponse(200, {
          data: [
            category(),
            category({ id: 2, name: 'Outdoor', sortOrder: 1, status: 'archived', productCount: 0 }),
          ],
        }),
      ),
    ]);

    render(<App />);

    const table = await screen.findByRole('table', { name: 'Categories' });
    expect(within(table).getByText('Lighting')).toBeInTheDocument();
    expect(within(table).getByText('3 products')).toBeInTheDocument();
    expect(within(table).getByText('Outdoor')).toBeInTheDocument();
    expect(within(table).getByText('Archived')).toBeInTheDocument();
    expect(within(table).getByRole('button', { name: 'Restore Outdoor' })).toBeInTheDocument();
  });

  it('category_search_filters_the_visible_rows', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(() =>
        jsonResponse(200, { data: [category(), category({ id: 2, name: 'Outdoor' })] }),
      ),
    ]);

    render(<App />);

    await screen.findByRole('table', { name: 'Categories' });
    fireEvent.change(screen.getByLabelText('Search categories'), { target: { value: 'out' } });

    const table = screen.getByRole('table', { name: 'Categories' });
    expect(within(table).getByText('Outdoor')).toBeInTheDocument();
    expect(within(table).queryByText('Lighting')).not.toBeInTheDocument();
  });

  it('create_category_maps_name_conflict_to_the_field', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(() => jsonResponse(200, { data: [category()] })),
      {
        method: 'POST',
        path: '/admin/categories',
        handler: () =>
          jsonResponse(409, {
            error: {
              code: 'CATEGORY_NAME_TAKEN',
              message: 'A category with this name already exists.',
            },
          }),
      },
    ]);

    render(<App />);

    await screen.findByRole('table', { name: 'Categories' });
    fireEvent.click(screen.getByRole('button', { name: 'Add category' }));

    const dialog = await screen.findByRole('dialog', { name: 'Add category' });
    fireEvent.change(within(dialog).getByLabelText('Name'), { target: { value: 'Lighting' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(
      await screen.findByText('A category with this name already exists.'),
    ).toBeInTheDocument();
  });

  it('archive_category_reports_blocking_products', async () => {
    mockApi([
      sessionRoute(),
      categoryListRoute(() => jsonResponse(200, { data: [category({ productCount: 2 })] })),
      {
        method: 'POST',
        path: '/admin/categories/1/archive',
        handler: () =>
          jsonResponse(409, {
            error: {
              code: 'CATEGORY_NOT_EMPTY',
              message: 'This category still has active products.',
              details: [{ message: 'Move or archive the 2 active products first.' }],
            },
          }),
      },
    ]);

    render(<App />);

    const table = await screen.findByRole('table', { name: 'Categories' });
    fireEvent.click(within(table).getByRole('button', { name: 'Archive Lighting' }));

    const dialog = await screen.findByRole('dialog', { name: 'Archive Lighting?' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Archive category' }));

    expect(
      await screen.findByText(
        'This category still has active products. Move or archive the 2 active products first.',
      ),
    ).toBeInTheDocument();
  });

  it('restore_category_updates_status_and_reports_feedback', async () => {
    let restored = false;
    mockApi([
      sessionRoute(),
      categoryListRoute(() =>
        jsonResponse(200, { data: [restored ? category() : category({ status: 'archived' })] }),
      ),
      {
        method: 'POST',
        path: '/admin/categories/1/restore',
        handler: () => {
          restored = true;
          return jsonResponse(200, { data: category() });
        },
      },
    ]);

    render(<App />);

    const table = await screen.findByRole('table', { name: 'Categories' });
    fireEvent.click(within(table).getByRole('button', { name: 'Restore Lighting' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Lighting was restored.');

    await waitFor(() => {
      const table = screen.getByRole('table', { name: 'Categories' });
      expect(within(table).getByText('Active')).toBeInTheDocument();
    });
  });
});
