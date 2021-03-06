/*
Bilingual Reader for Android

Copyright (c) 2015 Tomáš Orsava

This program is free software: you can redistribute it and/or modify it under the terms
of the GNU General Public License as published by the Free Software Foundation, either
version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this
program (see the LICENSE.html file). If not, see http://www.gnu.org/licenses/.

Contact: gpl at orsava.cz


This file incorporates work covered by the following copyright and permission notice:


The MIT License (MIT)

Copyright (c) 2013, V. Giacometti, M. Giuriato, B. Petrantuono

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package cz.metaverse.android.bilingualreader.panel;

import java.io.File;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.manager.Governor;
import cz.metaverse.android.bilingualreader.manager.PanelHolder;

/**
 *
 * SplitPanel specialized to show a list of audio files and to play the desired one.
 * 		The audio files are stored in an array of array.
 * 		The first index indicates the different audio files,
 * 			the second one indicates the different extensions for the same audio file.
 *
 * Not used in our application.
 *
 */
public class AudioPanel extends SplitPanel {

	private static final String LOG = "AudioPanel";

	String[][] audio;
	ListView list;
	private MediaPlayer player;
	private Button rew;
	private Button playpause;
	private String actuallyPlaying = null;
	private SeekBar progressBar;
	private Runnable update;
	private Handler progressHandler;


	/**
	 * Constructor - let's get the important info filled.
	 * @param governor  The Governor of our application.
	 * @param panelHolder  The PanelHolder instance holding this panel.
	 * @param position  The position of this panel.
	 */
	public AudioPanel(Governor governor, PanelHolder panelHolder, int position) {
		super(governor, panelHolder, position);
	}

	/**
	 * Empty constructor for when Android needs to recreate the Fragment.
	 */
	public AudioPanel() {
		super();
	}

