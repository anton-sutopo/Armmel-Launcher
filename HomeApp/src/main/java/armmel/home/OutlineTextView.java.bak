package armmel.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class OutlineTextView extends View {
  private final Paint TEXT_PAINT;
  private final Paint WHITE_BORDER_PAINT;

  private String text = "";

  private int desiredWidth, desiredHeight, charwidth;
  private final int bigBorderSize, halfMargin;

  // constructors
  public OutlineTextView(Context context) {
    this(context, null, 0);
  }

  public OutlineTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public OutlineTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    float textSize = 0;
    int textColor = context.getColor(R.color.main_color);
    int textBackColor = context.getColor(R.color.white);
    text = "";
    if (attrs != null) {
      @SuppressLint("CustomViewStyleable")
      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StickerTextAttrs);
      textSize =
          a.getDimensionPixelSize(R.styleable.StickerTextAttrs_textSize, dpToPx(context, 12));
      textColor = a.getColor(R.styleable.StickerTextAttrs_textColor, textColor);
      textBackColor = a.getColor(R.styleable.StickerTextAttrs_textBackColor, textBackColor);
      text = a.getString(R.styleable.StickerTextAttrs_text);
      a.recycle();
    }

    TEXT_PAINT = new Paint();
    TEXT_PAINT.setTextSize(textSize);
    TEXT_PAINT.setStyle(Paint.Style.FILL);
    TEXT_PAINT.setColor(textColor);
    TEXT_PAINT.setTextAlign(Paint.Align.CENTER);

    int smallBorderSize = dpToPx(context, 2);
    bigBorderSize = smallBorderSize * 3;
    halfMargin = bigBorderSize / 2;

    WHITE_BORDER_PAINT = new Paint();
    WHITE_BORDER_PAINT.setTextSize(textSize);
    WHITE_BORDER_PAINT.setStyle(Paint.Style.STROKE);
    WHITE_BORDER_PAINT.setStrokeWidth(smallBorderSize);
    WHITE_BORDER_PAINT.setColor(textBackColor);
    WHITE_BORDER_PAINT.setTextAlign(Paint.Align.CENTER);
    /*BROWN_BORDER_PAINT = new Paint();
    BROWN_BORDER_PAINT.setTextSize(textSize);
    BROWN_BORDER_PAINT.setStyle(Paint.Style.STROKE);
    BROWN_BORDER_PAINT.setStrokeWidth(smallBorderSize);
    BROWN_BORDER_PAINT.setColor(context.getColor(R.color.main_color_dark));*/
    measure();
  }

  public void setText(String t) {
    text = t;
    measure();
    invalidate();
    requestLayout();
  }

  public void setText(CharSequence t) {
    text = t.toString();
    measure();
    invalidate();
    requestLayout();
  }

  private void measure() {
    Rect bounds = new Rect();
    if (text != null) {
      TEXT_PAINT.getTextBounds(text, 0, text.length(), bounds);
      desiredHeight = bounds.height() + bigBorderSize;
      desiredWidth = bounds.width() + bigBorderSize;
    }
    if (charwidth <= 0) {
      TEXT_PAINT.getTextBounds("b", 0, 1, bounds);
      charwidth = bounds.width();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (text == null || text.isEmpty()) return;

    float maxWidth =
        getWidth() - getPaddingLeft() - getPaddingRight() - WHITE_BORDER_PAINT.getStrokeWidth() * 2;

    int x = getWidth() / 2;

    float originalSize = TEXT_PAINT.getTextSize();
    float minSize = dpToPx(getContext(), 10);
    float shrinkStep = dpToPx(getContext(), 0.5f);

    String[] lines;

    // 1️⃣ Try normal size
    lines = wrapLauncherText(TEXT_PAINT, text, maxWidth);

    // 2️⃣ Shrink until max 2 lines
    while (lines.length > 2 && TEXT_PAINT.getTextSize() > minSize) {
      TEXT_PAINT.setTextSize(TEXT_PAINT.getTextSize() - shrinkStep);
      WHITE_BORDER_PAINT.setTextSize(TEXT_PAINT.getTextSize());
      lines = wrapLauncherText(TEXT_PAINT, text, maxWidth);
    }

    // 3️⃣ Ellipsis fallback
    if (lines.length > 2) {
      TEXT_PAINT.setTextSize(originalSize);
      WHITE_BORDER_PAINT.setTextSize(originalSize);

      TextPaint tp = new TextPaint(TEXT_PAINT);
      String ellipsized =
          TextUtils.ellipsize(text, tp, maxWidth * 2, TextUtils.TruncateAt.END).toString();

      lines = new String[] {ellipsized};
    }

    // --- draw ---
    Paint.FontMetrics fm = TEXT_PAINT.getFontMetrics();
    int lineHeight = (int) (fm.descent - fm.ascent);
    int y = (int) (getPaddingTop() - fm.ascent);

    for (String line : lines) {
      canvas.drawText(line, x, y, WHITE_BORDER_PAINT);
      canvas.drawText(line, x, y, TEXT_PAINT);
      y += lineHeight;
    }
  }

  public static int dpToPx(Context context, float dp) {
    final float scale = context.getResources().getDisplayMetrics().density;
    return (int) (dp * scale + 0.5f);
  }

  public static String[] wrapLauncherText(Paint paint, String text, float maxWidth) {
    if (text == null || text.isEmpty()) return new String[0];

    List<String> lines = new ArrayList<>();
    StringBuilder line = new StringBuilder();

    int lastBreak = -1;

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);

      // Hard newline
      if (c == '\n') {
        lines.add(line.toString());
        line.setLength(0);
        lastBreak = -1;
        continue;
      }

      // Detect break BEFORE append
      if (i > 0) {
        char prev = text.charAt(i - 1);

        if (c == ' ') {
          lastBreak = line.length();
        } else if (Character.isLowerCase(prev) && Character.isUpperCase(c)) {
          lastBreak = line.length();
        } else if (Character.isLetter(prev) && Character.isDigit(c)) {
          lastBreak = line.length();
        } else if (prev == '_' || prev == '-') {
          lastBreak = line.length();
        }
      }

      line.append(c);

      if (paint.measureText(line.toString()) > maxWidth) {
        if (lastBreak >= 0) {
          lines.add(line.substring(0, lastBreak));
          line.delete(0, lastBreak);

          // Trim leading space
          if (line.length() > 0 && line.charAt(0) == ' ') {
            line.deleteCharAt(0);
          }
        } else {
          // 🚫 no semantic break → stop wrapping
          break;
        }

        lastBreak = -1;
      }
    }

    if (line.length() > 0) {
      lines.add(line.toString());
    }

    return lines.toArray(new String[0]);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    int width;
    int height;

    // Measure Width
    if (widthMode == MeasureSpec.EXACTLY) {
      // Must be this size
      width = widthSize;
    } else if (widthMode == MeasureSpec.AT_MOST) {
      // Can't be bigger than...
      width = Math.min(desiredWidth, widthSize);
    } else {
      // Be whatever you want
      width = desiredWidth;
    }

    // Measure Height
    if (heightMode == MeasureSpec.EXACTLY) {
      // Must be this size
      height = heightSize;
    } else if (heightMode == MeasureSpec.AT_MOST) {
      // Can't be bigger than...
      height = Math.min(desiredHeight, heightSize);
    } else {
      // Be whatever you want
      height = desiredHeight;
    }

    setMeasuredDimension(width, height);
  }
}
