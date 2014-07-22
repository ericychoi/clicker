package com.ericchoi.clicker;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ClickerMain extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private NavigationDrawerFragment mNavigationDrawerFragment;

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;

  private Metronome metronome;
  private SoundPool clickSP;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.v("metronome", "onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_clicker_main);

    mNavigationDrawerFragment = (NavigationDrawerFragment)
            getFragmentManager().findFragmentById(R.id.navigation_drawer);
    mTitle = getTitle();

    // Set up the drawer.
    mNavigationDrawerFragment.setUp(
            R.id.navigation_drawer,
            (DrawerLayout) findViewById(R.id.drawer_layout));

    // set up audio track
    //TODO make initial pool size a config value
    clickSP = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
    int clickSid = clickSP.load(getApplicationContext(), R.raw.click, 1);
    int loudClickSid = clickSP.load(getApplicationContext(), R.raw.loud, 1);

    this.metronome = new Metronome(clickSP, clickSid, loudClickSid);
  }

  @Override
  public void onNavigationDrawerItemSelected(int position) {
    // update the main content by replacing fragments
    FragmentManager fragmentManager = getFragmentManager();
    fragmentManager.beginTransaction()
            .replace(R.id.container, MetronomeFragment.newInstance(position + 1))
            .commit();
  }

  public void onSectionAttached(int number) {
    switch (number) {
      case 1:
        mTitle = getString(R.string.title_section1);
        break;
      case 2:
        mTitle = getString(R.string.title_section2);
        break;
      case 3:
        mTitle = getString(R.string.title_section3);
        break;
    }
  }

  public void restoreActionBar() {
    ActionBar actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (!mNavigationDrawerFragment.isDrawerOpen()) {
      // Only show items in the action bar relevant to this screen
      // if the drawer is not showing. Otherwise, let the drawer
      // decide what to show in the action bar.
      getMenuInflater().inflate(R.menu.clicker_main, menu);
      for (int i = 0; i < menu.size(); i++) {
        MenuItem mi = menu.getItem(i);
        if (mi.getItemId() == R.id.play_action) {
          metronome.setPlayMenuItem(mi);
        }
      }
      restoreActionBar();
      return true;
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.play_action) {
      Log.v("metronome", "play action pressed");
      metronome.startOrPause(this.getCurrentFocus());
        /*
      if (metronome.startOrPause(this.getCurrentFocus()) item.setIcon(R.drawable.ic_action_pause);
      else item.setIcon(R.drawable.ic_action_play);*/
    }

    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public Metronome getMetronome() {
    return metronome;
  }

  @Override
  protected void onStop() {
    super.onStop();
    clickSP.release();
  }

  /**
   * A placeholder fragment containing a simple view.
   */
  public static class MetronomeFragment extends Fragment {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
      Log.v("metronome", "frag onCreateView called");
      //TODO factor set up code out
      final View rootView = inflater.inflate(R.layout.fragment_clicker_main, container, false);
      initView(rootView);
      return rootView;
    }

    private void initView(final View rootView) {
      ClickerMain clickerActivity = (ClickerMain)getActivity();
      final Metronome metronome = clickerActivity.getMetronome();

      View.OnTouchListener buttonTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
          if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            metronome.stopAutoMode();
          }
          return false;
        }
      };

      // buttons
      final Button startButton = (Button) rootView.findViewById(R.id.start_button);
      startButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          metronome.startOrPause(v);
        }
      });

      final Button upButton = (Button) rootView.findViewById(R.id.up_button);
      upButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          metronome.increaseTempo(v);
        }
      });
      upButton.setOnLongClickListener(new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
          Log.v("metronome", "longClick");
          metronome.startAutoMode(v, true);
          return false;
        }
      });
      upButton.setOnTouchListener(buttonTouchListener);

      final Button downButton = (Button) rootView.findViewById(R.id.down_button);
      downButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          metronome.decreaseTempo(v);
        }
      });
      downButton.setOnLongClickListener(new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
          metronome.startAutoMode(v, false);
          return false;
        }
      });
      downButton.setOnTouchListener(buttonTouchListener);

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
}
