package com.averonlabs.eshopverse.core.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.averonlabs.eshopverse.R;
import com.google.android.material.textview.MaterialTextView;

/** Text-labelled semantic status badge; colour never carries the status by itself. */
public final class StatusBadgeView extends MaterialTextView {
    public enum Status { SUCCESS, WARNING, ERROR, NEUTRAL }

    private Status currentStatus = Status.NEUTRAL;

    public StatusBadgeView(Context context) {
        this(context, null);
    }

    public StatusBadgeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public StatusBadgeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTextAppearance(R.style.TextAppearance_EShopVerse_Supporting);
        setBackgroundResource(R.drawable.bg_status_badge);
        setPadding(getResources().getDimensionPixelSize(R.dimen.spacing_sm),
                getResources().getDimensionPixelSize(R.dimen.spacing_xs),
                getResources().getDimensionPixelSize(R.dimen.spacing_sm),
                getResources().getDimensionPixelSize(R.dimen.spacing_xs));
        setStatus(Status.NEUTRAL);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        updateSemanticDescription();
    }

    public Status getStatus() {
        return currentStatus;
    }

    public void setStatus(Status status) {
        this.currentStatus = status != null ? status : Status.NEUTRAL;
        int foreground;
        int background;
        switch (this.currentStatus) {
            case SUCCESS:
                foreground = R.color.status_success;
                background = R.color.status_success_container;
                break;
            case WARNING:
                foreground = R.color.status_warning;
                background = R.color.status_warning_container;
                break;
            case ERROR:
                foreground = R.color.status_error;
                background = R.color.status_error_container;
                break;
            case NEUTRAL:
            default:
                foreground = R.color.status_neutral;
                background = R.color.status_neutral_container;
        }
        setTextColor(ContextCompat.getColor(getContext(), foreground));
        getBackground().setTint(ContextCompat.getColor(getContext(), background));
        updateSemanticDescription();
    }

    private void updateSemanticDescription() {
        CharSequence text = getText();
        if (text != null && text.length() > 0) {
            String label = text.toString().trim();
            if (!label.toLowerCase(java.util.Locale.US).startsWith("status")) {
                setContentDescription(getContext().getString(R.string.status_badge_description, label));
            } else {
                setContentDescription(label);
            }
        }
    }
}
