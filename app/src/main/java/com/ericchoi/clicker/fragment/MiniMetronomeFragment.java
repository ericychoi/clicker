package com.ericchoi.clicker.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ericchoi.clicker.ClickerMain;
import com.ericchoi.clicker.Metronome;
import com.ericchoi.clicker.R;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.google.gson.stream.JsonReader;

/**
 * Created by ericchoi on 7/24/14.
 */
public class MiniMetronomeFragment extends MetronomeFragment {

  private static final String ARG_SECTION_NUMBER = "section_number";

  public MiniMetronomeFragment() {
    Log.v("metronome", "mini metronome called");
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v("metronome", "frag onCreateView called");
    //TODO factor set up code out
    final View rootView = inflater.inflate(R.layout.fragment_trainer, container, false);

    // buttons
    final Button upButton = (Button) rootView.findViewById(R.id.up_button);
    final Button downButton = (Button) rootView.findViewById(R.id.down_button);

    Metronome metronome = ((ClickerMain) getActivity()).getMetronome();
    metronome.setBasicButtons(upButton, downButton);
    metronome.setTempoView((TextView) rootView.findViewById(R.id.tempo));
    metronome.updateTempoView();

    // only one circle
    ImageView circle = (ImageView) rootView.findViewById(R.id.minimetronome_circle);
    circle.setAlpha (0.0f);
    metronome.setLeftCircle(circle);
    metronome.setRightCircle(circle);

    // Construct the data source
    InputStream in = getResources().openRawResource(R.raw.stickings);
    ArrayList<String> stickings = new ArrayList<String>();

    JsonReader reader;
    try {
      reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
      reader.beginObject();
      if (reader.nextName().equals("stickings")) {
        reader.beginArray();
        while (reader.hasNext()) {
          String s = "";
          reader.beginArray();
          while (reader.hasNext()) {
            s += reader.nextString();
          }
          stickings.add(s);
          reader.endArray();
        }
        reader.endArray();
      }
      reader.endObject();
    } catch (Exception e) {
      Log.e("metronome", "couldn't read with jsonReader");
    }

    // Create the adapter to convert the array to views
    ArrayAdapter<String> adapter = new ArrayAdapter(
      this.getActivity().getApplicationContext(),
      android.R.layout.simple_list_item_1,
      stickings);

    // Attach the adapter to a ListView
    ListView listView = (ListView) rootView.findViewById(R.id.sticking_list);
    listView.setAdapter(adapter);

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
