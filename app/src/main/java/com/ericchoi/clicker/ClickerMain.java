package com.ericchoi.clicker;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import com.ericchoi.clicker.fragment.MetronomeFragment;
import com.ericchoi.clicker.fragment.MiniMetronomeFragment;

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
    Log.v("metronome", "navi item pressed:" + position);
    // update the main content by replacing fragments
    FragmentManager fragmentManager = getFragmentManager();
    Fragment frag;

    if (position == 1) {
      Log.v("metronome", "here:" + position);
      frag = MiniMetronomeFragment.newInstance(position + 1);
    }
    else {
      frag = MetronomeFragment.newInstance(position + 1);
    }

    fragmentManager.beginTransaction()
            .replace(R.id.container, frag)
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

    if (id == R.id.action_play) {
      Log.v("metronome", "play action pressed");
      metronome.startOrPause(this.getCurrentFocus());
    }

    if (id == R.id.action_settings) {
      Log.v("metronome", "setting action pressed");
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
}
