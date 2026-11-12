package com.breathe.app;

public class Exercise {
    public final String title, label, session;
    public final int[] pattern;      // inhale, hold, exhale, hold (0 = skip)
    public final int icon, iconTint, cardColor; // iconTint 0 = no tint

    public Exercise(String title, String label, String session, int[] pattern,
                    int icon, int iconTint, int cardColor) {
        this.title = title; this.label = label; this.session = session;
        this.pattern = pattern; this.icon = icon; this.iconTint = iconTint; this.cardColor = cardColor;
    }

    public static final Exercise[] ALL = {
        new Exercise("Relax", "4-4-4-4", "Relax Breathing", new int[]{4, 4, 4, 4},
                R.drawable.ic_lotus, 0, R.color.card_blue),
        new Exercise("Sleep Better", "4-7-8", "Sleep Breathing", new int[]{4, 7, 8, 0},
                R.drawable.ic_moon, R.color.purple_primary, R.color.card_blue),
        new Exercise("Reduce Stress", "Box Breathing", "Stress Breathing", new int[]{4, 4, 4, 4},
                R.drawable.ic_heart, R.color.orange, R.color.card_peach),
        new Exercise("Boost Focus", "4-4-4-4", "Focus Breathing", new int[]{4, 4, 4, 4},
                R.drawable.ic_target, R.color.green, R.color.card_mint),
    };
}
