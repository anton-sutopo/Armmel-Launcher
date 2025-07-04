/*
 * Copyright (C) 2011 Thomas Lundqvist
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package armmel.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import armmel.keyboard.Keyboard.Key;
import java.util.List;

/**
 * A view that renders a virtual {@link Keyboard}. It handles rendering of keys and detecting key
 * presses and touch movements.
 *
 * <p>Based on the official Android KeyboardView class.
 */
public class ModKeyboardView extends View implements View.OnClickListener {

  /** Listener for virtual keyboard events. */
  public interface OnKeyboardActionListener {

    /**
     * Called when the user presses a key. This is sent before the {@link #onKey} is called. For
     * keys that repeat, this is only called once.
     *
     * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid key,
     *     the value will be zero.
     */
    void onPress(int primaryCode);

    /**
     * Called when the user releases a key. This is sent after the {@link #onKey} is called. For
     * keys that repeat, this is only called once.
     *
     * @param primaryCode the code of the key that was released
     */
    void onRelease(int primaryCode);

    /**
     * Send a key press to the listener.
     *
     * @param primaryCode this is the key that was pressed
     */
    void onKey(int primaryCode);

    /**
     * Sends a sequence of characters to the listener.
     *
     * @param text the sequence of characters to be displayed.
     */
    void onText(CharSequence text);
  }

  /** Listener for {@link OnKeyboardActionListener}. */
  private OnKeyboardActionListener mKeyboardActionListener;

  private static final int NOT_A_KEY = -1;
  private static final int VERTICAL_CORRECTION = -10;

  private Keyboard mKeyboard;
  private Key[] mKeys;
  private boolean mCapsLock;

  /** Whether the keyboard bitmap needs to be redrawn before it's blitted. * */
  private boolean mDrawPending;

  /** The dirty region in the keyboard bitmap */
  private Rect mDirtyRect = new Rect();

  /** The keyboard bitmap for faster updates */
  private Bitmap mBuffer;

  /** Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer. */
  private boolean mKeyboardChanged;

  /** The canvas for the above mutable keyboard bitmap */
  private Canvas mCanvas;

  private Rect mPadding = new Rect(0, 0, 0, 0);

  private static final int MSG_REPEAT = 3;
  private static final int REPEAT_INTERVAL = 60; // ~16 keys per second
  private static final int REPEAT_START_DELAY = 400;
  private static final int REPEAT_START_DELAY_CHARS = 800; // for non-repeating keys
  private boolean mRepeating = false;
  private Context context;
  private Typeface tf;
  private Typeface tfbold;
  private boolean isDark;
  final int color_normal_dark = 0xffffffff;
  final int color_current_dark = 0xffff0000;
  final int color_normal_light = 0xff000000;
  final int color_current_light = 0xffff0000;
  Drawable keyBackground;
  private boolean oldDarkStatus;
  private Drawable bg;
  Handler mHandler =
      new Handler() {
        @Override
        public void handleMessage(Message msg) {
          switch (msg.what) {
            case MSG_REPEAT:
              sendCurrentCode();
              Message repeat = Message.obtain(this, MSG_REPEAT);
              sendMessageDelayed(repeat, REPEAT_INTERVAL);
              mRepeating = true;
              break;
          }
        }
      };

