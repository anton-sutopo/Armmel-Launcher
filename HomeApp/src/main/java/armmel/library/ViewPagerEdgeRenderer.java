package armmel.library;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.EdgeEffect;

import java.util.ArrayList;

/**
 * Helper responsible for drawing edge effects and page margin drawables for ViewPager.
 */
public class ViewPagerEdgeRenderer {
  private final ViewPager mViewPager;

  public ViewPagerEdgeRenderer(ViewPager viewPager) {
    mViewPager = viewPager;
  }

  public void drawEdges(Canvas canvas) {
    boolean needsInvalidate = false;
    final int overScrollMode = mViewPager.getOverScrollMode();
    final PagerAdapter adapter = mViewPager.getAdapter();
    if (overScrollMode == android.view.View.OVER_SCROLL_ALWAYS
        || (overScrollMode == android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS
            && adapter != null
            && adapter.getCount() > 1)) {
      EdgeEffect left = mViewPager.getLeftEdge();
      EdgeEffect right = mViewPager.getRightEdge();
      if (left != null && !left.isFinished()) {
        final int restoreCount = canvas.save();
        final int height = mViewPager.getHeight() - mViewPager.getPaddingTop() - mViewPager.getPaddingBottom();
        final int width = mViewPager.getWidth();

        canvas.rotate(270);
        canvas.translate(-height + mViewPager.getPaddingTop(), mViewPager.getFirstOffset() * width);
        left.setSize(height, width);
        needsInvalidate |= left.draw(canvas);
        canvas.restoreToCount(restoreCount);
      }
      if (right != null && !right.isFinished()) {
        final int restoreCount = canvas.save();
        final int width = mViewPager.getWidth();
        final int height = mViewPager.getHeight() - mViewPager.getPaddingTop() - mViewPager.getPaddingBottom();

        canvas.rotate(90);
        canvas.translate(-mViewPager.getPaddingTop(), -(mViewPager.getLastOffset() + 1) * width);
        right.setSize(height, width);
        needsInvalidate |= right.draw(canvas);
        canvas.restoreToCount(restoreCount);
      }
    } else {
      // ensure finished
      EdgeEffect left = mViewPager.getLeftEdge();
      EdgeEffect right = mViewPager.getRightEdge();
      if (left != null) left.finish();
      if (right != null) right.finish();
    }

    if (needsInvalidate) {
      mViewPager.postInvalidateOnAnimation();
    }
  }

  public void drawPageMargins(Canvas canvas) {
    final int pageMargin = mViewPager.getPageMargin();
    final Drawable marginDrawable = mViewPager.getMarginDrawable();
    final ArrayList<ViewPager.ItemInfo> items = mViewPager.getItems();
    final PagerAdapter adapter = mViewPager.getAdapter();

    if (pageMargin > 0 && marginDrawable != null && items.size() > 0 && adapter != null) {
      final int scrollX = mViewPager.getScrollX();
      final int width = mViewPager.getWidth();

      final float marginOffset = (float) pageMargin / width;
      int itemIndex = 0;
      ViewPager.ItemInfo ii = items.get(0);
      float offset = ii.offset;
      final int itemCount = items.size();
      final int firstPos = ii.position;
      final int lastPos = items.get(itemCount - 1).position;
      for (int pos = firstPos; pos < lastPos; pos++) {
        while (pos > ii.position && itemIndex < itemCount) {
          ii = items.get(++itemIndex);
        }

        float drawAt;
        if (pos == ii.position) {
          drawAt = (ii.offset + ii.widthFactor) * width;
          offset = ii.offset + ii.widthFactor + marginOffset;
        } else {
          float widthFactor = adapter.getPageWidth(pos);
          drawAt = (offset + widthFactor) * width;
          offset += widthFactor + marginOffset;
        }

        if (drawAt + pageMargin > scrollX) {
          marginDrawable.setBounds((int) drawAt, mViewPager.getTopPageBounds(), (int) (drawAt + pageMargin + 0.5f), mViewPager.getBottomPageBounds());
          marginDrawable.draw(canvas);
        }

        if (drawAt > scrollX + width) {
          break;
        }
      }
    }
  }
}
