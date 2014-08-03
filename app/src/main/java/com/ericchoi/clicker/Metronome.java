package com.ericchoi.clicker;

import android.app.Activity;
import android.media.SoundPool;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
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

  private MenuItem playMenuItem;
  private MenuItem tempoMenuItem;

  private int clickSoundId;
  private int loudClickSid;
  private SoundPool clickSoundPool;

  final private AtomicBoolean isRunning = new AtomicBoolean(false);
  final private AtomicInteger beatCounter = new AtomicInteger(0);

  //TODO make initial value configurable
  final private AtomicInteger tempo = new AtomicInteger(100);
  private TextView tempoView;

  //TODO make initial value configurable
  final private AtomicInteger beatsPerMeasure = new AtomicInteger(4);
  private TextView BPMView;
  private TextView BPMLabelView;

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

    this.tempoView = tempoView;
    updateTempoView();

    this.BPMView = BPMView;
    this.BPMLabelView = BPMLabelView;
    updateBPMView();
  }

  public Metronome(SoundPool clickSoundPool, int clickSoundId, int loudClickSid) {
    this.clickSoundPool = clickSoundPool;
    this.clickSoundId = clickSoundId;
    this.loudClickSid = loudClickSid;
  }

  public void initView() {
    this.leftCircleFadeOut = AnimationUtils.loadAnimation(leftCircle.getContext(), R.anim.circle_fade_out);
    this.rightCircleFadeOut = AnimationUtils.loadAnimation(rightCircle.getContext(), R.anim.circle_fade_out);
    this.needleTurnR = AnimationUtils.loadAnimation(needle.getContext(), R.anim.needle_rotate);
    this.needleTurnL = AnimationUtils.loadAnimation(needle.getContext(), R.anim.needle_rotate_reverse);
    updateTempoView();
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
            playMenuItem.setIcon(R.drawable.ic_action_pause);
          }});
      }
    };
    setBPMLabel(false);
    this.clickerHandle = scheduler.scheduleAtFixedRate(clicker, 0, interval.get(), TimeUnit.MILLISECONDS);
  }

  public void pause() {
    Log.v("metronome", "pause!");
    if (clickerHandle != null) clickerHandle.cancel(true);
    stopAutoMode();
    updateBPMView();
    setBPMLabel(true);
    playMenuItem.setIcon(R.drawable.ic_action_play);
  }

  /* return true if started.  else false */
  public boolean startOrPause(View v) {
    if (isRunning.getAndSet(false)) {
      pause();
      beatCounter.set(0);
      return false;
    } else {
      isRunning.set(true);
      start(v);
      return true;
    }
  }

  public void increaseTempo(View v) {
    if (this.isAutoRunning()) return; // don't increase again if auto incrementing
    this.tempo.getAndIncrement();
    if (this.isRunning()) restart(v);
    updateTempoView();
  }

  public void decreaseTempo(View v) {
    if (this.isAutoRunning()) return; // don't increase again if auto incrementing
    this.tempo.getAndDecrement();
    if (this.isRunning()) restart(v);
    updateTempoView();
  }

  public void updateTempoTo(View v, int tempo) {
    if (this.isAutoRunning()) stopAutoMode();
    setTempo(tempo);
    if (this.isRunning()) pause();
    updateTempoView();
  }

  public boolean isAutoRunning() {
    return this.autoHandle != null && !this.autoHandle.isCancelled();
  }

  // spins up a thread that auto-increments (or decrements) the tempo display
  public void startAutoMode(final View v, final boolean isIncrement) {
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

  public void stopAutoMode() {
    Log.v("metronome", "auto stop! tempo:" + this.tempo.get());
    if (autoHandle != null) autoHandle.cancel(true);
  }

  public void restart(View v) {
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

  public void updateTempoMenuItem() {
    tempoMenuItem.setTitle(tempo.get() + "");
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

  public void toggleBeat(View v) {
    pause();
    if (beatsPerMeasure.incrementAndGet() > 8) beatsPerMeasure.set(2);
    updateBPMView();
    Log.v("metronome", "current bpm: " + beatsPerMeasure.get());
  }

  public void setLeftCircle(ImageView leftCircle) {
    this.leftCircle = leftCircle;
  }

  public void setLeftCircleFadeOut(Animation leftCircleFadeOut) {
    this.leftCircleFadeOut = leftCircleFadeOut;
  }

  public void setRightCircle(ImageView rightCircle) {
    this.rightCircle = rightCircle;
  }

  public void setRightCircleFadeOut(Animation rightCircleFadeOut) {
    this.rightCircleFadeOut = rightCircleFadeOut;
  }

  public void setNeedle(ImageView needle) {
    this.needle = needle;
  }

  public void setNeedleTurnR(Animation needleTurnR) {
    this.needleTurnR = needleTurnR;
  }

  public void setNeedleTurnL(Animation needleTurnL) {
    this.needleTurnL = needleTurnL;
  }

  public void setTempoView(TextView tempoView) {
    this.tempoView = tempoView;
  }

  public void setBPMLabelView(TextView BPMLabelView) {
    this.BPMLabelView = BPMLabelView;
  }

  public void setBPMView(TextView BPMView) {
    this.BPMView = BPMView;
  }

  public void setPlayMenuItem(MenuItem playMenuItem) {
    this.playMenuItem = playMenuItem;
  }

  public void setTempoMenuItem(MenuItem tempoMenuItem) { this.tempoMenuItem = tempoMenuItem; }

  public void setBasicButtons(Button startButton, Button upButton, Button downButton) {
    View.OnTouchListener buttonTouchListener = new View.OnTouchListener() {
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
          stopAutoMode();
        }
        return false;
      }
    };
    startButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        startOrPause(v);
      }
    });

    upButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        increaseTempo(v);
      }
    });
    upButton.setOnLongClickListener(new View.OnLongClickListener() {
      public boolean onLongClick(View v) {
        Log.v("metronome", "longClick");
        startAutoMode(v, true);
        return false;
      }
    });
    upButton.setOnTouchListener(buttonTouchListener);

    downButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        decreaseTempo(v);
      }
    });
    downButton.setOnLongClickListener(new View.OnLongClickListener() {
      public boolean onLongClick(View v) {
        startAutoMode(v, false);
        return false;
      }
    });
    downButton.setOnTouchListener(buttonTouchListener);

  }
}

