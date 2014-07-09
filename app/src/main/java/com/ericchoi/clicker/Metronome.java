package com.ericchoi.clicker;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Metronome {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(100);
  private ImageView leftCircle;
  private Animation leftCircleFadeOut;
  private ImageView rightCircle;
  private Animation rightCircleFadeOut;
  private ImageView needle;
  private Animation needleTurn;
  final private AtomicBoolean isLeftsTurn;
  private int clickSoundId;
  private SoundPool clickSoundPool;

  public Metronome(ImageView leftCircle, ImageView rightCircle, ImageView needle, SoundPool sp, int clickSoundId) {
    this.leftCircle = leftCircle;
    this.leftCircleFadeOut = AnimationUtils.loadAnimation(leftCircle.getContext(), R.anim.circle_fade_out);
    this.rightCircle = rightCircle;
    this.rightCircleFadeOut = AnimationUtils.loadAnimation(rightCircle.getContext(), R.anim.circle_fade_out);
    this.needle = needle;
    this.needleTurn = AnimationUtils.loadAnimation(needle.getContext(), R.anim.needle_rotate);
    this.clickSoundPool = sp;
    this.clickSoundId = clickSoundId;

    isLeftsTurn = new AtomicBoolean(true);
  }

  void startMetronome(final View v) {
    final Runnable clicker = new Runnable() {
      @Override
      public void run() {
        Log.v("metronome", "click!");
        final ImageView circle;
        final Animation circleAnimation;
        synchronized (isLeftsTurn) {
          if (isLeftsTurn.get()) {
            circle = leftCircle;
            circleAnimation = leftCircleFadeOut;
            isLeftsTurn.set(false);
          }
          else {
            circle = rightCircle;
            circleAnimation = rightCircleFadeOut;
            isLeftsTurn.set(true);
          }
        }
        Activity a = (Activity)v.getContext();
        a.runOnUiThread( new Runnable() {
          @Override
          public void run() {
            // not sure why, but can't rely on fillBefore on animation for this
            circle.setAlpha(1.0f);
            circle.startAnimation(circleAnimation);
            //TODO make this alternate
            needleTurn.setDuration(600);
            needle.startAnimation(needleTurn);
            clickSoundPool.play(clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            }
        });
      }
    };

    final ScheduledFuture clickerHandle = scheduler.scheduleAtFixedRate(clicker, 0, 600, TimeUnit.MILLISECONDS);
    scheduler.schedule(new Runnable() {
      public void run() {
        //    clickerHandle.cancel(true);
        Log.v("metronome", "cancel!");
      }
    }, 5, TimeUnit.SECONDS);
  }
}
