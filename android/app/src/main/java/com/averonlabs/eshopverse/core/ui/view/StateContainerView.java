package com.averonlabs.eshopverse.core.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

/** Hosts content and consistent loading, empty, and retryable error states. */
public final class StateContainerView extends FrameLayout {
    public enum State { CONTENT, LOADING, EMPTY, ERROR }

    private final FrameLayout contentHost;
    private final LinearLayout messagePanel;
    private final CircularProgressIndicator progress;
    private final MaterialTextView message;
    private final MaterialButton retry;
    @Nullable private View contentView;
    @Nullable private Runnable retryAction;

    public StateContainerView(Context context) {
        this(context, null);
    }

    public StateContainerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StateContainerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        contentHost = new FrameLayout(context);
        addView(contentHost, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        messagePanel = new LinearLayout(context);
        messagePanel.setOrientation(LinearLayout.VERTICAL);
        messagePanel.setGravity(android.view.Gravity.CENTER);
        messagePanel.setPadding(getResources().getDimensionPixelSize(R.dimen.spacing_lg),
                getResources().getDimensionPixelSize(R.dimen.spacing_lg),
                getResources().getDimensionPixelSize(R.dimen.spacing_lg),
                getResources().getDimensionPixelSize(R.dimen.spacing_lg));
        progress = new CircularProgressIndicator(context);
        progress.setIndeterminate(true);
        message = new MaterialTextView(context);
        message.setTextAppearance(R.style.TextAppearance_EShopVerse_Body);
        retry = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        retry.setMinWidth(getResources().getDimensionPixelSize(R.dimen.touch_target_min));
        retry.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target_min));
        retry.setOnClickListener(view -> {
            if (retryAction != null) retryAction.run();
        });
        messagePanel.addView(progress);
        messagePanel.addView(message, centeredLayoutParams());
        messagePanel.addView(retry, centeredLayoutParams());
        addView(messagePanel, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setState(State.CONTENT, null, null, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child != contentHost && child != messagePanel) {
                removeView(child);
                setContentView(child);
            }
        }
    }

    public void setContentView(View view) {
        if (view == null) return;
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        if (contentView != null) contentHost.removeView(contentView);
        contentView = view;
        contentHost.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void setState(State state, @Nullable CharSequence messageText,
            @Nullable CharSequence retryLabel, @Nullable Runnable onRetry) {
        retryAction = onRetry;
        contentHost.setVisibility(state == State.CONTENT ? VISIBLE : GONE);
        messagePanel.setVisibility(state == State.CONTENT ? GONE : VISIBLE);
        progress.setVisibility(state == State.LOADING ? VISIBLE : GONE);
        message.setVisibility(state == State.EMPTY || state == State.ERROR ? VISIBLE : GONE);
        retry.setVisibility(state == State.ERROR && onRetry != null ? VISIBLE : GONE);
        if (state != State.CONTENT) {
            messagePanel.bringToFront();
        }
        if (messageText != null) message.setText(messageText);
        if (retryLabel != null) retry.setText(retryLabel);
        setImportantForAccessibility(state == State.LOADING
                ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    public MaterialButton getRetryButton() {
        return retry;
    }

    public MaterialTextView getMessageView() {
        return message;
    }

    public CircularProgressIndicator getProgressIndicator() {
        return progress;
    }

    private LinearLayout.LayoutParams centeredLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        return params;
    }
}
