package com.ericchoi.clicker.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ericchoi.clicker.ClickerMain;
import com.ericchoi.clicker.Metronome;
import com.ericchoi.clicker.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class MetronomeFragment extends Fragment {
  /**
   * The fragment argument representing the section number for this
   * fragment.
   */
  private static final String ARG_SECTION_NUMBER = "section_number";

  private long countLastClicked = 0;

  /**
   * Returns a new instance of this fragment for the given section
   * number.
   */
  public static MetronomeFragment newInstance(int sectionNumber) {
    MetronomeFragment fragment = new MetronomeFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    fragment.setArguments(args);
    return fragment;
  }

  public MetronomeFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    Log.v("metronome", "frag onCreateView called");
    //TODO factor set up code out
    final View rootView = inflater.inflate(R.layout.fragment_clicker_main, container, false);
    initView(rootView);
    return rootView;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.clicker_main, menu);
    for (int i = 0; i < menu.size(); i++) {
      MenuItem mi = menu.getItem(i);
      if (mi.getItemId() == R.id.action_play) {
        ((ClickerMain)getActivity()).getMetronome().setPlayMenuItem(mi);
      }
    }
  }

  protected void initView(final View rootView) {
    ClickerMain clickerActivity = (ClickerMain)getActivity();
    final Metronome metronome = clickerActivity.getMetronome();

    // buttons
    final Button upButton = (Button) rootView.findViewById(R.id.up_button);
    final Button downButton = (Button) rootView.findViewById(R.id.down_button);

    metronome.setBasicButtons(upButton, downButton);

    final Button startButton = (Button) rootView.findViewById(R.id.start_button);
    startButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        metronome.startOrPause(v);
      }
    });

    final Button beatButton = (Button) rootView.findViewById(R.id.beat_button);
    beatButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        metronome.toggleBeat(v);
      }
    });

    final Button countButton = (Button) rootView.findViewById(R.id.count_button);
    countButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (countLastClicked != 0) {
          long interval = SystemClock.elapsedRealtime() - countLastClicked;
          Log.v("metronome", "interval: " + interval);
          metronome.updateTempoTo(v, (int) (1000L * 60L / interval));
        }
        countLastClicked = SystemClock.elapsedRealtime();
      }
    });

    // setup Metronome
    ImageView leftCircle = (ImageView) rootView.findViewById(R.id.metronome_circle_left);
    ImageView rightCircle = (ImageView) rootView.findViewById(R.id.metronome_circle_right);
    leftCircle.setAlpha (0.0f);
    rightCircle.setAlpha(0.0f);
    ImageView needle = (ImageView) rootView.findViewById(R.id.metronome_needle);

    TextView tempoView = (TextView) rootView.findViewById(R.id.tempo);
    TextView BPMView = (TextView) rootView.findViewById(R.id.beat_counter);
    TextView BPMLabelView = (TextView) rootView.findViewById(R.id.beat_label);

    metronome.setLeftCircle(leftCircle);
    metronome.setRightCircle(rightCircle);
    metronome.setNeedle(needle);
    metronome.setTempoView(tempoView);
    metronome.setBPMView(BPMView);
    metronome.setBPMLabelView(BPMLabelView);

    metronome.initView();
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ((ClickerMain) activity).onSectionAttached(
      getArguments().getInt(ARG_SECTION_NUMBER));
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.v("metronome", "frag paused called");
    ClickerMain clickerActivity = (ClickerMain)getActivity();
    final Metronome metronome = clickerActivity.getMetronome();
  }

  @Override
  public void onResume() {
    Log.v("metronome", "frag on resume called");
    super.onResume();
  }

  @Override
  public void onDestroyView() {
    Log.v("metronome", "frag on destroy view called");
    super.onDestroyView();
  }
}
