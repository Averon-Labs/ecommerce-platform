# Rule 05: Motion, Haptics & Micro-Interactions

Static interfaces feel rigid and mechanical. Modern Dribbble interfaces feel alive because every user touch triggers **immediate, organic feedback** through spring physics and subtle scaling.

---

## 1. Physics-Based Spring Animations (AndroidX)

Never use linear interpolators for touch feedback. Use `androidx.dynamicanimation.animation.SpringAnimation` to simulate real-world physical springs.

### Java Spring Helper Utility
```java
package com.yourapp.ui.motion;

import android.view.View;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public final class SpringMotionUtils {

    /**
     * Creates an organic spring scale bounce on view touch/click
     */
    public static void applySpringTouchFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    animateSpring(v, DynamicAnimation.SCALE_X, 0.96f, SpringForce.STIFFNESS_MEDIUM, SpringForce.DAMPING_RATIO_LOW_BOUNCY);
                    animateSpring(v, DynamicAnimation.SCALE_Y, 0.96f, SpringForce.STIFFNESS_MEDIUM, SpringForce.DAMPING_RATIO_LOW_BOUNCY);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    animateSpring(v, DynamicAnimation.SCALE_X, 1.0f, SpringForce.STIFFNESS_LOW, SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                    animateSpring(v, DynamicAnimation.SCALE_Y, 1.0f, SpringForce.STIFFNESS_LOW, SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                    break;
            }
            return false; // Return false so OnClickListener still triggers!
        });
    }

    public static void animateSpring(View view, DynamicAnimation.ViewProperty property, float finalPosition, float stiffness, float dampingRatio) {
        SpringAnimation springAnim = new SpringAnimation(view, property);
        SpringForce force = new SpringForce(finalPosition);
        force.setStiffness(stiffness);
        force.setDampingRatio(dampingRatio);
        springAnim.setSpring(force);
        springAnim.start();
    }
}
```

---

## 2. Touch Feedback: Ripples & StateListDrawables

Never leave interactive views without touch states. Every card, button, and icon must announce its interactivity.

### Proper Ripple on MaterialCardView
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clickable="true"
    android:focusable="true"
    app:rippleColor="?attr/colorControlHighlight">
```

### Masked Ripple on Custom Views
When applying a ripple to a custom circular icon or rounded container:
```xml
<!-- res/drawable/bg_ripple_pill.xml -->
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?attr/colorControlHighlight">
    <item android:id="@android:id/mask">
        <shape android:shape="rectangle">
            <corners android:radius="24dp" />
            <solid android:color="@android:color/white" />
        </shape>
    </item>
</ripple>
```

---

## 3. High-Performance ViewPropertyAnimator

For smooth entrance reveals, always animate compositor-safe properties (`alpha`, `translationY`, `scaleX`, `scaleY`). Never animate layout dimensions (`width` or `height`):

```java
// Staggered Card Entrance Animation
public static void animateEntrance(@NonNull View view, int delayMs) {
    view.setAlpha(0f);
    view.setTranslationY(40f); // 40dp downward offset
    
    view.animate()
        .alpha(1.0f)
        .translationY(0f)
        .setDuration(450)
        .setStartDelay(delayMs)
        .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
        .withLayer() // Promotes to hardware GPU layer during animation
        .start();
}
```

---

## 4. Haptic Feedback Integration

Subtle tactile feedback turns a standard screen into a tactile instrument:

```java
public static void performTactileClick(@NonNull View view) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
    } else {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
    }
}
```
*Rule*: Attach this to primary CTA submissions, switch toggles, and deletion confirmations. Never attach haptics to continuous scrolling or high-frequency events.
