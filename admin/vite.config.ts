import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Backend SANCTUM_STATEFUL_DOMAINS expects the SPA on localhost:5173.
    port: 5173,
    strictPort: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    // jsdom startup is expensive on handset hardware; cap workers to reduce
    // CPU contention and give slower devices room to settle each test.
    maxWorkers: 4,
    testTimeout: 20000,
  },
});
