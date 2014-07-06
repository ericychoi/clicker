package com.ericchoi.clicker;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Metronome {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(100);
    private ImageView circle;
    private Animation circleFadeOut;

    public Metronome(ImageView circle) {
        this.circle = circle;
        this.circleFadeOut = AnimationUtils.loadAnimation(circle.getContext(), R.anim.circle_fade_out);
    }

    void startMetronome(final View v) {
        final Runnable clicker = new Runnable() {
            @Override
            public void run() {
                Log.v("metronome", "click!");
                Activity a = (Activity)v.getContext();
                a.runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        circle.startAnimation(circleFadeOut);
                    }
                });
            }
        };

        final ScheduledFuture clickerHandle = scheduler.scheduleAtFixedRate(clicker, 0, 1, TimeUnit.SECONDS);
        scheduler.schedule(new Runnable() {
            public void run() {
                //    clickerHandle.cancel(true);
                Log.v("metronome", "cancel!");
            }
        }, 5, TimeUnit.SECONDS);
    }
}

