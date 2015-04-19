package cz.metaverse.android.bilingualreader.dialog;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.R;

/**
 * 
 * Customize display style dialog.
 *
 */
public class ChangeCSSDialog extends DialogFragment {

	protected Button defaultButton;
	protected Builder builder;
	protected Spinner spinColor;
	protected Spinner spinBgColor;
	protected Spinner spinFontStyle;
	protected Spinner spinAlignText;
	protected Spinner spinFontSize;
	protected Spinner spinLineHeight;
	protected Spinner spinLeft;
	protected Spinner spinRight;
	protected int colorInt, bgColorInt, fontInt, alignInt, sizeInt, heightInt,
				  marginLeftInt, marginRightInt;
	protected ReaderActivity activity;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Get the dialog builder and layout inflater
		builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		activity = (ReaderActivity) getActivity();
		View view = inflater.inflate(R.layout.change_css, null);

		// Get saved preferences
		final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);

		// ---- Set up individual setting widgets
		spinColor = (Spinner) view.findViewById(R.id.spinnerColor);
		colorInt = preferences.getInt("spinColorValue", 0);
		spinColor.setSelection(colorInt);

		spinBgColor = (Spinner) view.findViewById(R.id.spinnerBackgroundColor);
		bgColorInt = preferences.getInt("spinBackValue", 0);
		spinBgColor.setSelection(bgColorInt);

		spinFontStyle = (Spinner) view.findViewById(R.id.spinnerFontFamily);
		fontInt = preferences.getInt("spinFontStyleValue", 0);
		spinFontStyle.setSelection(fontInt);

		spinAlignText = (Spinner) view.findViewById(R.id.spinnerAlign);
		alignInt = preferences.getInt("spinAlignTextValue", 0);
		spinAlignText.setSelection(alignInt);

		spinFontSize = (Spinner) view.findViewById(R.id.spinnerFontSize);
		sizeInt = preferences.getInt("spinFontSizeValue", 0);
		spinFontSize.setSelection(sizeInt);

		spinLineHeight = (Spinner) view.findViewById(R.id.spinnerLineHeight);
		heightInt = preferences.getInt("spinLineHValue", 0);
		spinLineHeight.setSelection(heightInt);

		spinLeft = (Spinner) view.findViewById(R.id.spinnerLeftMargin);
		marginLeftInt = preferences.getInt("spinLeftValue", 0);
		spinLeft.setSelection(marginLeftInt);

		spinRight = (Spinner) view.findViewById(R.id.spinnerRightMargin);
		marginRightInt = preferences.getInt("spinRightValue", 0);
		spinRight.setSelection(marginRightInt);

		// The button that says "Default" that resets the settings
		defaultButton = (Button) view.findViewById(R.id.buttonDefault);

		// Set title and set the view
		builder.setTitle("Style");
		builder.setView(view);

		// ------ Set listeners for the widgets
		spinColor
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						colorInt = (int) id;
						switch ((int) id) {
						case 0:
							activity.setColor(getString(R.string.black_rgb));
							break;
						case 1:
							activity.setColor(getString(R.string.red_rgb));
							break;
						case 2:
							activity.setColor(getString(R.string.green_rgb));
							break;
						case 3:
							activity.setColor(getString(R.string.blue_rgb));
							break;
						case 4:
							activity.setColor(getString(R.string.white_rgb));
							break;
						default:
							break;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		spinBgColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				bgColorInt = (int) id;
				switch ((int) id) {
				case 0:
					activity.setBackColor(getString(R.string.white_rgb));
					break;
				case 1:
					activity.setBackColor(getString(R.string.red_rgb));
					break;
				case 2:
					activity.setBackColor(getString(R.string.green_rgb));
					break;
				case 3:
					activity.setBackColor(getString(R.string.blue_rgb));
					break;
				case 4:
					activity.setBackColor(getString(R.string.black_rgb));
					break;
				default:
					break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spinFontStyle
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						fontInt = (int) id;
						switch ((int) id) {
						case 0:
							activity.setFontType(getString(R.string.Arial));
							break;
						case 1:
							activity.setFontType(getString(R.string.Serif));
							break;
						case 2:
							activity.setFontType(getString(R.string.Monospace));
							break;
						default:
							break;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		spinAlignText
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						alignInt = (int) id;
						switch ((int) id) {
						case 0:
							activity.setAlign(getString(R.string.Left_Align));
							break;
						case 1:
							activity.setAlign(getString(R.string.Center_Align));
							break;
						case 2:
							activity.setAlign(getString(R.string.Right_Align));
							break;
						case 3:
							activity.setAlign(getString(R.string.Justify));
							break;
						default:
							break;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		spinFontSize
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						sizeInt = (int) id;
						switch ((int) id) {
						case 0:
							activity.setFontSize("100");
							break;
						case 1:
							activity.setFontSize("125");
							break;
						case 2:
							activity.setFontSize("150");
							break;
						case 3:
							activity.setFontSize("175");
							break;
						case 4:
							activity.setFontSize("200");
							break;
						case 5:
							activity.setFontSize("90");
							break;
						default:
							break;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		spinLineHeight
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						heightInt = (int) id;
						switch ((int) id) {
						case 0:
							activity.setLineHeight("1");
							break;
						case 1:
							activity.setLineHeight("1.25");
							break;
						case 2:
							activity.setLineHeight("1.5");
							break;
						case 3:
							activity.setLineHeight("1.75");
							break;
						case 4:
							activity.setLineHeight("2");
							break;
						case 5:
							activity.setLineHeight("0.9");
							break;
						default:
							break;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		spinLeft.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				marginLeftInt = (int) id;
				switch ((int) id) {
				case 0:
					activity.setMarginLeft("0");
					break;
				case 1:
					activity.setMarginLeft("5");
					break;
				case 2:
					activity.setMarginLeft("10");
					break;
				case 3:
					activity.setMarginLeft("15");
					break;
				case 4:
					activity.setMarginLeft("20");
					break;
				case 5:
					activity.setMarginLeft("25");
					break;
				default:
					break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		spinRight
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						marginRightInt = (int) id;
						switch ((int) id) {
						case 0:
							activity.setMarginRight("0");
							break;
						case 1:
							activity.setMarginRight("5");
							break;
						case 2:
							activity.setMarginRight("10");
							break;
						case 3:
							activity.setMarginRight("15");
							break;
						case 4:
							activity.setMarginRight("20");
							break;
						case 5:
							activity.setMarginRight("25");
							break;
						default:
							break;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		// The button that says "Default".
		defaultButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// Reset all settings to their defaults and dismiss the dialog.
				activity.setColor("");
				activity.setBackColor("");
				activity.setFontType("");
				activity.setFontSize("");
				activity.setLineHeight("");
				activity.setAlign("");
				activity.setMarginLeft("");
				activity.setMarginRight("");
				activity.setCSS();
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("spinColorValue", 0);
				editor.putInt("spinBackValue", 0);
				editor.putInt("spinFontStyleValue", 0);
				editor.putInt("spinAlignTextValue", 0);
				editor.putInt("spinFontSizeValue", 0);
				editor.putInt("spinLineHValue", 0);
				editor.putInt("spinLeftValue", 0);
				editor.putInt("spinRightValue", 0);
				editor.commit();

				dismiss();
			}
		});

		// The OK button
		builder.setPositiveButton(getString(R.string.OK),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						// Tell activity to apply changes
						activity.setCSS();

						// Save all settings into the preferences memory store.
						SharedPreferences.Editor editor = preferences.edit();
						editor.putInt("spinColorValue", colorInt);
						editor.putInt("spinBackValue", bgColorInt);
						editor.putInt("spinFontStyleValue", fontInt);
						editor.putInt("spinAlignTextValue", alignInt);
						editor.putInt("spinFontSizeValue", sizeInt);
						editor.putInt("spinLineHValue", heightInt);
						editor.putInt("spinLeftValue", marginLeftInt);
						editor.putInt("spinRightValue", marginRightInt);
						editor.commit();
					}
				});
		
		// The Cancel button
		builder.setNegativeButton(getString(R.string.Cancel),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});

		// Finally, create the dialogue and return it.
		return builder.create();
	}
}
