# Rule 06: Accessible Forms & Human Feedback

Forms are where conversions happen and where user frustration is most acute. A sleek form that lacks accessible markup or clear feedback results in abandoned carts, angry users, and failed audits.

---

## 1. Persistent Labels & Associations

Never rely exclusively on `placeholder` attributes. When a user begins typing, placeholders vanish, leaving them unsure of what field they are editing.

```tsx
// ✅ CORRECT: Semantic, Accessible Input Group
<div className="flex flex-col gap-1.5">
  <label 
    htmlFor="billing-email" 
    className="text-xs font-medium uppercase tracking-wider text-neutral-400"
  >
    Email Address <span className="text-rose-400">*</span>
  </label>
  <div className="relative">
    <input
      id="billing-email"
      name="email"
      type="email"
      autoComplete="email"
      required
      aria-describedby={error ? "email-error" : undefined}
      aria-invalid={error ? "true" : "false"}
      className="w-full rounded-xl bg-neutral-900 border border-neutral-800 px-4 py-3 text-sm text-white placeholder-neutral-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition-all"
      placeholder="alex@company.com"
    />
  </div>
  {error && (
    <p id="email-error" role="alert" className="text-xs text-rose-400 flex items-center gap-1 mt-1">
      <AlertCircleIcon className="w-3.5 h-3.5" />
      {error}
    </p>
  )}
</div>
```

---

## 2. Validation Timing Discipline

1. **Do NOT validate on every keystroke**: Shouting errors at the user while they are midway through typing an email address causes severe cognitive friction.
2. **Validate on `onBlur`**: Trigger validation only after the user leaves the input field.
3. **Clear errors on `onChange`**: As soon as the user starts correcting an invalid field, immediately remove the red error styling.

---

## 3. Accessible Loading Buttons (Zero Layout Jump)

When a form is submitted, the button must disable duplicate clicks and display a progress spinner **without shrinking or changing its dimensions**:

```tsx
export function LoadingButton({ 
  isLoading, 
  children, 
  ...props 
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { isLoading?: boolean }) {
  return (
    <button
      {...props}
      disabled={isLoading || props.disabled}
      className={`relative inline-flex items-center justify-center h-12 px-6 rounded-full font-medium text-sm transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-500/25 ${props.className || ''}`}
    >
      {/* Invisible content preserves exact width and height */}
      <span className={isLoading ? "invisible" : "visible flex items-center gap-2"}>
        {children}
      </span>
      {/* Center Spinner overlay */}
      {isLoading && (
        <span className="absolute inset-0 flex items-center justify-center">
          <svg className="animate-spin h-5 w-5 text-white" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
        </span>
      )}
    </button>
  );
}
```

---

## 4. Modal Dialog Focus Management

Modal dialogs must satisfy three non-negotiable accessibility behaviors:
1. **Focus Trapping**: Pressing `Tab` must cycle focus strictly within the dialog elements. Focus must never escape to background page elements.
2. **Escape Key Handling**: Pressing `Esc` must immediately dismiss the modal.
3. **Focus Restoration**: Upon dismissal, focus must return precisely to the button that originally triggered the modal.

*Recommended implementation*: Rely on `@radix-ui/react-dialog` or headless primitives that guarantee compliant focus trapping out-of-the-box.
