package armmel.library;

import android.database.DataSetObserver;

/**
 * Extracted PagerObserver to keep ViewPager smaller. Uses a ViewPager reference to call
 * dataSetChanged() which is package-private.
 */
public class PagerObserver extends DataSetObserver {
  private final ViewPager mViewPager;

  public PagerObserver(ViewPager viewPager) {
    mViewPager = viewPager;
  }

  @Override
  public void onChanged() {
    mViewPager.dataSetChanged();
  }

  @Override
  public void onInvalidated() {
    mViewPager.dataSetChanged();
  }
}
