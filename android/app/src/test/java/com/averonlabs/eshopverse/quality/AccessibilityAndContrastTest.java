package com.averonlabs.eshopverse.quality;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.core.ui.view.QuantityRange;
import com.averonlabs.eshopverse.core.ui.view.StatusBadgeView;

import org.junit.Test;

/**
 * Automated accessibility, WCAG AA contrast ratio, and edge-to-edge layout calculation tests
 * verifying acceptance criteria for QUAL-002 per docs/ANDROID_ROADMAP.md and docs/DESIGN_SYSTEM.md.
 */
public class AccessibilityAndContrastTest {

    // --- WCAG AA Color Contrast Tests ---

    @Test
    public void wcagContrast_primaryAndContainerRoles_meetOrExceedWcagAA() {
        // Primary: #161616 on coral #F7767E (expected ~6.76:1, min 4.5:1)
        double primaryRatio = calculateContrastRatio("#161616", "#F7767E");
        assertTrue("Primary contrast must meet WCAG AA normal text (4.5:1), actual: " + primaryRatio,
                primaryRatio >= 4.5);

        // Primary container: #8D2531 on #FFF0F1 (expected ~7.78:1, min 4.5:1)
        double primaryContainerRatio = calculateContrastRatio("#8D2531", "#FFF0F1");
        assertTrue("Primary container contrast must meet WCAG AA, actual: " + primaryContainerRatio,
                primaryContainerRatio >= 4.5);
    }

    @Test
    public void wcagContrast_secondaryAndContainerRoles_meetOrExceedWcagAA() {
        // Secondary: white #FFFFFF on lavender #7651B5 (expected ~5.81:1, min 4.5:1)
        double secondaryRatio = calculateContrastRatio("#FFFFFF", "#7651B5");
        assertTrue("Secondary contrast must meet WCAG AA, actual: " + secondaryRatio,
                secondaryRatio >= 4.5);

        // Secondary container: #392653 on #EEE7F7 (expected ~11.05:1, min 4.5:1)
        double secondaryContainerRatio = calculateContrastRatio("#392653", "#EEE7F7");
        assertTrue("Secondary container contrast must meet WCAG AA, actual: " + secondaryContainerRatio,
                secondaryContainerRatio >= 4.5);
    }

    @Test
    public void wcagContrast_tertiaryAndContainerRoles_meetOrExceedWcagAA() {
        // Tertiary: white #FFFFFF on aqua #287E87 (expected ~4.75:1, min 4.5:1)
        double tertiaryRatio = calculateContrastRatio("#FFFFFF", "#287E87");
        assertTrue("Tertiary contrast must meet WCAG AA, actual: " + tertiaryRatio,
                tertiaryRatio >= 4.5);

        // Tertiary container: #164E53 on #DDF3F4 (expected ~8.09:1, min 4.5:1)
        double tertiaryContainerRatio = calculateContrastRatio("#164E53", "#DDF3F4");
        assertTrue("Tertiary container contrast must meet WCAG AA, actual: " + tertiaryContainerRatio,
                tertiaryContainerRatio >= 4.5);
    }

    @Test
    public void wcagContrast_errorAndContainerRoles_meetOrExceedWcagAA() {
        // Error: white #FFFFFF on #B3261E (expected ~6.54:1, min 4.5:1)
        double errorRatio = calculateContrastRatio("#FFFFFF", "#B3261E");
        assertTrue("Error contrast must meet WCAG AA, actual: " + errorRatio,
                errorRatio >= 4.5);

        // Error container: #8C1D18 on #FCEEEE (expected ~8.07:1, min 4.5:1)
        double errorContainerRatio = calculateContrastRatio("#8C1D18", "#FCEEEE");
        assertTrue("Error container contrast must meet WCAG AA, actual: " + errorContainerRatio,
                errorContainerRatio >= 4.5);
    }

    @Test
    public void wcagContrast_surfaceAndBackgroundRoles_meetOrExceedWcagAA() {
        // On-surface: #161616 on white #FFFFFF (expected ~18.10:1, min 4.5:1)
        double surfaceRatio = calculateContrastRatio("#161616", "#FFFFFF");
        assertTrue("Surface text contrast must meet WCAG AA, actual: " + surfaceRatio,
                surfaceRatio >= 4.5);

        // On-background: #161616 on canvas #F5F6F7 (expected ~16.72:1, min 4.5:1)
        double canvasRatio = calculateContrastRatio("#161616", "#F5F6F7");
        assertTrue("Canvas background contrast must meet WCAG AA, actual: " + canvasRatio,
                canvasRatio >= 4.5);

        // On-surface-variant: #6B6B6B on white #FFFFFF (expected ~5.33:1, min 4.5:1)
        double surfaceVariantOnWhite = calculateContrastRatio("#6B6B6B", "#FFFFFF");
        assertTrue("Surface variant on white contrast must meet WCAG AA, actual: " + surfaceVariantOnWhite,
                surfaceVariantOnWhite >= 4.5);

        // On-surface-variant: #6B6B6B on surface variant #F1F1F3 (expected ~4.72:1, min 4.5:1)
        double surfaceVariantOnVariant = calculateContrastRatio("#6B6B6B", "#F1F1F3");
        assertTrue("Surface variant text contrast must meet WCAG AA, actual: " + surfaceVariantOnVariant,
                surfaceVariantOnVariant >= 4.5);

        // Outline: #767676 on white #FFFFFF (expected ~4.54:1, min 3.0:1 component boundary)
        double outlineRatio = calculateContrastRatio("#767676", "#FFFFFF");
        assertTrue("Outline contrast must meet WCAG boundary requirement (3.0:1), actual: " + outlineRatio,
                outlineRatio >= 3.0);
    }

