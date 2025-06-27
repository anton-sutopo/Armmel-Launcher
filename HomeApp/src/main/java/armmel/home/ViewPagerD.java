/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package armmel.home;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * @author anton
 */
public class ViewPagerD extends armmel.library.ViewPager {

  private float downX;
  private float downY;
  private float deltaX;
  private float deltaY;

  public ViewPagerD(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ViewPagerD(Context c) {
    super(c);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    float x = ev.getX();
    float y = ev.getY();
    switch (ev.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        {
          downX = x;
          downY = y;
          return super.onInterceptTouchEvent(ev);
        }
      case MotionEvent.ACTION_MOVE:
        {
          deltaX = Math.abs(downX - x);
          deltaY = Math.abs(downY - y);
          if (deltaX > 10) {
            super.onTouchEvent(ev);
          } else {
            super.onInterceptTouchEvent(ev);
          }
        }
      case MotionEvent.ACTION_UP:
        {
          if (deltaX > 10) {
            super.onTouchEvent(ev);
          } else {
            super.onInterceptTouchEvent(ev);
          }
        }
    }
    return super.onInterceptTouchEvent(
        ev); // To change body of generated methods, choose Tools | Templates.
  }
}
