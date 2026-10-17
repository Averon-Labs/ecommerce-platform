# Implementation Plan: ADMIN-DASHBOARD-INTEGRATION — Admin Panel Alignment & Customer OAuth Support

## Overview
This plan specifies the enhancements and integrations for the **Admin Dashboard** (React 18 + TypeScript + Vite) in relation to the overall EShop Verse architecture, customer Google OAuth authentication support, and design system alignment.

---

## Architecture & Integration Scope

The Admin Dashboard communicates with the Laravel backend via `/api/v1/` using Sanctum session cookies and CSRF tokens.
With the introduction of Google OAuth for customers, the Admin Panel must:
1. Accurately reflect customer authentication providers (Email/Password vs Google OAuth) in customer lists or order details.
2. Prevent accidental password-reset triggers for OAuth-only customer accounts without a local password.
3. Align visual components with the unified design language (Bento grid panels, OLED dark mode, responsive tables).

---

## 1. Key Features & Workflows

### 1.1 Customer Auth Provider Visibility
- In customer/order detail views, display a subtle provider indicator:
  - **Google Account:** Display Google icon badge with text `Google`.
  - **Standard Account:** Display email icon badge with text `Password`.
- If an account has `google_id` and no local password, disable the "Reset Password Email" action with an explanatory tooltip: *"This customer uses Google Sign-In."*

### 1.2 Design System Alignment (Pinterest / MUSE & Bento Metrics)
- Follow `docs/DESIGN_SYSTEM.md` and `docs/SCREEN_SPECIFICATIONS.md`:
  - White surface cards (`#FFFFFF`) with 1dp hairline border (`#E4E0D7`).
  - Warm neutral canvas (`#FAF8F2` / `#F5F2EA`).
  - Deep OLED dark mode (`#151A16` canvas, `#202720` surface).
  - 52dp pill buttons for primary CTAs and 20dp Bento card metrics.

---

## 2. Files to Create / Modify

| Area | Path | Description |
| :--- | :--- | :--- |
| **Types** | `admin/src/types/user.ts` | Add `googleId?: string` or `authProvider: 'google' \| 'email'` to Customer/User types. |
| **Components** | `admin/src/components/ui/AuthProviderBadge.tsx` | Reusable badge indicating account auth provider (Google vs Email). |
| **Orders View** | `admin/src/pages/Orders/OrderDetail.tsx` | Display customer auth badge in customer details card. |
| **Customer View**| `admin/src/pages/Customers/CustomerDetail.tsx` | Show account type and disable password reset for OAuth users. |
| **Tests** | `admin/src/components/ui/AuthProviderBadge.test.tsx` | Unit tests for badge rendering and states. |

---

## 3. Implementation Steps

1. **Step 1: Type Definitions Update**
   - Update `User` and `Customer` interfaces in `admin/src/types/` to include `authProvider` or `googleId` returned by `/api/v1/customers` or `/api/v1/orders`.
2. **Step 2: Auth Provider Badge Component**
   - Create `AuthProviderBadge.tsx` using Tailwind / Vanilla CSS with Google logo vector.
3. **Step 3: Detail Views Integration**
   - Integrate the badge into Order Details and Customer management rows.
   - Add conditional logic to prevent sending password reset emails to Google OAuth-only accounts.
4. **Step 4: Quality Checks & Verification**
   - Run `npm run typecheck` in `admin/`.
   - Run `npm run lint` in `admin/`.
   - Run `npm test` to ensure 100% test pass rate.
   - Verify `npm run build` generates clean production bundles.
