/**
 * Temporary section stub so the authenticated shell is navigable while
 * CAT-004 builds the real catalog and inventory screens. Removed by CAT-004.
 */
export function SectionPlaceholder({ title }: { title: string }) {
  return (
    <section aria-labelledby="section-title">
      <h1 id="section-title" className="text-2xl font-bold tracking-tight">
        {title}
      </h1>
      <p className="mt-2 text-sm text-muted-foreground">
        This section arrives with CAT-004 — Admin catalog and inventory screens.
      </p>
    </section>
  );
}
