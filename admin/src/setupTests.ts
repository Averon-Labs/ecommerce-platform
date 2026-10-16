import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterEach } from 'vitest';

// React Testing Library's default 1s async query timeout is too tight for
// jsdom on slow CI or handset hardware under parallel test files; the cap
// only delays a genuine failure, it does not weaken assertions.
configure({ asyncUtilTimeout: 5000 });

// Vitest runs without globals, so React Testing Library cannot register its
// automatic cleanup; unmount each render to keep tests isolated.
afterEach(cleanup);
