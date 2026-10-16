/// <reference types="vite/client" />

const FALLBACK_API_BASE_URL = 'http://localhost:8000/api/v1';

/**
 * REST API origin for the local Laravel backend, read from the git-ignored
 * `.env` / `.env.local` (see `.env.example`). Never put credentials here —
 * authentication is handled by the backend's Sanctum session cookie.
 */
export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL || FALLBACK_API_BASE_URL;
