package com.ericchoi.clicker;

import android.app.Activity;
import android.media.SoundPool;
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
  private ScheduledFuture autoHandle;

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
  final private TextView tempoView;

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

  void start(final View v) {
    final AtomicInteger interval = new AtomicInteger(computeInterval()); // interval in ms
    Log.v("metronome", "starting with interval: " + interval.get());

    //TODO factor out this Runnable
    final Runnable clicker = new Runnable() {
      @Override
      public void run() {
        //Log.v("metronome", "click!");
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

  void pause(final View v) {
    Log.v("metronome", "pause!");
    if (clickerHandle != null) {
      clickerHandle.cancel(true);
    }
  }

  void startOrStopMetronome(View v) {
    if (isRunning.getAndSet(false)) {
      pause(v);
      isLeftsTurn.set(true);
    } else {
      isRunning.set(true);
      start(v);
    }
  }

  void increaseTempo(View v) {
    if (this.isAutoRunning()) return; // don't increase again if auto incrementing
    this.tempo.getAndIncrement();
    if (this.isRunning()) restart(v);
    updateTempoView();
  }

  void decreaseTempo(View v) {
    if (this.isAutoRunning()) return; // don't increase again if auto incrementing
    this.tempo.getAndDecrement();
    if (this.isRunning()) restart(v);
    updateTempoView();
  }

  boolean isAutoRunning() {
    return this.autoHandle != null && !this.autoHandle.isCancelled();
  }

  // spins up a thread that auto-increments (or decrements) the tempo display
  void startAutoMode(final View v, final boolean isIncrement) {
    //TODO make this configurable
    final int autoInterval = 50;
    if (autoHandle != null && !autoHandle.isCancelled()) {
      stopAutoMode();
    }

    final Runnable autoUpdater = new Runnable() {
      @Override
      public void run() {
        //Log.v("metronome", "auto update!");

        Activity a = (Activity)v.getContext();
        a.runOnUiThread( new Runnable() {
          @Override
          public void run() {
            if (isIncrement) tempo.getAndIncrement();
            else tempo.getAndDecrement();
            updateTempoView();
          }
        });
      }
    };

    this.autoHandle = scheduler.scheduleAtFixedRate(autoUpdater, 0, autoInterval, TimeUnit.MILLISECONDS);
  }

  void stopAutoMode() {
    Log.v("metronome", "auto stop! tempo:" + this.tempo.get());
    if (autoHandle != null) {
      autoHandle.cancel(true);
    }
  }

  void restart(View v) {
    pause(v);
    start(v);
  }

  void stop() {
    //TODO
    Log.v("metronome", "stop called");
    //scheduler.shutdown();
  }

  // computes timer interval in milliseconds from tempo (BPM)
  int computeInterval() {
    return 60 * 1000 / this.tempo.get();
  }

  void updateTempoView() {
    this.tempoView.setText(this.tempo.get() + "");
  }

  void updateTempoView(int i) {
    this.tempoView.setText(i + "");
  }

  void setTempo(int i) {
    this.tempo.set(i);
  }

}

