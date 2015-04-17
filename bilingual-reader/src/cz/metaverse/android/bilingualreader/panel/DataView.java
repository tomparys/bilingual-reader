/*
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

import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.R.id;
import cz.metaverse.android.bilingualreader.R.layout;
import cz.metaverse.android.bilingualreader.R.string;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

//Panel specialized in visualizing HTML-data
/**
 * 
 * Fragment that extends SplitPanel by using WebView to display content.
 *
 */
public class DataView extends SplitPanel {
	protected WebView webView;
	protected String data;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.activity_data_view, container, false);
		return v;
	}
	
	@Override
    public void onActivityCreated(Bundle saved) {
		super.onActivityCreated(saved);
		webView = (WebView) getView().findViewById(R.id.Viewport);
		
		// Set an Extended WebViewClient
		webView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// Set a new book page when a link is pressed inside the web view
				try {
					navigator.setBookPage(url, index);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_LoadPage));
				}
				return true;
			}
		});
		
		loadData(data);
	}
	
	/**
	 * Loads text into the webview
	 * @param source String to display
	 */
	public void loadData(String source)
	{
		this.data = source;
		
		if (created)
			webView.loadData(data, 
				getActivity().getApplicationContext().getResources().getString(R.string.textOrHTML),
				null);
	}

	/**
	 * Saves the currently displayed text in the web view
	 */
	@Override
	public void saveState(Editor editor)
	{
		super.saveState(editor);
		editor.putString("data"+index, data);
	}
	
	/**
	 * Restores the currently displayed text into the web view
	 */
	@Override
	public void loadState(SharedPreferences preferences)
	{
		super.loadState(preferences);
		loadData(preferences.getString("data"+index, ""));
	}
}
