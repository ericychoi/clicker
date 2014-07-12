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
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Metronome {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(100);
  private ScheduledFuture clickerHandle;

  private ImageView leftCircle;
  private Animation leftCircleFadeOut;
  private ImageView rightCircle;
  private Animation rightCircleFadeOut;
  private ImageView needle;
  private Animation needleTurnR;
  private Animation needleTurnL;

  private int clickSoundId;
  private SoundPool clickSoundPool;

  final private AtomicBoolean isRunning;
  final private AtomicBoolean isLeftsTurn;

  final private AtomicInteger tempo;
  private TextView tempoView;

  public boolean isRunning() {
    return isRunning.get();
  }

  public Metronome(ImageView leftCircle, ImageView rightCircle, ImageView needle, SoundPool sp,
                   int clickSoundId, int initialTempo, TextView tempoView) {
    this.leftCircle = leftCircle;
    this.leftCircleFadeOut = AnimationUtils.loadAnimation(leftCircle.getContext(), R.anim.circle_fade_out);
    this.rightCircle = rightCircle;
    this.rightCircleFadeOut = AnimationUtils.loadAnimation(rightCircle.getContext(), R.anim.circle_fade_out);

    this.needle = needle;
    this.needleTurnR = AnimationUtils.loadAnimation(needle.getContext(), R.anim.needle_rotate);
    this.needleTurnL = AnimationUtils.loadAnimation(needle.getContext(), R.anim.needle_rotate_reverse);

    this.clickSoundPool = sp;
    this.clickSoundId = clickSoundId;

    isLeftsTurn = new AtomicBoolean(true);
    isRunning = new AtomicBoolean(false);

    tempo = new AtomicInteger(initialTempo);
    this.tempoView = tempoView;
    updateTempoView();
  }

  void startMetronome(final View v) {
    final AtomicInteger interval = new AtomicInteger(computeInterval()); // interval in ms
    Log.v("metronome", "starting with interval: " + interval.get());

    //TODO factor out this Runnable
    final Runnable clicker = new Runnable() {
      @Override
      public void run() {
        Log.v("metronome", "click!");
        final ImageView circle;
        final Animation circleAnimation;
        final Animation needleAnimation;

        synchronized (isLeftsTurn) {
          if (isLeftsTurn.get()) {
            circle = leftCircle;
            circleAnimation = leftCircleFadeOut;
            needleAnimation = needleTurnR;
            isLeftsTurn.set(false);
          }
          else {
            circle = rightCircle;
            circleAnimation = rightCircleFadeOut;
            needleAnimation = needleTurnL;
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
            needleAnimation.setDuration(interval.get());
            needle.startAnimation(needleAnimation);
            clickSoundPool.play(clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            }
        });
      }
    };

    this.clickerHandle = scheduler.scheduleAtFixedRate(clicker, 0, interval.get(), TimeUnit.MILLISECONDS);
  }

  void stopMetronome(final View v) {
    Log.v("metronome", "stop!");
    if (clickerHandle != null) {
      clickerHandle.cancel(true);
    }
  }

  void startOrStopMetronome(View v) {
    if (isRunning.getAndSet(false)) {
      stopMetronome(v);
      isLeftsTurn.set(true);
    } else {
      isRunning.set(true);
      startMetronome(v);
    }
  }

  void increaseTempo(View v) {
    this.tempo.getAndIncrement();
    if (this.isRunning()) restart(v);
    updateTempoView();
  }

  void decreaseTempo(View v) {
    this.tempo.getAndDecrement();
    if (this.isRunning()) restart(v);
    updateTempoView();
  }

  void restart(View v) {
    stopMetronome(v);
    startMetronome(v);
  }

  // computes timer interval in milliseconds from tempo (BPM)
  int computeInterval() {
    return 60 * 1000 / this.tempo.get();
  }

  void updateTempoView() {
    this.tempoView.setText(this.tempo.get() + "");
  }
}