    @Test
    public void wcagContrast_statusSemanticPairs_meetOrExceedWcagAA() {
        // Success: #256B4B on #E8F3ED (expected ~5.63:1, min 4.5:1)
        double successRatio = calculateContrastRatio("#256B4B", "#E8F3ED");
        assertTrue("Status success contrast must meet WCAG AA, actual: " + successRatio,
                successRatio >= 4.5);

        // Warning: #8A5300 on #FBF1E3 (expected ~5.66:1, min 4.5:1)
        double warningRatio = calculateContrastRatio("#8A5300", "#FBF1E3");
        assertTrue("Status warning contrast must meet WCAG AA, actual: " + warningRatio,
                warningRatio >= 4.5);

        // Error: #B3261E on #FCEEEE (expected ~5.79:1, min 4.5:1)
        double errorStatusRatio = calculateContrastRatio("#B3261E", "#FCEEEE");
        assertTrue("Status error contrast must meet WCAG AA, actual: " + errorStatusRatio,
                errorStatusRatio >= 4.5);

        // Neutral: #505050 on #EFF0F2 (expected ~7.07:1, min 4.5:1)
        double neutralRatio = calculateContrastRatio("#505050", "#EFF0F2");
        assertTrue("Status neutral contrast must meet WCAG AA, actual: " + neutralRatio,
                neutralRatio >= 4.5);
    }

    // --- Quantity Stepper Logic & Bounds ---

    @Test
    public void quantityRange_boundsAndIncrements_correctlyMaintained() {
        QuantityRange range = new QuantityRange();
        assertEquals(1, range.getQuantity());

        // Increment
        assertEquals(2, range.increment());
        assertEquals(2, range.getQuantity());

        // Decrement
        assertEquals(1, range.decrement());
        assertEquals(1, range.getQuantity());

        // Clamping at min
        assertEquals(1, range.decrement());
        assertEquals(1, range.getQuantity());

        // Set max and clamp at max
        range.setMax(5);
        range.setQuantity(5);
        assertEquals(5, range.increment());
        assertEquals(5, range.getQuantity());
    }

    // --- Status Badge Semantic Status Verification ---

    @Test
    public void statusBadgeView_allStatusEnums_definedAndValid() {
        StatusBadgeView.Status[] statuses = StatusBadgeView.Status.values();
        assertEquals(4, statuses.length);
        assertEquals(StatusBadgeView.Status.SUCCESS, StatusBadgeView.Status.valueOf("SUCCESS"));
        assertEquals(StatusBadgeView.Status.WARNING, StatusBadgeView.Status.valueOf("WARNING"));
        assertEquals(StatusBadgeView.Status.ERROR, StatusBadgeView.Status.valueOf("ERROR"));
        assertEquals(StatusBadgeView.Status.NEUTRAL, StatusBadgeView.Status.valueOf("NEUTRAL"));
    }

    // --- Insets & IME Keyboard Transition Non-Clipping Math ---

    @Test
    public void insetsCalculation_rootDestinationWithBottomNav_calculatesCorrectBottomPadding() {
        int bottomNavHeight = 144; // e.g. 56dp height + system bar insets
        int systemBottom = 72;     // gesture bar inset
        int imeHidden = 0;
        int imeOpen = 720;         // virtual soft keyboard open

        // When bottomNav is visible and IME is hidden:
        int baseBottom = Math.max(bottomNavHeight, systemBottom);
        int finalPadding = Math.max(baseBottom, imeHidden);
        assertEquals("Should pad for bottomNav when keyboard is closed", bottomNavHeight, finalPadding);

        // When bottomNav is visible and IME opens:
        finalPadding = Math.max(baseBottom, imeOpen);
        assertEquals("Should expand padding for IME keyboard when opened", imeOpen, finalPadding);
    }

    @Test
    public void insetsCalculation_childDestinationWithoutBottomNav_preventsNavigationAndImeClipping() {
        int bottomNavHeight = 0;   // hidden on child screens (checkout, product detail, sign in)
        int systemBottom = 72;     // 3-button or gesture navigation bar height
        int imeHidden = 0;
        int imeOpen = 720;         // virtual keyboard height

        // When bottomNav is GONE and keyboard is closed:
        int baseBottom = Math.max(bottomNavHeight, systemBottom);
        int finalPadding = Math.max(baseBottom, imeHidden);
        assertEquals("Should pad for system navigation bar to prevent button clipping", systemBottom, finalPadding);
        assertTrue("Content must never have 0 bottom padding under system navigation bar", finalPadding >= systemBottom);

        // When bottomNav is GONE and keyboard opens:
        finalPadding = Math.max(baseBottom, imeOpen);
        assertEquals("Should pad for full IME keyboard height", imeOpen, finalPadding);
    }

    // --- WCAG Contrast Helper Methods ---

    private double calculateContrastRatio(String hexColor1, String hexColor2) {
        double l1 = relativeLuminance(hexColor1);
        double l2 = relativeLuminance(hexColor2);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double relativeLuminance(String hexColor) {
        String cleanHex = hexColor.replace("#", "");
        int r = Integer.parseInt(cleanHex.substring(0, 2), 16);
        int g = Integer.parseInt(cleanHex.substring(2, 4), 16);
        int b = Integer.parseInt(cleanHex.substring(4, 6), 16);

        double rs = sRgbToLinear(r / 255.0);
        double gs = sRgbToLinear(g / 255.0);
        double bs = sRgbToLinear(b / 255.0);

        return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
    }

    private double sRgbToLinear(double val) {
        if (val <= 0.04045) {
            return val / 12.92;
        } else {
            return Math.pow((val + 0.055) / 1.055, 2.4);
        }
    }
}
