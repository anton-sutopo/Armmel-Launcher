/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package armmel.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.Scroller;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Layout manager that allows the user to flip left and right through pages of data. You supply an
 * implementation of a {@link PagerAdapter} to generate the pages that the view shows.
 *
 * <p>Note this class is currently under early design and development. The API will likely change in
 * later updates of the compatibility library, requiring changes to the source code of apps when
 * they are compiled against the newer version.
 *
 * <p>ViewPager is most often used in conjunction with {@link android.app.Fragment}, which is a
 * convenient way to supply and manage the lifecycle of each page. There are standard adapters
 * implemented for using fragments with the ViewPager, which cover the most common use cases. These
 * are {@link android.support.v4.app.FragmentPagerAdapter}, {@link
 * android.support.v4.app.FragmentStatePagerAdapter}, {@link
 * android.support.v13.app.FragmentPagerAdapter}, and {@link
 * android.support.v13.app.FragmentStatePagerAdapter}; each of these classes have simple code
 * showing how to build a full user interface with them.
 *
 * <p>Here is a more complicated example of ViewPager, using it in conjuction with {@link
 * android.app.ActionBar} tabs. You can find other examples of using ViewPager in the API 4+ Support
 * Demos and API 13+ Support Demos sample code.
 *
 * <p>{@sample
 * development/samples/Support13Demos/src/com/example/android/supportv13/app/ActionBarTabsPager.java
 * complete}
 */
public class ViewPager extends ViewGroup {
  private static final String TAG = "ViewPager";
  private static final boolean DEBUG = false;

  private static final boolean USE_CACHE = false;

  private static final int DEFAULT_OFFSCREEN_PAGES = 1;
  private static final int MAX_SETTLE_DURATION = 600; // ms
  private static final int MIN_DISTANCE_FOR_FLING = 25; // dips

  private static final int DEFAULT_GUTTER_SIZE = 16; // dips

  private static final int[] LAYOUT_ATTRS = new int[] {android.R.attr.layout_gravity};

  static class ItemInfo {
    Object object;
    int position;
    boolean scrolling;
    float widthFactor;
    float offset;
  }

  private static final Comparator<ItemInfo> COMPARATOR =
      new Comparator<ItemInfo>() {
        @Override
        public int compare(ItemInfo lhs, ItemInfo rhs) {
          return lhs.position - rhs.position;
        }
      };

  private static final Interpolator sInterpolator =
      new Interpolator() {
        public float getInterpolation(float t) {
          t -= 1.0f;
          return t * t * t * t * t + 1.0f;
        }
      };

  private final ArrayList<ItemInfo> mItems = new ArrayList<ItemInfo>();
  private final ItemInfo mTempItem = new ItemInfo();

  private final Rect mTempRect = new Rect();

  private PagerAdapter mAdapter;
  private int mCurItem; // Index of currently displayed page.
  private int mRestoredCurItem = -1;
  private Parcelable mRestoredAdapterState = null;
  private ClassLoader mRestoredClassLoader = null;
  private Scroller mScroller;
  private PagerObserver mObserver;

  private int mPageMargin;
  private Drawable mMarginDrawable;
  private int mTopPageBounds;
  private int mBottomPageBounds;

  // Offsets of the first and last items, if known.
  // Set during population, used to determine if we are at the beginning
  // or end of the pager data set during touch scrolling.
  private float mFirstOffset = -Float.MAX_VALUE;
  private float mLastOffset = Float.MAX_VALUE;

  private int mChildWidthMeasureSpec;
  private int mChildHeightMeasureSpec;
  private boolean mInLayout;

  private boolean mScrollingCacheEnabled;

  private boolean mPopulatePending;
  private int mOffscreenPageLimit = DEFAULT_OFFSCREEN_PAGES;

  private boolean mIsBeingDragged;
  private boolean mIsUnableToDrag;
  private boolean mIgnoreGutter;
  private int mDefaultGutterSize;
  private int mGutterSize;
  private int mTouchSlop;
  private float mInitialMotionX;

  /** Position of the last motion event. */
  private float mLastMotionX;

  private float mLastMotionY;

  /**
   * ID of the active pointer. This is used to retain consistency during drags/flings if multiple
   * pointers are used.
   */
  private int mActivePointerId = INVALID_POINTER;

  /** Sentinel value for no current active pointer. Used by {@link #mActivePointerId}. */
  private static final int INVALID_POINTER = -1;

  /** Determines speed during touch scrolling */
  private VelocityTracker mVelocityTracker;

  private int mMinimumVelocity;
  private int mMaximumVelocity;
  private int mFlingDistance;
  private int mCloseEnough;

  // If the pager is at least this close to its final position, complete the scroll
  // on touch down and let the user interact with the content inside instead of
  // "catching" the flinging pager.
  private static final int CLOSE_ENOUGH = 2; // dp

  private boolean mFakeDragging;
  private long mFakeDragBeginTime;

  private EdgeEffect mLeftEdge;
  private EdgeEffect mRightEdge;

  private boolean mFirstLayout = true;
  private boolean mNeedCalculatePageOffsets = false;
  private boolean mCalledSuper;
  private int mDecorChildCount;

  private OnPageChangeListener mOnPageChangeListener;
  private OnPageChangeListener mInternalPageChangeListener;
  private OnAdapterChangeListener mAdapterChangeListener;

  /**
   * Indicates that the pager is in an idle, settled state. The current page is fully in view and no
   * animation is in progress.
   */
  public static final int SCROLL_STATE_IDLE = 0;

  /** Indicates that the pager is currently being dragged by the user. */
  public static final int SCROLL_STATE_DRAGGING = 1;

  /** Indicates that the pager is in the process of settling to a final position. */
  public static final int SCROLL_STATE_SETTLING = 2;

  private int mScrollState = SCROLL_STATE_IDLE;

  /** Callback interface for responding to changing state of the selected page. */
  public interface OnPageChangeListener {

    /**
     * This method will be invoked when the current page is scrolled, either as part of a
     * programmatically initiated smooth scroll or a user initiated touch scroll.
     *
     * @param position Position index of the first page currently being displayed. Page position+1
     *     will be visible if positionOffset is nonzero.
     * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
     * @param positionOffsetPixels Value in pixels indicating the offset from position.
     */
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

    /**
     * This method will be invoked when a new page becomes selected. Animation is not necessarily
     * complete.
     *
     * @param position Position index of the new selected page.
     */
    public void onPageSelected(int position);

    /**
     * Called when the scroll state changes. Useful for discovering when the user begins dragging,
     * when the pager is automatically settling to the current page, or when it is fully
     * stopped/idle.
     *
     * @param state The new scroll state.
     * @see ViewPager#SCROLL_STATE_IDLE
     * @see ViewPager#SCROLL_STATE_DRAGGING
     * @see ViewPager#SCROLL_STATE_SETTLING
     */
    public void onPageScrollStateChanged(int state);
  }

  /**
   * Simple implementation of the {@link OnPageChangeListener} interface with stub implementations
   * of each method. Extend this if you do not intend to override every method of {@link
   * OnPageChangeListener}.
   */
  public static class SimpleOnPageChangeListener implements OnPageChangeListener {
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      // This space for rent
    }

    @Override
    public void onPageSelected(int position) {
      // This space for rent
    }

