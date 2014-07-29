package com.ericchoi.clicker.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ericchoi.clicker.ClickerMain;
import com.ericchoi.clicker.Metronome;
import com.ericchoi.clicker.R;

/**
 * Created by ericchoi on 7/24/14.
 */
public class MiniMetronomeFragment extends MetronomeFragment {

  private static final String ARG_SECTION_NUMBER = "section_number";

  public MiniMetronomeFragment() {
    Log.v("metronome", "mini metronome called");
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.clicker_mini, menu);
    Metronome m = ((ClickerMain) getActivity()).getMetronome();
    for (int i = 0; i < menu.size(); i++) {
      MenuItem mi = menu.getItem(i);
      if (mi.getItemId() == R.id.action_tempo) {
        m.setTempoMenuItem(mi);
      }
    }
    m.updateTempoMenuItem();
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v("metronome", "frag onCreateView called");
    //TODO factor set up code out
    final View rootView = inflater.inflate(R.layout.fragment_trainer, container, false);
    //super.initView(rootView);
    return rootView;
  }

  public static MiniMetronomeFragment newInstance(int sectionNumber) {
    MiniMetronomeFragment fragment = new MiniMetronomeFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    fragment.setArguments(args);
    return fragment;
  }
}
