package armmel.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
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

  private int desiredWidth, desiredHeight;
  private final int bigBorderSize;

  // constructors
  public OutlineTextView(Context context) {
    this(context, null, 0);
  }

  public OutlineTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public OutlineTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    float textSize = dpToPx(context, 12);
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
    updateDesiredSize();
  }

  public void setText(String t) {
    text = t;
    updateDesiredSize();
    invalidate();
    requestLayout();
  }

  public void setText(CharSequence t) {
    text = t == null ? "" : t.toString();
    updateDesiredSize();
    invalidate();
    requestLayout();
  }

  private void updateDesiredSize() {
    float widestLine = 0;
    if (text != null) {
      String[] paragraphs = text.split("\n", -1);
      for (String paragraph : paragraphs) {
        widestLine = Math.max(widestLine, TEXT_PAINT.measureText(paragraph));
      }
    }

    Paint.FontMetrics metrics = TEXT_PAINT.getFontMetrics();
    int outlineInset = getOutlineInset();
    desiredWidth =
        (int) Math.ceil(widestLine) + getPaddingLeft() + getPaddingRight() + outlineInset * 2;
    desiredHeight =
        (int) Math.ceil(metrics.descent - metrics.ascent)
            + getPaddingTop()
            + getPaddingBottom()
            + outlineInset * 2;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (text == null || text.isEmpty()) return;

    int outlineInset = getOutlineInset();
    float maxWidth = getWidth() - getPaddingLeft() - getPaddingRight() - outlineInset * 2;
    if (maxWidth <= 0) return;

    float x = (getPaddingLeft() + getWidth() - getPaddingRight()) / 2f;

    float originalSize = TEXT_PAINT.getTextSize();
    float minSize = spToPx(getContext(), 6);
    float shrinkStep = dpToPx(getContext(), 0.5f);

    String[] lines;

    if (isSingleWord(text)) {
      while (TEXT_PAINT.measureText(text) > maxWidth && TEXT_PAINT.getTextSize() > minSize) {
        TEXT_PAINT.setTextSize(Math.max(minSize, TEXT_PAINT.getTextSize() - shrinkStep));
        WHITE_BORDER_PAINT.setTextSize(TEXT_PAINT.getTextSize());
      }
      lines =
          TEXT_PAINT.measureText(text) <= maxWidth
              ? new String[] {text}
              : new String[] {
                TextUtils.ellipsize(text, new TextPaint(TEXT_PAINT), maxWidth, TextUtils.TruncateAt.END)
                    .toString()
              };
    } else {
      lines = wrapLauncherText(TEXT_PAINT, text, maxWidth);
      while (lines.length > 2 && TEXT_PAINT.getTextSize() > minSize) {
        TEXT_PAINT.setTextSize(Math.max(minSize, TEXT_PAINT.getTextSize() - shrinkStep));
        WHITE_BORDER_PAINT.setTextSize(TEXT_PAINT.getTextSize());
        lines = wrapLauncherText(TEXT_PAINT, text, maxWidth);
      }
    }

    if (lines.length > 2) {
      TEXT_PAINT.setTextSize(originalSize);
      WHITE_BORDER_PAINT.setTextSize(originalSize);
      lines =
          new String[] {
            TextUtils.ellipsize(text, new TextPaint(TEXT_PAINT), maxWidth, TextUtils.TruncateAt.END)
                .toString()
          };
    }

    Paint.FontMetrics fm = TEXT_PAINT.getFontMetrics();
    float lineHeight = fm.descent - fm.ascent;
    float textHeight = lineHeight * lines.length;
    float availableHeight =
        getHeight() - getPaddingTop() - getPaddingBottom() - outlineInset * 2;
    float y =
        getPaddingTop()
            + outlineInset
            + Math.max(0, (availableHeight - textHeight) / 2f)
            - fm.ascent;

    for (String line : lines) {
      canvas.drawText(line, x, y, WHITE_BORDER_PAINT);
      canvas.drawText(line, x, y, TEXT_PAINT);
      y += lineHeight;
    }

    TEXT_PAINT.setTextSize(originalSize);
    WHITE_BORDER_PAINT.setTextSize(originalSize);
  }

  public static int dpToPx(Context context, float dp) {
    final float scale = context.getResources().getDisplayMetrics().density;
    return (int) (dp * scale + 0.5f);
  }

  public static float spToPx(Context context, float sp) {
    return sp * context.getResources().getDisplayMetrics().scaledDensity;
  }

  public static String[] wrapLauncherText(Paint paint, String text, float maxWidth) {
    if (text == null || text.isEmpty()) return new String[0];

    List<String> lines = new ArrayList<>();
    int paragraphStart = 0;
    while (paragraphStart <= text.length()) {
      int paragraphEnd = text.indexOf('\n', paragraphStart);
      if (paragraphEnd == -1) paragraphEnd = text.length();

      int start = paragraphStart;
      if (start == paragraphEnd) {
        lines.add("");
      }
      while (start < paragraphEnd) {
        int count = paint.breakText(text, start, paragraphEnd, true, maxWidth, null);
        int end = count > 0 ? start + count : start + 1;
        int breakAt = end;

        if (end < paragraphEnd) {
          for (int i = end; i > start; i--) {
            char previous = text.charAt(i - 1);
            char next = text.charAt(i);
            if (Character.isWhitespace(previous)) {
              breakAt = i - 1;
              break;
            }
            if ((Character.isLowerCase(previous) && Character.isUpperCase(next))
                || (Character.isLetter(previous) && Character.isDigit(next))
                || previous == '_'
                || previous == '-') {
              breakAt = i;
              break;
            }
          }
        }

        if (breakAt == start) breakAt = end;
        lines.add(text.substring(start, breakAt));
        start = breakAt;
        while (start < paragraphEnd && Character.isWhitespace(text.charAt(start))) {
          start++;
        }
      }

      if (paragraphEnd == text.length()) break;
      paragraphStart = paragraphEnd + 1;
    }

    return lines.toArray(new String[0]);
  }

  private int getOutlineInset() {
    return (int) Math.ceil(WHITE_BORDER_PAINT.getStrokeWidth() / 2f);
  }

  private static boolean isSingleWord(String value) {
    for (int i = 0; i < value.length(); i++) {
      char current = value.charAt(i);
      if (Character.isWhitespace(current)) return false;

      if (i > 0) {
        char previous = value.charAt(i - 1);
        if ((Character.isLowerCase(previous) && Character.isUpperCase(current))
            || (Character.isLetter(previous) && Character.isDigit(current))
            || previous == '_'
            || previous == '-') {
          return false;
        }
      }
    }
    return true;
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

    float availableTextWidth =
        width - getPaddingLeft() - getPaddingRight() - getOutlineInset() * 2;
    Paint.FontMetrics metrics = TEXT_PAINT.getFontMetrics();
    int lineCount =
        availableTextWidth > 0 && text != null && !text.isEmpty()
            ? isSingleWord(text)
                ? 1
                : Math.min(
                    2, Math.max(1, wrapLauncherText(TEXT_PAINT, text, availableTextWidth).length))
            : 1;
    int multilineHeight =
        (int) Math.ceil(metrics.descent - metrics.ascent) * lineCount
            + getPaddingTop()
            + getPaddingBottom()
            + getOutlineInset() * 2;

    if (heightMode == MeasureSpec.EXACTLY) {
      height = heightSize;
    } else if (heightMode == MeasureSpec.AT_MOST) {
      height = Math.min(Math.max(desiredHeight, multilineHeight), heightSize);
    } else {
      height = Math.max(desiredHeight, multilineHeight);
    }

    setMeasuredDimension(width, height);
  }
}