	/**
	 * Checks whether there are any problems with this instance, if for example the Android system
	 * didn't close any important fields that would result in NullPointerExceptions.
	 * @return true if everything appears to be sound
	 */
	@Override
	public boolean selfCheck(boolean creatingActivity) {
		boolean ok = super.selfCheck(creatingActivity) && rew != null && playpause != null && progressBar != null
				&& progressHandler != null;

		Log.d(LOG, "AudioPanel selfCheck - " + ok);
		return ok;
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.panel_audio, container,
				false);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle saved) {
		super.onActivityCreated(saved);
		list = (ListView) getView().findViewById(R.id.audioListView);
		rew = (Button) getView().findViewById(R.id.RewindButton);
		playpause = (Button) getView().findViewById(R.id.PlayPauseButton);
		progressBar = (SeekBar) getView().findViewById(R.id.progressBar);
		progressHandler = new Handler();

		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> listView, View itemView,
					int position, long itemId) {
				start(position);
			}
		});

		// Play or Pause Button
		playpause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (player.isPlaying()) {
					player.pause();
					playpause.setText(getString(R.string.play));
				} else {
					player.start();
					playpause.setText(getString(R.string.pause));
					update.run();
				}
			}
		});

		// Rewind Button
		rew.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (player != null) {
					player.seekTo(0);
					player.start();
				}
			}
		});

		progressBar.setProgress(0);
		progressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser && player != null)
					player.seekTo(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		// This runnable update the progressBar progression every 500
		// milliseconds
		update = new Runnable() {
			@Override
			public void run() {
				if (player != null) {
					progressBar.setMax(player.getDuration());
					progressBar.setProgress(player.getCurrentPosition());
				}
				progressHandler.postDelayed(this, 500);
			}
		};
		progressHandler.postDelayed(update, 500);

		setAudioList(audio);

		updateButtons();
	}

	private void updateButtons() {
		if (player != null) {
			playpause.setEnabled(true);
			rew.setEnabled(true);

			if (player.isPlaying())
				playpause.setText(getString(R.string.pause));
			else
				playpause.setText(getString(R.string.play));
		} else {
			playpause.setEnabled(false);
			rew.setEnabled(false);
		}
	}

	// Load the list of audio files
	public void setAudioList(String[][] audio) {
		this.audio = audio;
		if (created) {
			if (player != null) {
				player.stop();
				player.release();
				player = null;
			}
			String[] songs = new String[audio.length];
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();

			for (int i = 0; i < audio.length; i++) {
				// Get Title
				retriever.setDataSource(audio[i][0].replace("file:///", ""));
				String title = retriever
						.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
				if (title == null)
					title = (new File(audio[i][0])).getName();

				// Get Duration
				String d = retriever
						.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
				if (d != null)
					d = (String) DateFormat
							.format("mm:ss", Integer.parseInt(d));
				else
					d = "";

				songs[i] = (i + 1) + "\t-\t" + title + "\t" + d;
			}

			ArrayAdapter<String> songList = new ArrayAdapter<String>(
					getActivity(), android.R.layout.simple_list_item_1, songs);
			list.setAdapter(songList);

			if (getActivity().getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
				// Portrait case: Adjust view's height depending on the number
				// of files
				int height = getView().findViewById(R.id.PlayerLayout)
						.getHeight() + 2;
				// no files: hide the view
				if (songs.length == 0)
					height = 0;

				View listItem;
				for (int i = 0; i < songs.length; i++) {
					listItem = songList.getView(i, null, list);
					listItem.measure(MeasureSpec.makeMeasureSpec(0,
							MeasureSpec.UNSPECIFIED), MeasureSpec
							.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
					height += listItem.getMeasuredHeight();
				}

				float weight = (float) height
						/ ((ReaderActivity) getActivity()).getHeight();
				if (weight > 0.5f)
					weight = 0.5f;
				governor.changePanelsWeight(1 - weight);

				// 1 file: show the player only and able it
				if (songs.length == 1) {
					start(0);
					player.pause();
				}
			} else {
				// Landscape case: fifty-fifty
				governor.changePanelsWeight(0.5f);
			}

			updateButtons();

		}
	}

	public void start(int i) {
		if (i >= 0 && i < audio.length) {
			int j = 0;
			boolean err = true;

			if (player == null)
				player = new MediaPlayer();

			// Try to play every format of the selected audio
			while (j < audio[i].length && err)
				try {
					player.reset();
					player.setDataSource(audio[i][j]);
					player.prepare();
					player.start();
					progressBar.setMax(player.getDuration());
					rew.setEnabled(true);
					playpause.setEnabled(true);
					playpause.setText(getString(R.string.pause));
					actuallyPlaying = audio[i][j];
					err = false;
				} catch (Exception e) {
					actuallyPlaying = null;
				}
			if (err) {
				playpause.setEnabled(false);
				((ReaderActivity) getActivity())
						.errorMessage(getString(R.string.error_openaudiofile));
			}

		}
	}

	public void stop() {
		if (player != null) {
			player.stop();
			player.release();
			player = null;
		}
		progressHandler.removeCallbacks(update);
	}

	@Override
	public void saveState(Editor editor) {
		progressHandler.removeCallbacks(update);
		super.saveState(editor);

		if (player != null) {
			editor.putBoolean(panelPosition + "isPlaying", player.isPlaying());
			editor.putInt(panelPosition + "current", player.getCurrentPosition());
			editor.putString(panelPosition + "actualSong", actuallyPlaying);
			stop();
		}
	}

	@Override
	public void loadState(SharedPreferences preferences, boolean creatingActivity) {
		super.loadState(preferences, creatingActivity);
		actuallyPlaying = preferences.getString(panelPosition + "actualSong", null);
		setAudioList(audio);

		if (actuallyPlaying != null) {
			player = new MediaPlayer();
			player.reset();
			try {
				player.setDataSource(actuallyPlaying);
				player.prepare();
				if (preferences.getBoolean(panelPosition + "isPlaying", false))
					player.start();
				player.seekTo(preferences.getInt(panelPosition + "current", 0));
			} catch (Exception e) {
				// TODO error message
			}
		}
	}

}
