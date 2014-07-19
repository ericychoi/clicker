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
  private int loudClickSid;
  private SoundPool clickSoundPool;

  final private AtomicBoolean isRunning;
  final private AtomicInteger beatCounter;

  final private AtomicInteger tempo;
  final private TextView tempoView;

  final private AtomicInteger beatsPerMeasure;
  final private TextView BPMView;
  final private TextView BPMLabelView;

  public boolean isRunning() {
    return isRunning.get();
  }

  public Metronome(ImageView leftCircle, ImageView rightCircle, ImageView needle, SoundPool sp,
                   int loudClickSid, int clickSid, int initialTempo, TextView tempoView,
                   int bpm, TextView BPMView, TextView BPMLabelView) {
    this.leftCircle = leftCircle;
    this.leftCircleFadeOut = AnimationUtils.loadAnimation(leftCircle.getContext(), R.anim.circle_fade_out);
    this.rightCircle = rightCircle;
    this.rightCircleFadeOut = AnimationUtils.loadAnimation(rightCircle.getContext(), R.anim.circle_fade_out);

    this.needle = needle;
    this.needleTurnR = AnimationUtils.loadAnimation(needle.getContext(), R.anim.needle_rotate);
    this.needleTurnL = AnimationUtils.loadAnimation(needle.getContext(), R.anim.needle_rotate_reverse);

    this.clickSoundPool = sp;
    this.clickSoundId = clickSid;
    this.loudClickSid = loudClickSid;

    beatCounter = new AtomicInteger(0);
    isRunning = new AtomicBoolean(false);

    tempo = new AtomicInteger(initialTempo);
    this.tempoView = tempoView;
    updateTempoView();

    beatsPerMeasure = new AtomicInteger(bpm);
    this.BPMView = BPMView;
    this.BPMLabelView = BPMLabelView;
    updateBPMView();
  }

  void start(final View v) {
    final AtomicInteger interval = new AtomicInteger(computeInterval()); // interval in ms
    Log.v("metronome", "starting with interval: " + interval.get());

    //TODO factor out this Runnable
    final Runnable clicker = new Runnable() {
      @Override
      public void run() {
        final ImageView circle;
        final Animation circleAnimation;
        final Animation needleAnimation;
        final boolean isBeginning;
        final int beat;

        synchronized (beatCounter) {
          beat = beatCounter.getAndIncrement();
          isBeginning = beat == 0;
          if (beat % 2 == 0) {
            circle = leftCircle;
            circleAnimation = leftCircleFadeOut;
            needleAnimation = needleTurnR;
          }
          else {
            circle = rightCircle;
            circleAnimation = rightCircleFadeOut;
            needleAnimation = needleTurnL;
          }
          if (beat == beatsPerMeasure.get() - 1) beatCounter.set(0);
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
            if (isBeginning) clickSoundPool.play(loudClickSid, 1.0f, 1.0f, 1, 0, 1.0f);
            else clickSoundPool.play(clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            updateBPMView(beat + 1);
          }});
      }
    };
    setBPMLabel(false);
    this.clickerHandle = scheduler.scheduleAtFixedRate(clicker, 0, interval.get(), TimeUnit.MILLISECONDS);
  }

  void pause() {
    Log.v("metronome", "pause!");
    if (clickerHandle != null) clickerHandle.cancel(true);
    stopAutoMode();
    updateBPMView();
    setBPMLabel(true);
  }

  void startOrPause(View v) {
    if (isRunning.getAndSet(false)) {
      pause();
      beatCounter.set(0);
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

  void updateTempoTo(View v, int tempo) {
    if (this.isAutoRunning()) stopAutoMode();
    setTempo(tempo);
    if (this.isRunning()) pause();
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
    if (autoHandle != null) autoHandle.cancel(true);
  }

  void restart(View v) {
    pause();
    start(v);
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

  void updateBPMView() {
    this.BPMView.setText(this.beatsPerMeasure.get() + "");
  }

  void updateBPMView(int i) {
    this.BPMView.setText(i + "");
  }

  void setBPMLabel(boolean isBPM) {
    if (isBPM) BPMLabelView.setText(R.string.BPM);
    else BPMLabelView.setText(R.string.beat);
  }

  void toggleBeat(View v) {
    pause();
    if (beatsPerMeasure.incrementAndGet() > 8) beatsPerMeasure.set(2);
    updateBPMView();
    Log.v("metronome", "current bpm: " + beatsPerMeasure.get());
  }

}

