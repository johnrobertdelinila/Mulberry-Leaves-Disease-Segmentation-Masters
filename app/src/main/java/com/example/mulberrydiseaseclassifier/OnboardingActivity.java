package com.example.mulberrydiseaseclassifier;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

/**
 * Elegant onboarding activity showing introductory slides for first-time users.
 * Features smooth animations and modern design.
 */
public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private OnboardingAdapter adapter;
    private MaterialButton continueButton;
    private TextView skipButton;
    private LinearLayout dotsIndicator;
    private View[] dots;

    private int[] layouts = {
        R.layout.onboarding_slide_1,
        R.layout.onboarding_slide_2,
        R.layout.onboarding_slide_3
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.onboarding_pager);
        continueButton = findViewById(R.id.continue_button);
        skipButton = findViewById(R.id.skip_button);
        dotsIndicator = findViewById(R.id.dots_indicator);

        // Setup adapter
        adapter = new OnboardingAdapter(layouts);
        viewPager.setAdapter(adapter);

        // Add smooth page transformer
        viewPager.setPageTransformer(new DepthPageTransformer());

        // Setup dots indicator
        setupDotsIndicator();
        updateDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);

                // Change button text on last slide with animation
                if (position == layouts.length - 1) {
                    animateButtonText(getString(R.string.onboarding_get_started));
                    // Hide skip button on last page
                    skipButton.animate().alpha(0f).setDuration(200).start();
                } else {
                    animateButtonText(getString(R.string.onboarding_continue));
                    skipButton.animate().alpha(1f).setDuration(200).start();
                }
            }
        });

        // Continue button click
        continueButton.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < layouts.length - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        // Skip button click
        skipButton.setOnClickListener(v -> finishOnboarding());
    }

    /**
     * Setup the dots indicator views
     */
    private void setupDotsIndicator() {
        dots = new View[layouts.length];
        dotsIndicator.removeAllViews();

        for (int i = 0; i < layouts.length; i++) {
            dots[i] = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(8), dpToPx(8)
            );
            params.setMargins(dpToPx(6), 0, dpToPx(6), 0);
            dots[i].setLayoutParams(params);
            dots[i].setBackground(ContextCompat.getDrawable(this, R.drawable.dot_inactive));
            dotsIndicator.addView(dots[i]);
        }
    }

    /**
     * Update dots with smooth animation
     */
    private void updateDots(int position) {
        for (int i = 0; i < dots.length; i++) {
            if (i == position) {
                // Animate to active state
                animateDotToActive(dots[i]);
            } else {
                // Animate to inactive state
                animateDotToInactive(dots[i]);
            }
        }
    }

    /**
     * Animate dot to active state (elongated pill)
     */
    private void animateDotToActive(View dot) {
        dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_active));

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(dot, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(dot, "scaleY", 0.8f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(200);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();

        // Update layout params for wider dot
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
        params.width = dpToPx(24);
        params.height = dpToPx(8);
        dot.setLayoutParams(params);
    }

    /**
     * Animate dot to inactive state (small circle)
     */
    private void animateDotToInactive(View dot) {
        dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_inactive));

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(200);
        animatorSet.start();

        // Update layout params for circular dot
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
        params.width = dpToPx(8);
        params.height = dpToPx(8);
        dot.setLayoutParams(params);
    }

    /**
     * Animate button text change
     */
    private void animateButtonText(String newText) {
        continueButton.animate()
            .alpha(0.7f)
            .setDuration(100)
            .withEndAction(() -> {
                continueButton.setText(newText);
                continueButton.animate()
                    .alpha(1f)
                    .setDuration(100)
                    .start();
            })
            .start();
    }

    /**
     * Mark onboarding as seen and navigate to main activity
     */
    private void finishOnboarding() {
        AppPreferences.setOnboardingSeen(this);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Custom page transformer for smooth depth effect
     */
    private static class DepthPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        @Override
        public void transformPage(View page, float position) {
            int pageWidth = page.getWidth();

            if (position < -1) {
                // Page is off-screen to the left
                page.setAlpha(0f);
            } else if (position <= 0) {
                // Page is moving out to the left or is the current page
                page.setAlpha(1f);
                page.setTranslationX(0f);
                page.setTranslationZ(0f);
                page.setScaleX(1f);
                page.setScaleY(1f);
            } else if (position <= 1) {
                // Page is moving in from the right
                page.setAlpha(1 - position);
                page.setTranslationX(pageWidth * -position);
                page.setTranslationZ(-1f);

                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);
            } else {
                // Page is off-screen to the right
                page.setAlpha(0f);
            }
        }
    }
}
