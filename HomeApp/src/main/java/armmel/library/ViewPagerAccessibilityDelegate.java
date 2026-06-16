package armmel.library;

import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/** Accessibility delegate extracted from ViewPager to reduce class size. */
public class ViewPagerAccessibilityDelegate extends View.AccessibilityDelegate {
  private final ViewPager mViewPager;

  public ViewPagerAccessibilityDelegate(ViewPager viewPager) {
    mViewPager = viewPager;
  }

  @Override
  public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
    super.onInitializeAccessibilityEvent(host, event);
    event.setClassName(ViewPager.class.getName());
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(host, info);
    info.setClassName(ViewPager.class.getName());
    final PagerAdapter adapter = mViewPager.getAdapter();
    info.setScrollable(adapter != null && adapter.getCount() > 1);
    if (adapter != null && mViewPager.getCurrentItem() >= 0 && mViewPager.getCurrentItem() < adapter.getCount() - 1) {
      info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }
    if (adapter != null && mViewPager.getCurrentItem() > 0 && mViewPager.getCurrentItem() < adapter.getCount()) {
      info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }
  }

  @Override
  public boolean performAccessibilityAction(View host, int action, Bundle args) {
    if (super.performAccessibilityAction(host, action, args)) {
      return true;
    }
    switch (action) {
      case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD: {
        final PagerAdapter adapter = mViewPager.getAdapter();
        if (adapter != null && mViewPager.getCurrentItem() >= 0 && mViewPager.getCurrentItem() < adapter.getCount() - 1) {
          mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
          return true;
        }
        return false;
      }
      case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
        final PagerAdapter adapter = mViewPager.getAdapter();
        if (adapter != null && mViewPager.getCurrentItem() > 0 && mViewPager.getCurrentItem() < adapter.getCount()) {
          mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
          return true;
        }
        return false;
      }
    }
    return false;
  }
}