    @Override
    public void onPageScrollStateChanged(int state) {
      // This space for rent
    }
  }

  /** Used internally to monitor when adapters are switched. */
  interface OnAdapterChangeListener {
    public void onAdapterChanged(PagerAdapter oldAdapter, PagerAdapter newAdapter);
  }

  /**
   * Used internally to tag special types of child views that should be added as pager decorations
   * by default.
   */
  interface Decor {}

  public ViewPager(Context context) {
    super(context);
    initViewPager();
  }

  public ViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
    initViewPager();
  }

  void initViewPager() {
    setWillNotDraw(false);
    setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    setFocusable(true);
    final Context context = getContext();
    mScroller = new Scroller(context, sInterpolator);
    final ViewConfiguration configuration = ViewConfiguration.get(context);
    mTouchSlop = configuration.getScaledPagingTouchSlop();
    mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
    mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    mLeftEdge = new EdgeEffect(context);
    mRightEdge = new EdgeEffect(context);

    final float density = context.getResources().getDisplayMetrics().density;
    mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
    mCloseEnough = (int) (CLOSE_ENOUGH * density);
    mDefaultGutterSize = (int) (DEFAULT_GUTTER_SIZE * density);

    this.setAccessibilityDelegate(new MyAccessibilityDelegate());

    if (this.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      this.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
  }

  private void setScrollState(int newState) {
    if (mScrollState == newState) {
      return;
    }

    mScrollState = newState;
    if (mOnPageChangeListener != null) {
      mOnPageChangeListener.onPageScrollStateChanged(newState);
    }
  }

  /**
   * Set a PagerAdapter that will supply views for this pager as needed.
   *
   * @param adapter Adapter to use
   */
  public void setAdapter(PagerAdapter adapter) {
    if (mAdapter != null) {
      mAdapter.unregisterDataSetObserver(mObserver);
      mAdapter.startUpdate(this);
      for (int i = 0; i < mItems.size(); i++) {
        final ItemInfo ii = mItems.get(i);
        mAdapter.destroyItem(this, ii.position, ii.object);
      }
      mAdapter.finishUpdate(this);
      mItems.clear();
      removeNonDecorViews();
      mCurItem = 0;
      scrollTo(0, 0);
    }

    final PagerAdapter oldAdapter = mAdapter;
    mAdapter = adapter;

    if (mAdapter != null) {
      if (mObserver == null) {
        mObserver = new PagerObserver();
      }
      mAdapter.registerDataSetObserver(mObserver);
      mPopulatePending = false;
      mFirstLayout = true;
      if (mRestoredCurItem >= 0) {
        mAdapter.restoreState(mRestoredAdapterState, mRestoredClassLoader);
        setCurrentItemInternal(mRestoredCurItem, false, true);
        mRestoredCurItem = -1;
        mRestoredAdapterState = null;
        mRestoredClassLoader = null;
      } else {
        populate();
      }
    }

    if (mAdapterChangeListener != null && oldAdapter != adapter) {
      mAdapterChangeListener.onAdapterChanged(oldAdapter, adapter);
    }
  }

  private void removeNonDecorViews() {
    for (int i = 0; i < getChildCount(); i++) {
      final View child = getChildAt(i);
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      if (!lp.isDecor) {
        removeViewAt(i);
        i--;
      }
    }
  }

  /**
   * Retrieve the current adapter supplying pages.
   *
   * @return The currently registered PagerAdapter
   */
  public PagerAdapter getAdapter() {
    return mAdapter;
  }

  void setOnAdapterChangeListener(OnAdapterChangeListener listener) {
    mAdapterChangeListener = listener;
  }

  /**
   * Set the currently selected page. If the ViewPager has already been through its first layout
   * with its current adapter there will be a smooth animated transition between the current item
   * and the specified item.
   *
   * @param item Item index to select
   */
  public void setCurrentItem(int item) {
    mPopulatePending = false;
    setCurrentItemInternal(item, !mFirstLayout, false);
  }

  /**
   * Set the currently selected page.
   *
   * @param item Item index to select
   * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
   */
  public void setCurrentItem(int item, boolean smoothScroll) {
    mPopulatePending = false;
    setCurrentItemInternal(item, smoothScroll, false);
  }

  public int getCurrentItem() {
    return mCurItem;
  }

  void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
    setCurrentItemInternal(item, smoothScroll, always, 0);
  }

  void setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
    if (mAdapter == null || mAdapter.getCount() <= 0) {
      setScrollingCacheEnabled(false);
      return;
    }
    if (!always && mCurItem == item && mItems.size() != 0) {
      setScrollingCacheEnabled(false);
      return;
    }

    if (item < 0) {
      item = 0;
    } else if (item >= mAdapter.getCount()) {
      item = mAdapter.getCount() - 1;
    }
    final int pageLimit = mOffscreenPageLimit;
    if (item > (mCurItem + pageLimit) || item < (mCurItem - pageLimit)) {
      // We are doing a jump by more than one page.  To avoid
      // glitches, we want to keep all current pages in the view
      // until the scroll ends.
      for (int i = 0; i < mItems.size(); i++) {
        mItems.get(i).scrolling = true;
      }
    }
    final boolean dispatchSelected = mCurItem != item;
    populate(item);
    final ItemInfo curInfo = infoForPosition(item);
    int destX = 0;
    if (curInfo != null) {
      final int width = getWidth();
      destX = (int) (width * Math.max(mFirstOffset, Math.min(curInfo.offset, mLastOffset)));
    }
    if (smoothScroll) {
      smoothScrollTo(destX, 0, velocity);
      if (dispatchSelected && mOnPageChangeListener != null) {
        mOnPageChangeListener.onPageSelected(item);
      }
      if (dispatchSelected && mInternalPageChangeListener != null) {
        mInternalPageChangeListener.onPageSelected(item);
      }
    } else {
      if (dispatchSelected && mOnPageChangeListener != null) {
        mOnPageChangeListener.onPageSelected(item);
      }
      if (dispatchSelected && mInternalPageChangeListener != null) {
        mInternalPageChangeListener.onPageSelected(item);
      }
      completeScroll();
      scrollTo(destX, 0);
    }
  }

  /**
   * Set a listener that will be invoked whenever the page changes or is incrementally scrolled. See
   * {@link OnPageChangeListener}.
   *
   * @param listener Listener to set
   */
  public void setOnPageChangeListener(OnPageChangeListener listener) {
    mOnPageChangeListener = listener;
  }

  /**
   * Set a separate OnPageChangeListener for internal use by the support library.
   *
   * @param listener Listener to set
   * @return The old listener that was set, if any.
   */
  OnPageChangeListener setInternalPageChangeListener(OnPageChangeListener listener) {
    OnPageChangeListener oldListener = mInternalPageChangeListener;
    mInternalPageChangeListener = listener;
    return oldListener;
  }

  /**
   * Returns the number of pages that will be retained to either side of the current page in the
   * view hierarchy in an idle state. Defaults to 1.
   *
   * @return How many pages will be kept offscreen on either side
   * @see #setOffscreenPageLimit(int)
   */
  public int getOffscreenPageLimit() {
    return mOffscreenPageLimit;
  }

  /**
   * Set the number of pages that should be retained to either side of the current page in the view
   * hierarchy in an idle state. Pages beyond this limit will be recreated from the adapter when
   * needed.
   *
   * <p>This is offered as an optimization. If you know in advance the number of pages you will need
   * to support or have lazy-loading mechanisms in place on your pages, tweaking this setting can
   * have benefits in perceived smoothness of paging animations and interaction. If you have a small
   * number of pages (3-4) that you can keep active all at once, less time will be spent in layout
   * for newly created view subtrees as the user pages back and forth.
   *
   * <p>You should keep this limit low, especially if your pages have complex layouts. This setting
   * defaults to 1.
   *
   * @param limit How many pages will be kept offscreen in an idle state.
   */
  public void setOffscreenPageLimit(int limit) {
    if (limit < DEFAULT_OFFSCREEN_PAGES) {
      Log.w(
          TAG,
          "Requested offscreen page limit "
              + limit
              + " too small; defaulting to "
              + DEFAULT_OFFSCREEN_PAGES);
      limit = DEFAULT_OFFSCREEN_PAGES;
    }
    if (limit != mOffscreenPageLimit) {
      mOffscreenPageLimit = limit;
      populate();
    }
  }

  /**
   * Set the margin between pages.
   *
   * @param marginPixels Distance between adjacent pages in pixels
   * @see #getPageMargin()
   * @see #setPageMarginDrawable(Drawable)
   * @see #setPageMarginDrawable(int)
   */
  public void setPageMargin(int marginPixels) {
    final int oldMargin = mPageMargin;
    mPageMargin = marginPixels;

    final int width = getWidth();
    recomputeScrollPosition(width, width, marginPixels, oldMargin);

    requestLayout();
  }

  /**
   * Return the margin between pages.
   *
   * @return The size of the margin in pixels
   */
  public int getPageMargin() {
    return mPageMargin;
  }

  /**
   * Set a drawable that will be used to fill the margin between pages.
   *
   * @param d Drawable to display between pages
   */
  public void setPageMarginDrawable(Drawable d) {
    mMarginDrawable = d;
    if (d != null) refreshDrawableState();
    setWillNotDraw(d == null);
    invalidate();
  }

  /**
   * Set a drawable that will be used to fill the margin between pages.
   *
   * @param resId Resource ID of a drawable to display between pages
   */
  public void setPageMarginDrawable(int resId) {
    setPageMarginDrawable(getContext().getResources().getDrawable(resId));
  }

  @Override
  protected boolean verifyDrawable(Drawable who) {
    return super.verifyDrawable(who) || who == mMarginDrawable;
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
    final Drawable d = mMarginDrawable;
    if (d != null && d.isStateful()) {
      d.setState(getDrawableState());
    }
  }

  // We want the duration of the page snap animation to be influenced by the distance that
  // the screen has to travel, however, we don't want this duration to be effected in a
  // purely linear fashion. Instead, we use this method to moderate the effect that the distance
  // of travel has on the overall snap duration.
  float distanceInfluenceForSnapDuration(float f) {
    f -= 0.5f; // center the values about 0.
    f *= 0.3f * Math.PI / 2.0f;
    return (float) Math.sin(f);
  }

  /**
   * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
   *
   * @param x the number of pixels to scroll by on the X axis
   * @param y the number of pixels to scroll by on the Y axis
   */
  void smoothScrollTo(int x, int y) {
    smoothScrollTo(x, y, 0);
  }

  /**
   * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
   *
   * @param x the number of pixels to scroll by on the X axis
   * @param y the number of pixels to scroll by on the Y axis
   * @param velocity the velocity associated with a fling, if applicable. (0 otherwise)
   */
  void smoothScrollTo(int x, int y, int velocity) {
    if (getChildCount() == 0) {
      // Nothing to do.
      setScrollingCacheEnabled(false);
      return;
    }
    int sx = getScrollX();
    int sy = getScrollY();
    int dx = x - sx;
    int dy = y - sy;
    if (dx == 0 && dy == 0) {
      completeScroll();
      populate();
      setScrollState(SCROLL_STATE_IDLE);
      return;
    }

    setScrollingCacheEnabled(true);
    setScrollState(SCROLL_STATE_SETTLING);

    final int width = getWidth();
    final int halfWidth = width / 2;
    final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
    final float distance = halfWidth + halfWidth * distanceInfluenceForSnapDuration(distanceRatio);

    int duration = 0;
    velocity = Math.abs(velocity);
    if (velocity > 0) {
      duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
    } else {
      final float pageWidth = width * mAdapter.getPageWidth(mCurItem);
      final float pageDelta = (float) Math.abs(dx) / (pageWidth + mPageMargin);
      duration = (int) ((pageDelta + 1) * 100);
    }
    duration = Math.min(duration, MAX_SETTLE_DURATION);

    mScroller.startScroll(sx, sy, dx, dy, duration);
    this.postInvalidateOnAnimation();
  }

  ItemInfo addNewItem(int position, int index) {
    ItemInfo ii = new ItemInfo();
    ii.position = position;
    ii.object = mAdapter.instantiateItem(this, position);
    ii.widthFactor = mAdapter.getPageWidth(position);
    if (index < 0 || index >= mItems.size()) {
      mItems.add(ii);
    } else {
      mItems.add(index, ii);
    }
    return ii;
  }

  void dataSetChanged() {
    // This method only gets called if our observer is attached, so mAdapter is non-null.

    boolean needPopulate =
        mItems.size() < mOffscreenPageLimit * 2 + 1 && mItems.size() < mAdapter.getCount();
    int newCurrItem = mCurItem;

    boolean isUpdating = false;
    for (int i = 0; i < mItems.size(); i++) {
      final ItemInfo ii = mItems.get(i);
      final int newPos = mAdapter.getItemPosition(ii.object);

      if (newPos == PagerAdapter.POSITION_UNCHANGED) {
        continue;
      }

      if (newPos == PagerAdapter.POSITION_NONE) {
        mItems.remove(i);
        i--;

        if (!isUpdating) {
          mAdapter.startUpdate(this);
          isUpdating = true;
        }

        mAdapter.destroyItem(this, ii.position, ii.object);
        needPopulate = true;

        if (mCurItem == ii.position) {
          // Keep the current item in the valid range
          newCurrItem = Math.max(0, Math.min(mCurItem, mAdapter.getCount() - 1));
          needPopulate = true;
        }
        continue;
      }

      if (ii.position != newPos) {
        if (ii.position == mCurItem) {
          // Our current item changed position. Follow it.
          newCurrItem = newPos;
        }

        ii.position = newPos;
        needPopulate = true;
      }
    }

    if (isUpdating) {
      mAdapter.finishUpdate(this);
    }

    Collections.sort(mItems, COMPARATOR);

    if (needPopulate) {
      // Reset our known page widths; populate will recompute them.
      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (!lp.isDecor) {
          lp.widthFactor = 0.f;
        }
      }

      setCurrentItemInternal(newCurrItem, false, true);
      requestLayout();
    }
  }

  void populate() {
    populate(mCurItem);
  }

  void populate(int newCurrentItem) {
    ItemInfo oldCurInfo = null;
    if (mCurItem != newCurrentItem) {
      oldCurInfo = infoForPosition(mCurItem);
      mCurItem = newCurrentItem;
    }

    if (mAdapter == null) {
      return;
    }

    // Bail now if we are waiting to populate.  This is to hold off
    // on creating views from the time the user releases their finger to
    // fling to a new position until we have finished the scroll to
    // that position, avoiding glitches from happening at that point.
    if (mPopulatePending) {
      if (DEBUG) Log.i(TAG, "populate is pending, skipping for now...");
      return;
    }

    // Also, don't populate until we are attached to a window.  This is to
    // avoid trying to populate before we have restored our view hierarchy
    // state and conflicting with what is restored.
    if (getWindowToken() == null) {
      return;
    }

    mAdapter.startUpdate(this);

    final int pageLimit = mOffscreenPageLimit;
    final int startPos = Math.max(0, mCurItem - pageLimit);
    final int N = mAdapter.getCount();
    final int endPos = Math.min(N - 1, mCurItem + pageLimit);

    // Locate the currently focused item or add it if needed.
    int curIndex = -1;
    ItemInfo curItem = null;
    for (curIndex = 0; curIndex < mItems.size(); curIndex++) {
      final ItemInfo ii = mItems.get(curIndex);
      if (ii.position >= mCurItem) {
        if (ii.position == mCurItem) curItem = ii;
        break;
      }
    }

    if (curItem == null && N > 0) {
      curItem = addNewItem(mCurItem, curIndex);
    }

    // Fill 3x the available width or up to the number of offscreen
    // pages requested to either side, whichever is larger.
    // If we have no current item we have no work to do.
    if (curItem != null) {
      float extraWidthLeft = 0.f;
      int itemIndex = curIndex - 1;
      ItemInfo ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
      final float leftWidthNeeded = 2.f - curItem.widthFactor;
      for (int pos = mCurItem - 1; pos >= 0; pos--) {
        if (extraWidthLeft >= leftWidthNeeded && pos < startPos) {
          if (ii == null) {
            break;
          }
          if (pos == ii.position && !ii.scrolling) {
            mItems.remove(itemIndex);
            mAdapter.destroyItem(this, pos, ii.object);
            itemIndex--;
            curIndex--;
            ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
          }
        } else if (ii != null && pos == ii.position) {
          extraWidthLeft += ii.widthFactor;
          itemIndex--;
          ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
        } else {
          ii = addNewItem(pos, itemIndex + 1);
          extraWidthLeft += ii.widthFactor;
          curIndex++;
          ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
        }
      }

      float extraWidthRight = curItem.widthFactor;
      itemIndex = curIndex + 1;
      if (extraWidthRight < 2.f) {
        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
        for (int pos = mCurItem + 1; pos < N; pos++) {
          if (extraWidthRight >= 2.f && pos > endPos) {
            if (ii == null) {
              break;
            }
            if (pos == ii.position && !ii.scrolling) {
              mItems.remove(itemIndex);
              mAdapter.destroyItem(this, pos, ii.object);
              ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
            }
          } else if (ii != null && pos == ii.position) {
            extraWidthRight += ii.widthFactor;
            itemIndex++;
            ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
          } else {
            ii = addNewItem(pos, itemIndex);
            itemIndex++;
            extraWidthRight += ii.widthFactor;
            ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
          }
        }
      }

      calculatePageOffsets(curItem, curIndex, oldCurInfo);
    }

    if (DEBUG) {
      Log.i(TAG, "Current page list:");
      for (int i = 0; i < mItems.size(); i++) {
        Log.i(TAG, "#" + i + ": page " + mItems.get(i).position);
      }
    }

    mAdapter.setPrimaryItem(this, mCurItem, curItem != null ? curItem.object : null);

    mAdapter.finishUpdate(this);

    // Check width measurement of current pages. Update LayoutParams as needed.
    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      if (!lp.isDecor && lp.widthFactor == 0.f) {
        // 0 means requery the adapter for this, it doesn't have a valid width.
        final ItemInfo ii = infoForChild(child);
        if (ii != null) {
          lp.widthFactor = ii.widthFactor;
        }
      }
    }

    if (hasFocus()) {
      View currentFocused = findFocus();
      ItemInfo ii = currentFocused != null ? infoForAnyChild(currentFocused) : null;
      if (ii == null || ii.position != mCurItem) {
        for (int i = 0; i < getChildCount(); i++) {
          View child = getChildAt(i);
          ii = infoForChild(child);
          if (ii != null && ii.position == mCurItem) {
            if (child.requestFocus(FOCUS_FORWARD)) {
              break;
            }
          }
        }
      }
    }
  }

  private void calculatePageOffsets(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
    final int N = mAdapter.getCount();
    final int width = getWidth();
    final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;
    // Fix up offsets for later layout.
    if (oldCurInfo != null) {
      final int oldCurPosition = oldCurInfo.position;
      // Base offsets off of oldCurInfo.
      if (oldCurPosition < curItem.position) {
        int itemIndex = 0;
        ItemInfo ii = null;
        float offset = oldCurInfo.offset + oldCurInfo.widthFactor + marginOffset;
        for (int pos = oldCurPosition + 1;
            pos <= curItem.position && itemIndex < mItems.size();
            pos++) {
          ii = mItems.get(itemIndex);
          while (pos > ii.position && itemIndex < mItems.size() - 1) {
            itemIndex++;
            ii = mItems.get(itemIndex);
          }
          while (pos < ii.position) {
            // We don't have an item populated for this,
            // ask the adapter for an offset.
            offset += mAdapter.getPageWidth(pos) + marginOffset;
            pos++;
          }
          ii.offset = offset;
          offset += ii.widthFactor + marginOffset;
        }
      } else if (oldCurPosition > curItem.position) {
        int itemIndex = mItems.size() - 1;
        ItemInfo ii = null;
        float offset = oldCurInfo.offset;
        for (int pos = oldCurPosition - 1; pos >= curItem.position && itemIndex >= 0; pos--) {
          ii = mItems.get(itemIndex);
          while (pos < ii.position && itemIndex > 0) {
            itemIndex--;
            ii = mItems.get(itemIndex);
          }
          while (pos > ii.position) {
            // We don't have an item populated for this,
            // ask the adapter for an offset.
            offset -= mAdapter.getPageWidth(pos) + marginOffset;
            pos--;
          }
          offset -= ii.widthFactor + marginOffset;
          ii.offset = offset;
        }
      }
    }

    // Base all offsets off of curItem.
    final int itemCount = mItems.size();
    float offset = curItem.offset;
    int pos = curItem.position - 1;
    mFirstOffset = curItem.position == 0 ? curItem.offset : -Float.MAX_VALUE;
    mLastOffset =
        curItem.position == N - 1 ? curItem.offset + curItem.widthFactor - 1 : Float.MAX_VALUE;
    // Previous pages
    for (int i = curIndex - 1; i >= 0; i--, pos--) {
      final ItemInfo ii = mItems.get(i);
      while (pos > ii.position) {
        offset -= mAdapter.getPageWidth(pos--) + marginOffset;
      }
      offset -= ii.widthFactor + marginOffset;
      ii.offset = offset;
      if (ii.position == 0) mFirstOffset = offset;
    }
    offset = curItem.offset + curItem.widthFactor + marginOffset;
    pos = curItem.position + 1;
    // Next pages
    for (int i = curIndex + 1; i < itemCount; i++, pos++) {
      final ItemInfo ii = mItems.get(i);
      while (pos < ii.position) {
        offset += mAdapter.getPageWidth(pos++) + marginOffset;
      }
      if (ii.position == N - 1) {
        mLastOffset = offset + ii.widthFactor - 1;
      }
      ii.offset = offset;
      offset += ii.widthFactor + marginOffset;
    }

    mNeedCalculatePageOffsets = false;
  }

  /**
   * This is the persistent state that is saved by ViewPager. Only needed if you are creating a
   * sublass of ViewPager that must save its own state, in which case it should implement a subclass
   * of this which contains that state.
   */
  public static class SavedState extends BaseSavedState {
    int position;
    Parcelable adapterState;
    ClassLoader loader;

    public SavedState(Parcelable superState) {
      super(superState);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeInt(position);
      out.writeParcelable(adapterState, flags);
    }

    @Override
    public String toString() {
      return "FragmentPager.SavedState{"
          + Integer.toHexString(System.identityHashCode(this))
          + " position="
          + position
          + "}";
    }

    public static final Parcelable.Creator<SavedState> CREATOR =
        new Parcelable.Creator<SavedState>() {
          @Override
          public SavedState createFromParcel(Parcel in) {

            return new SavedState(in, null);
          }

          @Override
          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };

    SavedState(Parcel in, ClassLoader loader) {
      super(in);
      if (loader == null) {
        loader = getClass().getClassLoader();
      }
      position = in.readInt();
      adapterState = in.readParcelable(loader);
      this.loader = loader;
    }
  }

  @Override
  public Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState ss = new SavedState(superState);
    ss.position = mCurItem;
    if (mAdapter != null) {
      ss.adapterState = mAdapter.saveState();
    }
    return ss;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    if (!(state instanceof SavedState)) {
      super.onRestoreInstanceState(state);
      return;
    }

    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());

    if (mAdapter != null) {
      mAdapter.restoreState(ss.adapterState, ss.loader);
      setCurrentItemInternal(ss.position, false, true);
    } else {
      mRestoredCurItem = ss.position;
      mRestoredAdapterState = ss.adapterState;
      mRestoredClassLoader = ss.loader;
    }
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    if (!checkLayoutParams(params)) {
      params = generateLayoutParams(params);
    }
    final LayoutParams lp = (LayoutParams) params;
    lp.isDecor |= child instanceof Decor;
    if (mInLayout) {
      if (lp != null && lp.isDecor) {
        throw new IllegalStateException("Cannot add pager decor view during layout");
      }
      lp.needsMeasure = true;
      addViewInLayout(child, index, params);
    } else {
      super.addView(child, index, params);
    }

    if (USE_CACHE) {
      if (child.getVisibility() != GONE) {
        child.setDrawingCacheEnabled(mScrollingCacheEnabled);
      } else {
        child.setDrawingCacheEnabled(false);
      }
    }
  }

  ItemInfo infoForChild(View child) {
    for (int i = 0; i < mItems.size(); i++) {
      ItemInfo ii = mItems.get(i);
      if (mAdapter.isViewFromObject(child, ii.object)) {
        return ii;
      }
    }
    return null;
  }

  ItemInfo infoForAnyChild(View child) {
    ViewParent parent;
    while ((parent = child.getParent()) != this) {
      if (parent == null || !(parent instanceof View)) {
        return null;
      }
      child = (View) parent;
    }
    return infoForChild(child);
  }

  ItemInfo infoForPosition(int position) {
    for (int i = 0; i < mItems.size(); i++) {
      ItemInfo ii = mItems.get(i);
      if (ii.position == position) {
        return ii;
      }
    }
    return null;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    mFirstLayout = true;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // For simple implementation, or internal size is always 0.
    // We depend on the container to specify the layout size of
    // our view.  We can't really know what it is since we will be
    // adding and removing different arbitrary views and do not
    // want the layout to change as this happens.
    setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), getDefaultSize(0, heightMeasureSpec));

    final int measuredWidth = getMeasuredWidth();
    final int maxGutterSize = measuredWidth / 10;
    mGutterSize = Math.min(maxGutterSize, mDefaultGutterSize);

    // Children are just made to fill our space.
    int childWidthSize = measuredWidth - getPaddingLeft() - getPaddingRight();
    int childHeightSize = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

    /*
     * Make sure all children have been properly measured. Decor views first.
     * Right now we cheat and make this less complicated by assuming decor
     * views won't intersect. We will pin to edges based on gravity.
     */
    int size = getChildCount();
    for (int i = 0; i < size; ++i) {
      final View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp != null && lp.isDecor) {
          final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
          final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
          int widthMode = MeasureSpec.AT_MOST;
          int heightMode = MeasureSpec.AT_MOST;
          boolean consumeVertical = vgrav == Gravity.TOP || vgrav == Gravity.BOTTOM;
          boolean consumeHorizontal = hgrav == Gravity.LEFT || hgrav == Gravity.RIGHT;

          if (consumeVertical) {
            widthMode = MeasureSpec.EXACTLY;
          } else if (consumeHorizontal) {
            heightMode = MeasureSpec.EXACTLY;
          }

          int widthSize = childWidthSize;
          int heightSize = childHeightSize;
          if (lp.width != LayoutParams.WRAP_CONTENT) {
            widthMode = MeasureSpec.EXACTLY;
            if (lp.width != LayoutParams.FILL_PARENT) {
              widthSize = lp.width;
            }
          }
          if (lp.height != LayoutParams.WRAP_CONTENT) {
            heightMode = MeasureSpec.EXACTLY;
            if (lp.height != LayoutParams.FILL_PARENT) {
              heightSize = lp.height;
            }
          }
          final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, widthMode);
          final int heightSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
          child.measure(widthSpec, heightSpec);

          if (consumeVertical) {
            childHeightSize -= child.getMeasuredHeight();
          } else if (consumeHorizontal) {
            childWidthSize -= child.getMeasuredWidth();
          }
        }
      }
    }

    mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
    mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);

    // Make sure we have created all fragments that we need to have shown.
    mInLayout = true;
    populate();
    mInLayout = false;

    // Page views next.
    size = getChildCount();
    for (int i = 0; i < size; ++i) {
      final View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        if (DEBUG) Log.v(TAG, "Measuring #" + i + " " + child + ": " + mChildWidthMeasureSpec);

        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp == null || !lp.isDecor) {
          final int widthSpec =
              MeasureSpec.makeMeasureSpec(
                  (int) (childWidthSize * lp.widthFactor), MeasureSpec.EXACTLY);
          child.measure(widthSpec, mChildHeightMeasureSpec);
        }
      }
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    // Make sure scroll position is set correctly.
    if (w != oldw) {
      recomputeScrollPosition(w, oldw, mPageMargin, mPageMargin);
    }
  }

  private void recomputeScrollPosition(int width, int oldWidth, int margin, int oldMargin) {
    if (oldWidth > 0 && !mItems.isEmpty()) {
      final int widthWithMargin = width + margin;
      final int oldWidthWithMargin = oldWidth + oldMargin;
      final int xpos = getScrollX();
      final float pageOffset = (float) xpos / oldWidthWithMargin;
      final int newOffsetPixels = (int) (pageOffset * widthWithMargin);

      scrollTo(newOffsetPixels, getScrollY());
      if (!mScroller.isFinished()) {
        // We now return to your regularly scheduled scroll, already in progress.
        final int newDuration = mScroller.getDuration() - mScroller.timePassed();
        ItemInfo targetInfo = infoForPosition(mCurItem);
        mScroller.startScroll(
            newOffsetPixels, 0, (int) (targetInfo.offset * width), 0, newDuration);
      }
    } else {
      final ItemInfo ii = infoForPosition(mCurItem);
      final float scrollOffset = ii != null ? Math.min(ii.offset, mLastOffset) : 0;
      final int scrollPos = (int) (scrollOffset * width);
      if (scrollPos != getScrollX()) {
        completeScroll();
        scrollTo(scrollPos, getScrollY());
      }
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    mInLayout = true;
    populate();
    mInLayout = false;

    final int count = getChildCount();
    int width = r - l;
    int height = b - t;
    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();
    int paddingRight = getPaddingRight();
    int paddingBottom = getPaddingBottom();
    final int scrollX = getScrollX();

    int decorCount = 0;

    // First pass - decor views. We need to do this in two passes so that
    // we have the proper offsets for non-decor views later.
    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int childLeft = 0;
        int childTop = 0;
        if (lp.isDecor) {
          final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
          final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
          switch (hgrav) {
            default:
              childLeft = paddingLeft;
              break;
            case Gravity.LEFT:
              childLeft = paddingLeft;
              paddingLeft += child.getMeasuredWidth();
              break;
            case Gravity.CENTER_HORIZONTAL:
              childLeft = Math.max((width - child.getMeasuredWidth()) / 2, paddingLeft);
              break;
            case Gravity.RIGHT:
              childLeft = width - paddingRight - child.getMeasuredWidth();
              paddingRight += child.getMeasuredWidth();
              break;
          }
          switch (vgrav) {
            default:
              childTop = paddingTop;
              break;
            case Gravity.TOP:
              childTop = paddingTop;
              paddingTop += child.getMeasuredHeight();
              break;
            case Gravity.CENTER_VERTICAL:
              childTop = Math.max((height - child.getMeasuredHeight()) / 2, paddingTop);
              break;
            case Gravity.BOTTOM:
              childTop = height - paddingBottom - child.getMeasuredHeight();
              paddingBottom += child.getMeasuredHeight();
              break;
          }
          childLeft += scrollX;
          child.layout(
              childLeft,
              childTop,
              childLeft + child.getMeasuredWidth(),
              childTop + child.getMeasuredHeight());
          decorCount++;
        }
      }
    }

    // Page views. Do this once we have the right padding offsets from above.
    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        ItemInfo ii;
        if (!lp.isDecor && (ii = infoForChild(child)) != null) {
          int loff = (int) (width * ii.offset);
          int childLeft = paddingLeft + loff;
          int childTop = paddingTop;
          if (lp.needsMeasure) {
            // This was added during layout and needs measurement.
            // Do it now that we know what we're working with.
            lp.needsMeasure = false;
            final int widthSpec =
                MeasureSpec.makeMeasureSpec(
                    (int) ((width - paddingLeft - paddingRight) * lp.widthFactor),
                    MeasureSpec.EXACTLY);
            final int heightSpec =
                MeasureSpec.makeMeasureSpec(
                    (int) (height - paddingTop - paddingBottom), MeasureSpec.EXACTLY);
            child.measure(widthSpec, heightSpec);
          }
          if (DEBUG)
            Log.v(
                TAG,
                "Positioning #"
                    + i
                    + " "
                    + child
                    + " f="
                    + ii.object
                    + ":"
                    + childLeft
                    + ","
                    + childTop
                    + " "
                    + child.getMeasuredWidth()
                    + "x"
                    + child.getMeasuredHeight());
          child.layout(
              childLeft,
              childTop,
              childLeft + child.getMeasuredWidth(),
              childTop + child.getMeasuredHeight());
        }
      }
    }
    mTopPageBounds = paddingTop;
    mBottomPageBounds = height - paddingBottom;
    mDecorChildCount = decorCount;
    mFirstLayout = false;
  }

  @Override
  public void computeScroll() {
    if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
      int oldX = getScrollX();
      int oldY = getScrollY();
      int x = mScroller.getCurrX();
      int y = mScroller.getCurrY();

      if (oldX != x || oldY != y) {
        scrollTo(x, y);
        if (!pageScrolled(x)) {
          mScroller.abortAnimation();
          scrollTo(0, y);
        }
      }

      // Keep on drawing until the animation has finished.
      this.postInvalidateOnAnimation();
      return;
    }

    // Done with scroll, clean up state.
    completeScroll();
  }

  private boolean pageScrolled(int xpos) {
    if (mItems.size() == 0) {
      mCalledSuper = false;
      onPageScrolled(0, 0, 0);
      if (!mCalledSuper) {
        throw new IllegalStateException("onPageScrolled did not call superclass implementation");
      }
      return false;
    }
    final ItemInfo ii = infoForCurrentScrollPosition();
    final int width = getWidth();
    final int widthWithMargin = width + mPageMargin;
    final float marginOffset = (float) mPageMargin / width;
    final int currentPage = ii.position;
    final float pageOffset = (((float) xpos / width) - ii.offset) / (ii.widthFactor + marginOffset);
    final int offsetPixels = (int) (pageOffset * widthWithMargin);

    mCalledSuper = false;
    onPageScrolled(currentPage, pageOffset, offsetPixels);
    if (!mCalledSuper) {
      throw new IllegalStateException("onPageScrolled did not call superclass implementation");
    }
    return true;
  }

  /**
   * This method will be invoked when the current page is scrolled, either as part of a
   * programmatically initiated smooth scroll or a user initiated touch scroll. If you override this
   * method you must call through to the superclass implementation (e.g.
   * super.onPageScrolled(position, offset, offsetPixels)) before onPageScrolled returns.
   *
   * @param position Position index of the first page currently being displayed. Page position+1
   *     will be visible if positionOffset is nonzero.
   * @param offset Value from [0, 1) indicating the offset from the page at position.
   * @param offsetPixels Value in pixels indicating the offset from position.
   */
  protected void onPageScrolled(int position, float offset, int offsetPixels) {
    // Offset any decor views if needed - keep them on-screen at all times.
    if (mDecorChildCount > 0) {
      final int scrollX = getScrollX();
      int paddingLeft = getPaddingLeft();
      int paddingRight = getPaddingRight();
      final int width = getWidth();
      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (!lp.isDecor) continue;

        final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        int childLeft = 0;
        switch (hgrav) {
          default:
            childLeft = paddingLeft;
            break;
          case Gravity.LEFT:
            childLeft = paddingLeft;
            paddingLeft += child.getWidth();
            break;
          case Gravity.CENTER_HORIZONTAL:
            childLeft = Math.max((width - child.getMeasuredWidth()) / 2, paddingLeft);
            break;
          case Gravity.RIGHT:
            childLeft = width - paddingRight - child.getMeasuredWidth();
            paddingRight += child.getMeasuredWidth();
            break;
        }
        childLeft += scrollX;

        final int childOffset = childLeft - child.getLeft();
        if (childOffset != 0) {
          child.offsetLeftAndRight(childOffset);
        }
      }
    }

    if (mOnPageChangeListener != null) {
      mOnPageChangeListener.onPageScrolled(position, offset, offsetPixels);
    }
    if (mInternalPageChangeListener != null) {
      mInternalPageChangeListener.onPageScrolled(position, offset, offsetPixels);
    }
    mCalledSuper = true;
  }

  private void completeScroll() {
    boolean needPopulate = mScrollState == SCROLL_STATE_SETTLING;
    if (needPopulate) {
      // Done with scroll, no longer want to cache view drawing.
      setScrollingCacheEnabled(false);
      mScroller.abortAnimation();
      int oldX = getScrollX();
      int oldY = getScrollY();
      int x = mScroller.getCurrX();
      int y = mScroller.getCurrY();
      if (oldX != x || oldY != y) {
        scrollTo(x, y);
      }
      setScrollState(SCROLL_STATE_IDLE);
    }
    mPopulatePending = false;
    for (int i = 0; i < mItems.size(); i++) {
      ItemInfo ii = mItems.get(i);
      if (ii.scrolling) {
        needPopulate = true;
        ii.scrolling = false;
      }
    }
    if (needPopulate) {
      populate();
    }
  }

  private boolean isGutterDrag(float x, float dx) {
    return (x < mGutterSize && dx > 0) || (x > getWidth() - mGutterSize && dx < 0);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    /*
     * This method JUST determines whether we want to intercept the motion.
     * If we return true, onMotionEvent will be called and we do the actual
     * scrolling there.
     */

    final int action = ev.getAction() & MotionEvent.ACTION_MASK;

    // Always take care of the touch gesture being complete.
    if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
      // Release the drag.
      if (DEBUG) Log.v(TAG, "Intercept done!");
      mIsBeingDragged = false;
      mIsUnableToDrag = false;
      mActivePointerId = INVALID_POINTER;
      if (mVelocityTracker != null) {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
      }
      return false;
    }

    // Nothing more to do here if we have decided whether or not we
    // are dragging.
    if (action != MotionEvent.ACTION_DOWN) {
      if (mIsBeingDragged) {
        if (DEBUG) Log.v(TAG, "Intercept returning true!");
        return true;
      }
      if (mIsUnableToDrag) {
        if (DEBUG) Log.v(TAG, "Intercept returning false!");
        return false;
      }
    }

    switch (action) {
      case MotionEvent.ACTION_MOVE:
        {
          /*
           * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
           * whether the user has moved far enough from his original down touch.
           */

          /*
           * Locally do absolute value. mLastMotionY is set to the y value
           * of the down event.
           */
          final int activePointerId = mActivePointerId;
          if (activePointerId == INVALID_POINTER) {
            // If we don't have a valid id, the touch down wasn't on content.
            break;
          }

          final int pointerIndex = ev.findPointerIndex(activePointerId);
          final float x = ev.getX(pointerIndex);
          final float dx = x - mLastMotionX;
          final float xDiff = Math.abs(dx);
          final float y = ev.getY(pointerIndex);
          final float yDiff = Math.abs(y - mLastMotionY);
          if (DEBUG) Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);

          if (dx != 0
              && !isGutterDrag(mLastMotionX, dx)
              && canScroll(this, false, (int) dx, (int) x, (int) y)) {
            // Nested view has scrollable area under this point. Let it be handled there.
            mInitialMotionX = mLastMotionX = x;
            mLastMotionY = y;
            mIsUnableToDrag = true;
            return false;
          }
          if (xDiff > mTouchSlop && xDiff > yDiff) {
            if (DEBUG) Log.v(TAG, "Starting drag!");
            mIsBeingDragged = true;
            setScrollState(SCROLL_STATE_DRAGGING);
            mLastMotionX = dx > 0 ? mInitialMotionX + mTouchSlop : mInitialMotionX - mTouchSlop;
            setScrollingCacheEnabled(true);
          } else {
            if (yDiff > mTouchSlop) {
              // The finger has moved enough in the vertical
              // direction to be counted as a drag...  abort
              // any attempt to drag horizontally, to work correctly
              // with children that have scrolling containers.
              if (DEBUG) Log.v(TAG, "Starting unable to drag!");
              mIsUnableToDrag = true;
            }
          }
          if (mIsBeingDragged) {
            // Scroll to follow the motion event
            if (performDrag(x)) {
              this.postInvalidateOnAnimation();
            }
          }
          break;
        }

      case MotionEvent.ACTION_DOWN:
        {
          /*
           * Remember location of down touch.
           * ACTION_DOWN always refers to pointer index 0.
           */
          mLastMotionX = mInitialMotionX = ev.getX();
          mLastMotionY = ev.getY();
          mActivePointerId = ev.getPointerId(0);
          mIsUnableToDrag = false;

          mScroller.computeScrollOffset();
          if (mScrollState == SCROLL_STATE_SETTLING
              && Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) > mCloseEnough) {
            // Let the user 'catch' the pager as it animates.
            mScroller.abortAnimation();
            mPopulatePending = false;
            populate();
            mIsBeingDragged = true;
            setScrollState(SCROLL_STATE_DRAGGING);
          } else {
            completeScroll();
            mIsBeingDragged = false;
          }

          if (DEBUG)
            Log.v(
                TAG,
                "Down at "
                    + mLastMotionX
                    + ","
                    + mLastMotionY
                    + " mIsBeingDragged="
                    + mIsBeingDragged
                    + "mIsUnableToDrag="
                    + mIsUnableToDrag);
          break;
        }

      case MotionEvent.ACTION_POINTER_UP:
        onSecondaryPointerUp(ev);
        break;
    }

    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(ev);

    /*
     * The only time we want to intercept motion events is if we are in the
     * drag mode.
     */
    return mIsBeingDragged;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (mFakeDragging) {
      // A fake drag is in progress already, ignore this real one
      // but still eat the touch events.
      // (It is likely that the user is multi-touching the screen.)
      return true;
    }

    if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
      // Don't handle edge touches immediately -- they may actually belong to one of our
      // descendants.
      return false;
    }

    if (mAdapter == null || mAdapter.getCount() == 0) {
      // Nothing to present or scroll; nothing to touch.
      return false;
    }

    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(ev);

    final int action = ev.getAction();
    boolean needsInvalidate = false;

    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN:
        {
          mScroller.abortAnimation();
          mPopulatePending = false;
          populate();
          mIsBeingDragged = true;
          setScrollState(SCROLL_STATE_DRAGGING);

          // Remember where the motion event started
          mLastMotionX = mInitialMotionX = ev.getX();
          mActivePointerId = ev.getPointerId(0);
          break;
        }
      case MotionEvent.ACTION_MOVE:
        if (!mIsBeingDragged) {
          final int pointerIndex = ev.findPointerIndex(mActivePointerId);
          final float x = ev.getX(pointerIndex);
          final float xDiff = Math.abs(x - mLastMotionX);
          final float y = ev.getY(pointerIndex);
          final float yDiff = Math.abs(y - mLastMotionY);
          if (DEBUG) Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);
          if (xDiff > mTouchSlop && xDiff > yDiff) {
            if (DEBUG) Log.v(TAG, "Starting drag!");
            mIsBeingDragged = true;
            mLastMotionX =
                x - mInitialMotionX > 0
                    ? mInitialMotionX + mTouchSlop
                    : mInitialMotionX - mTouchSlop;
            setScrollState(SCROLL_STATE_DRAGGING);
            setScrollingCacheEnabled(true);
          }
        }
        // Not else! Note that mIsBeingDragged can be set above.
        if (mIsBeingDragged) {
          // Scroll to follow the motion event
          final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
          final float x = ev.getX(activePointerIndex);
          needsInvalidate |= performDrag(x);
        }
        break;
      case MotionEvent.ACTION_UP:
        if (mIsBeingDragged) {
          final VelocityTracker velocityTracker = mVelocityTracker;
          velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
          int initialVelocity = (int) velocityTracker.getXVelocity(mActivePointerId);
          mPopulatePending = true;
          final int width = getWidth();
          final int scrollX = getScrollX();
          final ItemInfo ii = infoForCurrentScrollPosition();
          final int currentPage = ii.position;
          final float pageOffset = (((float) scrollX / width) - ii.offset) / ii.widthFactor;
          final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
          final float x = ev.getX(activePointerIndex);
          final int totalDelta = (int) (x - mInitialMotionX);
          int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity, totalDelta);
          setCurrentItemInternal(nextPage, true, true, initialVelocity);

          mActivePointerId = INVALID_POINTER;
          endDrag();
          needsInvalidate = edgeEffectOnRelase(mLeftEdge) | edgeEffectOnRelase(mRightEdge);
        }
        break;
      case MotionEvent.ACTION_CANCEL:
        if (mIsBeingDragged) {
          setCurrentItemInternal(mCurItem, true, true);
          mActivePointerId = INVALID_POINTER;
          endDrag();
          needsInvalidate = edgeEffectOnRelase(mLeftEdge) | edgeEffectOnRelase(mRightEdge);
        }
        break;
      case MotionEvent.ACTION_POINTER_DOWN:
        {
          final int index = ev.getActionIndex();
          final float x = ev.getX(index);
          mLastMotionX = x;
          mActivePointerId = ev.getPointerId(index);
          break;
        }
      case MotionEvent.ACTION_POINTER_UP:
        onSecondaryPointerUp(ev);
        mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId));
        break;
    }
    if (needsInvalidate) {
      this.postInvalidateOnAnimation();
    }
    return true;
  }

  private boolean edgeEffectOnRelase(EdgeEffect ee) {
    ee.onRelease();
    return ee.isFinished();
  }

  private boolean performDrag(float x) {
    boolean needsInvalidate = false;

    final float deltaX = mLastMotionX - x;
    mLastMotionX = x;

    float oldScrollX = getScrollX();
    float scrollX = oldScrollX + deltaX;
    final int width = getWidth();

    float leftBound = width * mFirstOffset;
    float rightBound = width * mLastOffset;
    boolean leftAbsolute = true;
    boolean rightAbsolute = true;

    final ItemInfo firstItem = mItems.get(0);
    final ItemInfo lastItem = mItems.get(mItems.size() - 1);
    if (firstItem.position != 0) {
      leftAbsolute = false;
      leftBound = firstItem.offset * width;
    }
    if (lastItem.position != mAdapter.getCount() - 1) {
      rightAbsolute = false;
      rightBound = lastItem.offset * width;
    }

    if (scrollX < leftBound) {
      if (leftAbsolute) {
        float over = leftBound - scrollX;
        needsInvalidate = edgeEffectOnPull(mLeftEdge, (Math.abs(over) / width));
      }
      scrollX = leftBound;
    } else if (scrollX > rightBound) {
      if (rightAbsolute) {
        float over = scrollX - rightBound;
        needsInvalidate = edgeEffectOnPull(mRightEdge, (Math.abs(over) / width));
      }
      scrollX = rightBound;
    }
    // Don't lose the rounded component
    mLastMotionX += scrollX - (int) scrollX;
    scrollTo((int) scrollX, getScrollY());
    pageScrolled((int) scrollX);

    return needsInvalidate;
  }

  private boolean edgeEffectOnPull(EdgeEffect ee, float deltaDistance) {
    ee.onPull(deltaDistance);
    return true;
  }

  /**
   * @return Info about the page at the current scroll position. This can be synthetic for a missing
   *     middle page; the 'object' field can be null.
   */
  private ItemInfo infoForCurrentScrollPosition() {
    final int width = getWidth();
    final float scrollOffset = width > 0 ? (float) getScrollX() / width : 0;
    final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;
    int lastPos = -1;
    float lastOffset = 0.f;
    float lastWidth = 0.f;
    boolean first = true;

    ItemInfo lastItem = null;
    for (int i = 0; i < mItems.size(); i++) {
      ItemInfo ii = mItems.get(i);
      float offset;
      if (!first && ii.position != lastPos + 1) {
        // Create a synthetic item for a missing page.
        ii = mTempItem;
        ii.offset = lastOffset + lastWidth + marginOffset;
        ii.position = lastPos + 1;
        ii.widthFactor = mAdapter.getPageWidth(ii.position);
        i--;
      }
      offset = ii.offset;

      final float leftBound = offset;
      final float rightBound = offset + ii.widthFactor + marginOffset;
      if (first || scrollOffset >= leftBound) {
        if (scrollOffset < rightBound || i == mItems.size() - 1) {
          return ii;
        }
      } else {
        return lastItem;
      }
      first = false;
      lastPos = ii.position;
      lastOffset = offset;
      lastWidth = ii.widthFactor;
      lastItem = ii;
    }

    return lastItem;
  }

  private int determineTargetPage(int currentPage, float pageOffset, int velocity, int deltaX) {
    int targetPage;
    if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
      targetPage = velocity > 0 ? currentPage : currentPage + 1;
    } else {
      targetPage = (int) (currentPage + pageOffset + 0.5f);
    }

    if (mItems.size() > 0) {
      final ItemInfo firstItem = mItems.get(0);
      final ItemInfo lastItem = mItems.get(mItems.size() - 1);

      // Only let the user target pages we have items for
      targetPage = Math.max(firstItem.position, Math.min(targetPage, lastItem.position));
    }

    return targetPage;
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    boolean needsInvalidate = false;

    final int overScrollMode = this.getOverScrollMode();
    if (overScrollMode == View.OVER_SCROLL_ALWAYS
        || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS
            && mAdapter != null
            && mAdapter.getCount() > 1)) {
      if (!mLeftEdge.isFinished()) {
        final int restoreCount = canvas.save();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();
        final int width = getWidth();

        canvas.rotate(270);
        canvas.translate(-height + getPaddingTop(), mFirstOffset * width);
        mLeftEdge.setSize(height, width);
        needsInvalidate |= mLeftEdge.draw(canvas);
        canvas.restoreToCount(restoreCount);
      }
      if (!mRightEdge.isFinished()) {
        final int restoreCount = canvas.save();
        final int width = getWidth();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();

        canvas.rotate(90);
        canvas.translate(-getPaddingTop(), -(mLastOffset + 1) * width);
        mRightEdge.setSize(height, width);
        needsInvalidate |= mRightEdge.draw(canvas);
        canvas.restoreToCount(restoreCount);
      }
    } else {
      mLeftEdge.finish();
      mRightEdge.finish();
    }

    if (needsInvalidate) {
      // Keep animating
      this.postInvalidateOnAnimation();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    // Draw the margin drawable between pages if needed.
    if (mPageMargin > 0 && mMarginDrawable != null && mItems.size() > 0 && mAdapter != null) {
      final int scrollX = getScrollX();
      final int width = getWidth();

      final float marginOffset = (float) mPageMargin / width;
      int itemIndex = 0;
      ItemInfo ii = mItems.get(0);
      float offset = ii.offset;
      final int itemCount = mItems.size();
      final int firstPos = ii.position;
      final int lastPos = mItems.get(itemCount - 1).position;
      for (int pos = firstPos; pos < lastPos; pos++) {
        while (pos > ii.position && itemIndex < itemCount) {
          ii = mItems.get(++itemIndex);
        }

        float drawAt;
        if (pos == ii.position) {
          drawAt = (ii.offset + ii.widthFactor) * width;
          offset = ii.offset + ii.widthFactor + marginOffset;
        } else {
          float widthFactor = mAdapter.getPageWidth(pos);
          drawAt = (offset + widthFactor) * width;
          offset += widthFactor + marginOffset;
        }

        if (drawAt + mPageMargin > scrollX) {
          mMarginDrawable.setBounds(
              (int) drawAt, mTopPageBounds, (int) (drawAt + mPageMargin + 0.5f), mBottomPageBounds);
          mMarginDrawable.draw(canvas);
        }

        if (drawAt > scrollX + width) {
          break; // No more visible, no sense in continuing
        }
      }
    }
  }

  /**
   * Start a fake drag of the pager.
   *
   * <p>A fake drag can be useful if you want to synchronize the motion of the ViewPager with the
   * touch scrolling of another view, while still letting the ViewPager control the snapping motion
   * and fling behavior. (e.g. parallax-scrolling tabs.) Call {@link #fakeDragBy(float)} to simulate
   * the actual drag motion. Call {@link #endFakeDrag()} to complete the fake drag and fling as
   * necessary.
   *
   * <p>During a fake drag the ViewPager will ignore all touch events. If a real drag is already in
   * progress, this method will return false.
   *
   * @return true if the fake drag began successfully, false if it could not be started.
   * @see #fakeDragBy(float)
   * @see #endFakeDrag()
   */
  public boolean beginFakeDrag() {
    if (mIsBeingDragged) {
      return false;
    }
    mFakeDragging = true;
    setScrollState(SCROLL_STATE_DRAGGING);
    mInitialMotionX = mLastMotionX = 0;
    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    } else {
      mVelocityTracker.clear();
    }
    final long time = SystemClock.uptimeMillis();
    final MotionEvent ev = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0, 0, 0);
    mVelocityTracker.addMovement(ev);
    ev.recycle();
    mFakeDragBeginTime = time;
    return true;
  }

  /**
   * End a fake drag of the pager.
   *
   * @see #beginFakeDrag()
   * @see #fakeDragBy(float)
   */
  public void endFakeDrag() {
    if (!mFakeDragging) {
      throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
    }

    final VelocityTracker velocityTracker = mVelocityTracker;
    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
    int initialVelocity = (int) velocityTracker.getXVelocity(mActivePointerId);
    mPopulatePending = true;
    final int width = getWidth();
    final int scrollX = getScrollX();
    final ItemInfo ii = infoForCurrentScrollPosition();
    final int currentPage = ii.position;
    final float pageOffset = (((float) scrollX / width) - ii.offset) / ii.widthFactor;
    final int totalDelta = (int) (mLastMotionX - mInitialMotionX);
    int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity, totalDelta);
    setCurrentItemInternal(nextPage, true, true, initialVelocity);
    endDrag();

    mFakeDragging = false;
  }

  /**
   * Fake drag by an offset in pixels. You must have called {@link #beginFakeDrag()} first.
   *
   * @param xOffset Offset in pixels to drag by.
   * @see #beginFakeDrag()
   * @see #endFakeDrag()
   */
  public void fakeDragBy(float xOffset) {
    if (!mFakeDragging) {
      throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
    }

    mLastMotionX += xOffset;

    float oldScrollX = getScrollX();
    float scrollX = oldScrollX - xOffset;
    final int width = getWidth();

    float leftBound = width * mFirstOffset;
    float rightBound = width * mLastOffset;

    final ItemInfo firstItem = mItems.get(0);
    final ItemInfo lastItem = mItems.get(mItems.size() - 1);
    if (firstItem.position != 0) {
      leftBound = firstItem.offset * width;
    }
    if (lastItem.position != mAdapter.getCount() - 1) {
      rightBound = lastItem.offset * width;
    }

    if (scrollX < leftBound) {
      scrollX = leftBound;
    } else if (scrollX > rightBound) {
      scrollX = rightBound;
    }
    // Don't lose the rounded component
    mLastMotionX += scrollX - (int) scrollX;
    scrollTo((int) scrollX, getScrollY());
    pageScrolled((int) scrollX);

    // Synthesize an event for the VelocityTracker.
    final long time = SystemClock.uptimeMillis();
    final MotionEvent ev =
        MotionEvent.obtain(mFakeDragBeginTime, time, MotionEvent.ACTION_MOVE, mLastMotionX, 0, 0);
    mVelocityTracker.addMovement(ev);
    ev.recycle();
  }

  /**
   * Returns true if a fake drag is in progress.
   *
   * @return true if currently in a fake drag, false otherwise.
   * @see #beginFakeDrag()
   * @see #fakeDragBy(float)
   * @see #endFakeDrag()
   */
  public boolean isFakeDragging() {
    return mFakeDragging;
  }

  private void onSecondaryPointerUp(MotionEvent ev) {
    final int pointerIndex = ev.getActionIndex();
    final int pointerId = ev.getPointerId(pointerIndex);
    if (pointerId == mActivePointerId) {
      // This was our active pointer going up. Choose a new
      // active pointer and adjust accordingly.
      final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
      mLastMotionX = ev.getX(newPointerIndex);
      mActivePointerId = ev.getPointerId(newPointerIndex);
      if (mVelocityTracker != null) {
        mVelocityTracker.clear();
      }
    }
  }

  private void endDrag() {
    mIsBeingDragged = false;
    mIsUnableToDrag = false;

    if (mVelocityTracker != null) {
      mVelocityTracker.recycle();
      mVelocityTracker = null;
    }
  }

  private void setScrollingCacheEnabled(boolean enabled) {
    if (mScrollingCacheEnabled != enabled) {
      mScrollingCacheEnabled = enabled;
      if (USE_CACHE) {
        final int size = getChildCount();
        for (int i = 0; i < size; ++i) {
          final View child = getChildAt(i);
          if (child.getVisibility() != GONE) {
            child.setDrawingCacheEnabled(enabled);
          }
        }
      }
    }
  }

  /**
   * Tests scrollability within child views of v given a delta of dx.
   *
   * @param v View to test for horizontal scrollability
   * @param checkV Whether the view v passed should itself be checked for scrollability (true), or
   *     just its children (false).
   * @param dx Delta scrolled in pixels
   * @param x X coordinate of the active touch point
   * @param y Y coordinate of the active touch point
   * @return true if child views of v can be scrolled by delta of dx.
   */
  protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
    if (v instanceof ViewGroup) {
      final ViewGroup group = (ViewGroup) v;
      final int scrollX = v.getScrollX();
      final int scrollY = v.getScrollY();
      final int count = group.getChildCount();
      // Count backwards - let topmost views consume scroll distance first.
      for (int i = count - 1; i >= 0; i--) {
        // TODO: Add versioned support here for transformed views.
        // This will not work for transformed views in Honeycomb+
        final View child = group.getChildAt(i);
        if (x + scrollX >= child.getLeft()
            && x + scrollX < child.getRight()
            && y + scrollY >= child.getTop()
            && y + scrollY < child.getBottom()
            && canScroll(
                child, true, dx, x + scrollX - child.getLeft(), y + scrollY - child.getTop())) {
          return true;
        }
      }
    }

    return checkV && viewCanScrollHorizontally(v, -dx);
  }

  private boolean viewCanScrollHorizontally(View v, int direction) {
    return (v instanceof ScrollingView)
        && viewCanScrollingHorizontally((ScrollingView) v, direction);
  }

  private boolean viewCanScrollingHorizontally(ScrollingView v, int direction) {
    final int offset = v.computeHorizontalScrollOffset();
    final int range = v.computeHorizontalScrollRange() - v.computeHorizontalScrollExtent();
    if (range == 0) return false;
    if (direction < 0) {
      return offset > 0;
    } else {
      return offset < range - 1;
    }
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    // Let the focused view and/or our descendants get the key first
    return super.dispatchKeyEvent(event) || executeKeyEvent(event);
  }

  /**
   * You can call this function yourself to have the scroll view perform scrolling from a key event,
   * just as if the event had been dispatched to it by the view hierarchy.
   *
   * @param event The key event to execute.
   * @return Return true if the event was handled, else false.
   */
  public boolean executeKeyEvent(KeyEvent event) {
    boolean handled = false;
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
          handled = arrowScroll(FOCUS_LEFT);
          break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          handled = arrowScroll(FOCUS_RIGHT);
          break;
        case KeyEvent.KEYCODE_TAB:
          if (Build.VERSION.SDK_INT >= 11) {
            // The focus finder had a bug handling FOCUS_FORWARD and FOCUS_BACKWARD
            // before Android 3.0. Ignore the tab key on those devices.
            if (event.hasNoModifiers()) {
              handled = arrowScroll(FOCUS_FORWARD);
            } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
              handled = arrowScroll(FOCUS_BACKWARD);
            }
          }
          break;
      }
    }
    return handled;
  }

  public boolean arrowScroll(int direction) {
    View currentFocused = findFocus();
    if (currentFocused == this) currentFocused = null;

    boolean handled = false;

    View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);
    if (nextFocused != null && nextFocused != currentFocused) {
      if (direction == View.FOCUS_LEFT) {
        // If there is nothing to the left, or this is causing us to
        // jump to the right, then what we really want to do is page left.
        final int nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
        final int currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
        if (currentFocused != null && nextLeft >= currLeft) {
          handled = pageLeft();
        } else {
          handled = nextFocused.requestFocus();
        }
      } else if (direction == View.FOCUS_RIGHT) {
        // If there is nothing to the right, or this is causing us to
        // jump to the left, then what we really want to do is page right.
        final int nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
        final int currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
        if (currentFocused != null && nextLeft <= currLeft) {
          handled = pageRight();
        } else {
          handled = nextFocused.requestFocus();
        }
      }
    } else if (direction == FOCUS_LEFT || direction == FOCUS_BACKWARD) {
      // Trying to move left and nothing there; try to page.
      handled = pageLeft();
    } else if (direction == FOCUS_RIGHT || direction == FOCUS_FORWARD) {
      // Trying to move right and nothing there; try to page.
      handled = pageRight();
    }
    if (handled) {
      playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
    }
    return handled;
  }

  private Rect getChildRectInPagerCoordinates(Rect outRect, View child) {
    if (outRect == null) {
      outRect = new Rect();
    }
    if (child == null) {
      outRect.set(0, 0, 0, 0);
      return outRect;
    }
    outRect.left = child.getLeft();
    outRect.right = child.getRight();
    outRect.top = child.getTop();
    outRect.bottom = child.getBottom();

    ViewParent parent = child.getParent();
    while (parent instanceof ViewGroup && parent != this) {
      final ViewGroup group = (ViewGroup) parent;
      outRect.left += group.getLeft();
      outRect.right += group.getRight();
      outRect.top += group.getTop();
      outRect.bottom += group.getBottom();

      parent = group.getParent();
    }
    return outRect;
  }

  boolean pageLeft() {
    if (mCurItem > 0) {
      setCurrentItem(mCurItem - 1, true);
      return true;
    }
    return false;
  }

  boolean pageRight() {
    if (mAdapter != null && mCurItem < (mAdapter.getCount() - 1)) {
      setCurrentItem(mCurItem + 1, true);
      return true;
    }
    return false;
  }

  /** We only want the current page that is being shown to be focusable. */
  @Override
  public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
    final int focusableCount = views.size();

    final int descendantFocusability = getDescendantFocusability();

    if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        if (child.getVisibility() == VISIBLE) {
          ItemInfo ii = infoForChild(child);
          if (ii != null && ii.position == mCurItem) {
            child.addFocusables(views, direction, focusableMode);
          }
        }
      }
    }

    // we add ourselves (if focusable) in all cases except for when we are
    // FOCUS_AFTER_DESCENDANTS and there are some descendants focusable.  this is
    // to avoid the focus search finding layouts when a more precise search
    // among the focusable children would be more interesting.
    if (descendantFocusability != FOCUS_AFTER_DESCENDANTS
        ||
        // No focusable descendants
        (focusableCount == views.size())) {
      // Note that we can't call the superclass here, because it will
      // add all views in.  So we need to do the same thing View does.
      if (!isFocusable()) {
        return;
      }
      if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE
          && isInTouchMode()
          && !isFocusableInTouchMode()) {
        return;
      }
      if (views != null) {
        views.add(this);
      }
    }
  }

  /** We only want the current page that is being shown to be touchable. */
  @Override
  public void addTouchables(ArrayList<View> views) {
    // Note that we don't call super.addTouchables(), which means that
    // we don't call View.addTouchables().  This is okay because a ViewPager
    // is itself not touchable.
    for (int i = 0; i < getChildCount(); i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() == VISIBLE) {
        ItemInfo ii = infoForChild(child);
        if (ii != null && ii.position == mCurItem) {
          child.addTouchables(views);
        }
      }
    }
  }

  /** We only want the current page that is being shown to be focusable. */
  @Override
  protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
    int index;
    int increment;
    int end;
    int count = getChildCount();
    if ((direction & FOCUS_FORWARD) != 0) {
      index = 0;
      increment = 1;
      end = count;
    } else {
      index = count - 1;
      increment = -1;
      end = -1;
    }
    for (int i = index; i != end; i += increment) {
      View child = getChildAt(i);
      if (child.getVisibility() == VISIBLE) {
        ItemInfo ii = infoForChild(child);
        if (ii != null && ii.position == mCurItem) {
          if (child.requestFocus(direction, previouslyFocusedRect)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
    // ViewPagers should only report accessibility info for the current page,
    // otherwise things get very confusing.

    // TODO: Should this note something about the paging container?

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() == VISIBLE) {
        final ItemInfo ii = infoForChild(child);
        if (ii != null
            && ii.position == mCurItem
            && child.dispatchPopulateAccessibilityEvent(event)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams();
  }

  @Override
  protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return generateDefaultLayoutParams();
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams && super.checkLayoutParams(p);
  }

  @Override
  public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  class MyAccessibilityDelegate extends AccessibilityDelegate {

    @Override
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
      super.onInitializeAccessibilityEvent(host, event);
      event.setClassName(ViewPager.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
      super.onInitializeAccessibilityNodeInfo(host, info);
      info.setClassName(ViewPager.class.getName());
      info.setScrollable(mAdapter != null && mAdapter.getCount() > 1);
      if (mAdapter != null && mCurItem >= 0 && mCurItem < mAdapter.getCount() - 1) {
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
      }
      if (mAdapter != null && mCurItem > 0 && mCurItem < mAdapter.getCount()) {
        info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
      }
    }

    @Override
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
      if (super.performAccessibilityAction(host, action, args)) {
        return true;
      }
      switch (action) {
        case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
          {
            if (mAdapter != null && mCurItem >= 0 && mCurItem < mAdapter.getCount() - 1) {
              setCurrentItem(mCurItem + 1);
              return true;
            }
          }
          return false;
        case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
          {
            if (mAdapter != null && mCurItem > 0 && mCurItem < mAdapter.getCount()) {
              setCurrentItem(mCurItem - 1);
              return true;
            }
          }
          return false;
      }
      return false;
    }
  }

  private class PagerObserver extends DataSetObserver {
    @Override
    public void onChanged() {
      dataSetChanged();
    }

    @Override
    public void onInvalidated() {
      dataSetChanged();
    }
  }

  /** Layout parameters that should be supplied for views added to a ViewPager. */
  public static class LayoutParams extends ViewGroup.LayoutParams {
    /**
     * true if this view is a decoration on the pager itself and not a view supplied by the adapter.
     */
    public boolean isDecor;

    /**
     * Gravity setting for use on decor views only: Where to position the view page within the
     * overall ViewPager container; constants are defined in {@link android.view.Gravity}.
     */
    public int gravity;

    /** Width as a 0-1 multiplier of the measured pager width */
    public float widthFactor = 0.f;

    /**
     * true if this view was added during layout and needs to be measured before being positioned.
     */
    public boolean needsMeasure;

    public LayoutParams() {
      super(FILL_PARENT, FILL_PARENT);
    }

    public LayoutParams(Context context, AttributeSet attrs) {
      super(context, attrs);

      final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
      gravity = a.getInteger(0, Gravity.TOP);
      a.recycle();
    }
  }
}
