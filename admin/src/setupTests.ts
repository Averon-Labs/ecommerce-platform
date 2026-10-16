import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Vitest runs without globals, so React Testing Library cannot register its
// automatic cleanup; unmount each render to keep tests isolated.
afterEach(cleanup);
