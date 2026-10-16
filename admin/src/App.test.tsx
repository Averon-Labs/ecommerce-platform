import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import App from './App';
import { API_BASE_URL } from './config/api';

describe('App', () => {
  it('renders_admin_shell_shows_title_and_api_origin', () => {
    render(<App />);

    expect(
      screen.getByRole('heading', { level: 1, name: 'EShop Verse Admin' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(`API origin: ${API_BASE_URL}`),
    ).toBeInTheDocument();
  });

  it('renders_admin_shell_without_dom_leakage_between_tests', () => {
    // src/setupTests.ts runs cleanup after each test.
    expect(document.body).toBeEmptyDOMElement();

    render(<App />);

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  });
});