  public ModKeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
  }

  public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
    mKeyboardActionListener = listener;
  }

  /**
   * Returns the {@link OnKeyboardActionListener} object.
   *
   * @return the listener attached to this keyboard
   */
  protected OnKeyboardActionListener getOnKeyboardActionListener() {
    return mKeyboardActionListener;
  }

  /**
   * Attaches a keyboard to this view. The keyboard can be switched at any time and the view will
   * re-layout itself to accommodate the keyboard.
   *
   * @see Keyboard
   * @see #getKeyboard()
   * @param keyboard the keyboard to display in this view
   */
  public void setKeyboard(Keyboard keyboard) {
    if (mDownKeyIndex != NOT_A_KEY) {
      // Active keypress ongoing, clean up old keyboard
      Key key = mKeys[mDownKeyIndex];
      key.setPressed(false);
    }
    mKeyboard = keyboard;
    List<Key> keys = mKeyboard.getKeys();
    mKeys = keys.toArray(new Key[keys.size()]);
    requestLayout();
    if (mDownKeyIndex != NOT_A_KEY) {
      // Active keypress ongoing, find corresponding new key and mark it as pressed
      mDownKeyIndex = getKeyIndex(mDownKeyX, mDownKeyY);
      if (mDownKeyIndex != NOT_A_KEY) {
        Key key = mKeys[mDownKeyIndex];
        key.setPressed(true);
      }
    }
    // Hint to reallocate the buffer if the size changed
    mKeyboardChanged = true;
    invalidateAllKeys();
  }

  /**
   * Returns the current keyboard being displayed by this view.
   *
   * @return the currently attached keyboard
   * @see #setKeyboard(Keyboard)
   */
  public Keyboard getKeyboard() {
    return mKeyboard;
  }

  /**
   * Sets the state of the shift key of the keyboard, if any.
   *
   * @param shifted whether or not to enable the state of the shift key
   * @return true if the shift key state changed, false if there was no change
   * @see CopyOfModKeyboardView#isShifted()
   */
  public boolean setShifted(boolean caps, boolean shifted, boolean isJawa) {
    mCapsLock = caps;
    if (mKeyboard != null) {
      boolean tempStatus = false;
      if (mKeyboard.setShifted(shifted)) {
        tempStatus = true;
      }
      if (mKeyboard.setJawa(isJawa)) {
        // The whole keyboard probably needs to be redrawn
        //                invalidateAllKeys();
        tempStatus = true;
      }
      return tempStatus;
    }
    return false;
  }

  public void setNumMode(boolean nummode) {
    if (mKeyboard != null) {
      mKeyboard.setNumMode(nummode);
    }
  }

  /**
   * Returns the state of the shift key of the keyboard, if any.
   *
   * @return true if the shift is in a pressed state, false otherwise. If there is no shift key on
   *     the keyboard or there is no keyboard attached, it returns false.
   * @see CopyOfModKeyboardView#setShifted(boolean)
   */
  public boolean isShifted() {
    if (mKeyboard != null) {
      return mKeyboard.isShifted();
    }
    return false;
  }

  public void onClick(View v) {}

  /**
   * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient because
   * the keyboard renders the keys to an off-screen buffer and an invalidate() only draws the cached
   * buffer.
   *
   * @see #invalidateKey(int)
   */
  public void invalidateAllKeys() {
    mDirtyRect.union(0, 0, getWidth(), getHeight());
    mDrawPending = true;
    invalidate();
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Round up a little
    if (mKeyboard == null) {
      setMeasuredDimension(
          getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom());
    } else {
      int width = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
      if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
        width = MeasureSpec.getSize(widthMeasureSpec);
      }
      setMeasuredDimension(width, mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom());
    }
  }

  /**
   * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only one
   * key is changing it's content. Any changes that affect the position or size of the key may not
   * be honored.
   *
   * @param keyIndex the index of the key in the attached {@link Keyboard}.
   * @see #invalidateAllKeys
   */
  public void invalidateKey(int keyIndex) {
    if (mKeys == null) return;
    if (keyIndex < 0 || keyIndex >= mKeys.length) {
      return;
    }
    final Key key = mKeys[keyIndex];
    mDirtyRect.union(
        key.x + getPaddingLeft(),
        key.y + getPaddingTop(),
        key.x + key.width + getPaddingLeft(),
        key.y + key.height + getPaddingTop());
    if (mCanvas != null) drawOneKey(mCanvas, keyIndex);
    // invalidate(key.x + getPaddingLeft(), key.y + getPaddingTop(),
    //        key.x + key.width + getPaddingLeft(), key.y + key.height + getPaddingTop());
    invalidate();
  }

  @Override
  public void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (mKeyboard == null) return;
    if (tf == null) {
      tf = Typeface.createFromAsset(context.getAssets(), "fonts/Pringgandani.ttf");
      if (tf != null) {
        tfbold = Typeface.create(tf, Typeface.BOLD);
      }
    }
    if (mDrawPending || mBuffer == null || mKeyboardChanged) {
      drawAllKeys();
    }
    canvas.drawBitmap(mBuffer, 0, 0, null);
  }

  private void drawAllKeys() {
    if (mBuffer == null || mKeyboardChanged) {
      if (mBuffer == null
          || mKeyboardChanged
              && (mBuffer.getWidth() != getWidth() || mBuffer.getHeight() != getHeight())) {
        // Make sure our bitmap is at least 1x1
        final int width = Math.max(1, getWidth());
        final int height = Math.max(1, getHeight());
        mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBuffer);
      }
      mKeyboardChanged = false;
    }
    final Canvas canvas = mCanvas;
    mDirtyRect.union(0, 0, getWidth(), getHeight());
    canvas.save();
    canvas.clipRect(mDirtyRect);

    canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
    if (oldDarkStatus != isDark || (bg == null || keyBackground == null)) {
      bg =
          getResources()
              .getDrawable(
                  isDark
                      ? R.drawable.keyboard_background_dark
                      : R.drawable.keyboard_background_light,
                  context.getTheme());
      keyBackground =
          getResources()
              .getDrawable(
                  isDark ? R.drawable.key_background_dark : R.drawable.key_background_light,
                  context.getTheme());
      oldDarkStatus = isDark;
    }
    bg.setBounds(canvas.getClipBounds());
    bg.draw(canvas);

    for (int i = 0; i < mKeys.length; i++) {
      drawOneKey(canvas, i);
    }
    canvas.restore();
    mDrawPending = false;
    mDirtyRect.setEmpty();
  }

  public static int dpToPx(float dp, Resources res) {
    return (int)
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
  }

  private String[] getLabelArray(String labeles) {
    String[] labels = labeles.split(",");
    String[] retlabels = new String[labels.length];
    for (int i = 0; i < labels.length; i++) {
      char[] a = Character.toChars(Integer.parseInt(labels[i], 16));
      retlabels[i] = new String(a);
    }
    return retlabels;
  }

  private void drawOneKey(Canvas canvas, int index) {
    final Key key = mKeys[index];
    int currentcodeindex = -1;
    if (mDownKeyIndex == index) // If one key active, only care if it is this key
    currentcodeindex = mCurrentCodeIndex;

    int[] drawableState = key.getCurrentDrawableState();

    keyBackground.setState(drawableState);

    String labela = key.label == null ? null : key.label.toString();
    String[] labels = getLabelArray(labela);
    final Rect bounds = keyBackground.getBounds();
    if (key.width != bounds.right || key.height != bounds.bottom) {
      keyBackground.setBounds(0, 0, key.width, key.height);
    }
    final Rect padding = mPadding;
    final int kbdPaddingLeft = getPaddingLeft();
    final int kbdPaddingTop = getPaddingTop();
    final int color_normal = isDark ? color_normal_dark : color_normal_light;
    final int color_current = isDark ? color_current_dark : color_current_light;

    canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
    keyBackground.draw(canvas);

    int centerX = (key.width) / 2;
    int centerY = ((key.height) / 2);
    int marginX = key.width / 10;
    int marginY = key.height / 16;

    final Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setTextAlign(Paint.Align.LEFT);
    Rect r = new Rect();
    if (labels != null) {
      // Draw the text
      paint.setTypeface((tfbold == null ? Typeface.DEFAULT_BOLD : tfbold));
      paint.setTextSize(dpToPx(16, context.getResources()));
      paint.setColor(currentcodeindex == 0 ? color_current : color_normal);
      paint.getTextBounds(labels[0], 0, 1, r);
      canvas.drawText(
          labels[0], centerX - r.width() / 2 - r.left, centerY + r.height() / 2 - r.bottom, paint);
      if (key.codes.length == 5) {
        // Draw more side letters
        paint.setTypeface(tf == null ? Typeface.DEFAULT : tf);
        paint.setTextSize(dpToPx(13, context.getResources()));
        paint.setColor(currentcodeindex == 1 ? color_current : color_normal);
        paint.getTextBounds(labels[1], 0, 1, r);
        canvas.drawText(
            labels[1], padding.left + marginX, centerY + r.height() / 2 - r.bottom, paint);
        paint.setColor(currentcodeindex == 3 ? color_current : color_normal);
        paint.getTextBounds(labels[3], 0, 1, r);
        canvas.drawText(
            labels[3], key.width - marginX - r.right, centerY + r.height() / 2 - r.bottom, paint);
        paint.setColor(currentcodeindex == 2 ? color_current : color_normal);
        paint.getTextBounds(labels[2], 0, 1, r);
        canvas.drawText(
            labels[2], centerX - r.width() / 2 - r.left, padding.top + marginY - r.top, paint);
        paint.setColor(currentcodeindex == 4 ? color_current : color_normal);
        paint.getTextBounds(labels[4], 0, 1, r);
        canvas.drawText(
            labels[4], centerX - r.width() / 2 - r.left, key.height - marginY - r.bottom, paint);
      }
      if (key.codes[0] == Keyboard.KEYCODE_SHIFT && mCapsLock) {
        // Special handling of caps lock
        final String lock = "LOCK";
        paint.setTextSize(12);
        paint.setTextAlign(Align.CENTER);
        paint.setColor(color_normal);
        canvas.drawText(lock, centerX, padding.top + marginY + paint.getTextSize(), paint);
      }
    } else if (key.icon != null) {
      final int drawableX =
          (key.width - padding.left - padding.right - key.icon.getIntrinsicWidth()) / 2
              + padding.left;
      final int drawableY =
          (key.height - padding.top - padding.bottom - key.icon.getIntrinsicHeight()) / 2
              + padding.top;
      canvas.translate(drawableX, drawableY);
      key.icon.setBounds(0, 0, key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());
      key.icon.draw(canvas);
      canvas.translate(-drawableX, -drawableY);

      if (key.codes[0] == Keyboard.KEYCODE_SHIFT && mCapsLock) {
        // Special handling of caps lock
        final String lock = "LOCK";
        paint.setTextSize(12);
        paint.setTextAlign(Align.CENTER);
        paint.setColor(color_normal);
        canvas.drawText(lock, centerX, padding.top + marginY + paint.getTextSize(), paint);
      }
    }
    canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
  }

  int mDownKeyIndex = NOT_A_KEY;
  int mDownKeyX;
  int mDownKeyY;

  int mCurrentCodeIndex = -1; // current letter while touch down

  @Override
  public boolean onTouchEvent(MotionEvent me) {
    //        final int pointerCount = me.getPointerCount();
    final int action = me.getAction();
    boolean result = false;
    //        final long now = me.getEventTime();
    int touchX = (int) me.getX() - getPaddingLeft();
    int touchY = (int) me.getY() - getPaddingTop() + VERTICAL_CORRECTION;

    if (action == MotionEvent.ACTION_DOWN) {
      mDownKeyIndex = getKeyIndex(touchX, touchY);
      mDownKeyX = touchX;
      mDownKeyY = touchY;
      if (mDownKeyIndex != NOT_A_KEY) {
        Key key = mKeys[mDownKeyIndex];
        key.setPressed(true);
        mCurrentCodeIndex = 0;
        invalidateKey(mDownKeyIndex);
        initRepeat(key);
        if (key.repeatable) {
          sendCurrentCode(); // for explicitly repeatable marked keys, send key directly
        }
      }
      result = true;
    } else if (action == MotionEvent.ACTION_MOVE) {
      if (mDownKeyIndex != NOT_A_KEY) {
        Key key = mKeys[mDownKeyIndex];
        // Movement might also change current code (letter)
        int oldindex = mCurrentCodeIndex;
        updateCurrentCodeIndex(key, touchX, touchY);
        if (mCurrentCodeIndex != oldindex) {
          invalidateKey(mDownKeyIndex);
          // New letter/code resets repeat
          mHandler.removeMessages(MSG_REPEAT);
          initRepeat(key);
        }
      }
    } else if (action == MotionEvent.ACTION_UP) {
      if (mDownKeyIndex != NOT_A_KEY) {
        Key key = mKeys[mDownKeyIndex];
        if (!mRepeating) {
          updateCurrentCodeIndex(key, touchX, touchY);
          sendCurrentCode();
        }
        endMotion();
        result = true;
      }
    } else if (action == MotionEvent.ACTION_CANCEL) {
      endMotion();
      result = true;
    }

    return result;
  }

  private void updateCurrentCodeIndex(Key key, int touchX, int touchY) {
    mCurrentCodeIndex = -1;
    if (key.isInside(touchX, touchY)) {
      mCurrentCodeIndex = 0;
    } else if (key.codes.length == 5) {
      if (touchY < key.y) {
        mCurrentCodeIndex = 2;
      } else if (touchY >= key.y + key.height) {
        mCurrentCodeIndex = 4;
      } else if (touchX < key.x) {
        mCurrentCodeIndex = 1;
      } else if (touchX >= key.x + key.width) {
        mCurrentCodeIndex = 3;
      }
    }
  }

  private void initRepeat(Key key) {
    Message msg = mHandler.obtainMessage(MSG_REPEAT);
    mRepeating = false;
    if (key.repeatable) {
      mHandler.sendMessageDelayed(msg, REPEAT_START_DELAY);
      mRepeating = true;
    } else {
      mHandler.sendMessageDelayed(msg, REPEAT_START_DELAY_CHARS);
    }
  }

  private void endMotion() {
    if (mDownKeyIndex != NOT_A_KEY) {
      Key key = mKeys[mDownKeyIndex];
      mRepeating = false;
      mHandler.removeMessages(MSG_REPEAT);
      key.setPressed(false);
      mCurrentCodeIndex = -1;
      invalidateKey(mDownKeyIndex);
      mDownKeyIndex = NOT_A_KEY;
    }
  }

  private int getKeyIndex(int x, int y) {
    final Key[] keys = mKeys;
    int primaryIndex = NOT_A_KEY;
    int[] nearestKeyIndices = mKeyboard.getNearestKeys(x, y);
    final int keyCount = nearestKeyIndices.length;
    for (int i = 0; i < keyCount; i++) {
      final Key key = keys[nearestKeyIndices[i]];
      boolean isInside = key.isInside(x, y);
      if (isInside) {
        primaryIndex = nearestKeyIndices[i];
      }
    }
    return primaryIndex;
  }

  private void sendCurrentCode() {
    if (mDownKeyIndex != NOT_A_KEY) {
      Key key = mKeys[mDownKeyIndex];
      int code = Keyboard.KEYCODE_NOTUSED;
      if (mCurrentCodeIndex >= 0) code = key.codes[mCurrentCodeIndex];
      if (code != Keyboard.KEYCODE_NOTUSED) {
        mKeyboardActionListener.onKey(code);
        mKeyboardActionListener.onRelease(code);
      }
    }
  }

  public void setTheme(boolean isDark) {
    this.isDark = isDark;
  }
}
