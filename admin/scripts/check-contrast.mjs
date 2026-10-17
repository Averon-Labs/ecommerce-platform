/**
 * Repeatable WCAG 2.2 contrast check for the admin light-theme tokens.
 *
 * Token values mirror `src/globals.css` `:root` (HSL triples, Tailwind style)
 * and the design-system status semantics rendered by `StatusBadge`. Run with
 * `npm run check:contrast`; exits non-zero when any pair falls below its
 * threshold (4.5:1 for normal text, 3:1 for focus indicators).
 */

const hsl = (h, s, l) => {
  const c = (1 - Math.abs((2 * l) / 100 - 1)) * (s / 100);
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
  const m = l / 100 - c / 2;
  const seg = Math.floor(h / 60) % 6;
  const rgb = [
    [c, x, 0],
    [x, c, 0],
    [0, c, x],
    [0, x, c],
    [x, 0, c],
    [c, 0, x],
  ][seg];
  return rgb.map((v) => Math.round((v + m) * 255));
};

const hex = (value) => {
  const v = value.replace('#', '');
  return [0, 2, 4].map((i) => parseInt(v.slice(i, i + 2), 16));
};

const tokens = {
  background: hsl(240, 10, 98),
  card: hsl(0, 0, 100),
  foreground: hsl(0, 0, 9),
  primary: hsl(356, 89, 72),
  primaryForeground: hsl(0, 0, 9),
  primaryStrong: hsl(354, 59.5, 45.5),
  secondary: hsl(262, 41, 52),
  secondaryForeground: hsl(0, 0, 100),
  mutedForeground: hsl(0, 0, 42),
  accent: hsl(186, 54, 34),
  accentForeground: hsl(0, 0, 100),
  destructive: hsl(0, 71, 41),
  destructiveForeground: hsl(0, 0, 100),
  ring: hsl(354, 59.5, 45.5),
  badgeSuccessFg: hex('#256B4B'),
  badgeSuccessBg: hex('#E8F3ED'),
  badgeWarningFg: hex('#8A5300'),
  badgeWarningBg: hex('#FBF1E3'),
  badgeErrorFg: hex('#B3261E'),
  badgeErrorBg: hex('#FCEEEE'),
  badgeNeutralFg: hex('#505050'),
  badgeNeutralBg: hex('#EFF0F2'),
};

const blend = (fg, bg, alpha) => fg.map((v, i) => Math.round(alpha * v + (1 - alpha) * bg[i]));

const TEXT = 4.5;
const GRAPHIC = 3;

const pairs = [
  [tokens.foreground, tokens.card, TEXT, 'foreground on card'],
  [tokens.foreground, tokens.background, TEXT, 'foreground on background'],
  [tokens.mutedForeground, tokens.card, TEXT, 'muted-foreground on card'],
  [tokens.mutedForeground, tokens.background, TEXT, 'muted-foreground on background'],
  [tokens.primaryForeground, tokens.primary, TEXT, 'primary-foreground on primary (filled buttons)'],
  [tokens.primaryStrong, tokens.card, TEXT, 'primary-strong text on card'],
  [tokens.primaryStrong, tokens.background, TEXT, 'primary-strong text on background'],
  [
    tokens.primaryStrong,
    blend(tokens.primaryStrong, tokens.card, 0.1),
    TEXT,
    'primary-strong text on active-nav (primary-strong/10 over card)',
  ],
  [tokens.secondaryForeground, tokens.secondary, TEXT, 'secondary-foreground on secondary'],
  [tokens.accentForeground, tokens.accent, TEXT, 'accent-foreground on accent'],
  [tokens.destructiveForeground, tokens.destructive, TEXT, 'destructive-foreground on destructive'],
  [tokens.badgeSuccessFg, tokens.badgeSuccessBg, TEXT, 'status success badge'],
  [tokens.badgeWarningFg, tokens.badgeWarningBg, TEXT, 'status warning badge'],
  [tokens.badgeErrorFg, tokens.badgeErrorBg, TEXT, 'status error badge'],
  [tokens.badgeNeutralFg, tokens.badgeNeutralBg, TEXT, 'status neutral badge'],
  [tokens.ring, tokens.card, GRAPHIC, 'focus ring on card'],
  [tokens.ring, tokens.background, GRAPHIC, 'focus ring on background'],
];

const luminance = (rgb) => {
  const [r, g, b] = rgb.map((v) => {
    const channel = v / 255;
    return channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};

const ratio = (a, b) => {
  const [light, dark] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (light + 0.05) / (dark + 0.05);
};

let failures = 0;
for (const [fg, bg, threshold, label] of pairs) {
  const value = ratio(fg, bg);
  const ok = value >= threshold;
  if (!ok) failures += 1;
  const status = ok ? 'PASS' : 'FAIL';
  console.log(`${status}  ${value.toFixed(2).padStart(5)}:1  (min ${threshold}:1)  ${label}`);
}

if (failures > 0) {
  console.error(`\n${failures} contrast check(s) failed.`);
  process.exit(1);
}
console.log(`\nAll ${pairs.length} contrast checks passed.`);
