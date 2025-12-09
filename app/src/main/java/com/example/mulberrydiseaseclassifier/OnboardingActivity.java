package com.example.mulberrydiseaseclassifier;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

/**
 * Onboarding activity showing introductory slides for first-time users.
 * Uses ViewPager2 for smooth slide transitions.
 */
public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private OnboardingAdapter adapter;
    private MaterialButton continueButton;
    private TextView skipButton;
    private LinearLayout dotsIndicator;

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

        // Setup dots indicator
        addDotsIndicator(0);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                addDotsIndicator(position);

                // Change button text on last slide
                if (position == layouts.length - 1) {
                    continueButton.setText(R.string.onboarding_get_started);
                } else {
                    continueButton.setText(R.string.onboarding_continue);
                }
            }
        });

        // Continue button click
        continueButton.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < layouts.length - 1) {
                // Go to next slide
                viewPager.setCurrentItem(current + 1);
            } else {
                // Last slide, finish onboarding
                finishOnboarding();
            }
        });

        // Skip button click
        skipButton.setOnClickListener(v -> finishOnboarding());
    }

    /**
     * Add dots indicator for current page
     */
    private void addDotsIndicator(int position) {
        dotsIndicator.removeAllViews();

        for (int i = 0; i < layouts.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(8), dpToPx(8)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);

            if (i == position) {
                dot.setBackgroundResource(R.drawable.circle);
                dot.setBackgroundTintList(
                    getResources().getColorStateList(R.color.sage_green, getTheme())
                );
            } else {
                dot.setBackgroundResource(R.drawable.circle);
                dot.setBackgroundTintList(
                    getResources().getColorStateList(R.color.text_tertiary, getTheme())
                );
            }

            dotsIndicator.addView(dot);
        }
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
}
