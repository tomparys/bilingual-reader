package cz.metaverse.android.bilingualreader.helper;

import android.util.Log;
import cz.metaverse.android.bilingualreader.enums.ScrollSyncMethod;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 * This class holds Scroll Synchronization Sync Points that are used for the syncPoints method of ScrollSync.
 *
 */
public class ScrollSyncPoint {

	private static final String LOG = "ScrollSyncPoint";

	private long p0;
	private long p1;

	public ScrollSyncPoint(int scrollPosition0, int scrollPosition1) {
		this.p0 = scrollPosition0;
		this.p1 = scrollPosition1;
	}

	/**
	 * Computes the ScrollSync offset and ratio from the two provided SynCPoints,
	 * and fills the resulting values to both the WebViews so that ScrollSync can be activated.
	 * @param other    The other ScrollSyncPoint to use for computations.
	 * @param webView  The two SelectionWebViews that are about to be synchronized.
	 * @return         Whether or not the computation was successful.
	 *                   (Fails if the position of one of the books is the same in both SyncPoints.)
	 */
	public boolean computeAndActivateScrollSync(ScrollSyncPoint other, SelectionWebView[] webView) {

		if (this.p0 == other.p0 || this.p1 == other.p1) {
			// Panel 0 or Panel 1 positions are the same in both panels, impossible to compute.
			return false;
		}

		// Compute the offset.
		long offset = (other.p1 * this.p0 - other.p0 * this.p1) / (this.p0 - other.p0);

		/* Compute the ratio, but only if it is possible (if panel 0 was at non-zero scroll at one of the points). */
		double ratio = 0;

		if (other.p0 != 0) {
			ratio = (double) (other.p1 - offset) / (double) other.p0;
		}
		else if (this.p0 != 0) {
			ratio = (double) (this.p1 - offset) / (double) this.p0;
		}
		else {
			// Both Scroll points have scroll 0 in panel 0, impossible to compute.
			return false;
		}

		// Initialize the ScrollSync syncPoint method and its data into the two WebViews
		webView[0].initializeScrollSyncData(ScrollSyncMethod.syncPoints, (int) offset, (float) ratio);
		webView[1].initializeScrollSyncData(ScrollSyncMethod.syncPoints, (int) offset, (float) ratio);

		Log.d(LOG, String.format("%s.computeAndActivateScrollSync - this (%s, %s), other (%s, %s)",
				LOG, this.p0, this.p1, other.p0, other.p1));
		Log.d(LOG, LOG + ".computeAndActivateScrollSync - offset: " + offset + ", ratio: " + ratio);

		return true;
	}
}
