package armmel.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

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
    int x = getWidth() / 2;
    int y = (int) (TEXT_PAINT.descent() - TEXT_PAINT.ascent());
    int maxWidth = (getWidth() / charwidth);
    for (String line : wrap(text, "\n", maxWidth)) {
      canvas.drawText(line, x, y, WHITE_BORDER_PAINT);
      canvas.drawText(line, x, y, TEXT_PAINT);
      y += TEXT_PAINT.descent() - TEXT_PAINT.ascent();
    }
  }

  public static final String[] wrap(String toWrap, String wrapKey, int maxLineLength) {
    // Handle null inputs
    if (toWrap == null || wrapKey == null || maxLineLength <= 0) {
      throw new IllegalArgumentException("Invalid input");
    }

    StringBuilder result = new StringBuilder();
    String[] words = toWrap.split(" ");

    int currentLineLength = 0;
    for (String word : words) {

      // Manual wrapping
      if (word.contains(wrapKey)) {
        word = word.replace(wrapKey, "\n"); // Replace all occurrences
        result.append(word).append(" ");
        currentLineLength = 0;
        continue; // Avoid auto-wrapping
      }

      // Auto wrapping
      if (currentLineLength + word.length() + 1 <= maxLineLength) {
        if (currentLineLength > 0) { // Add space if not the first word on the line
          result.append(" ");
        }
        result.append(word);
        currentLineLength += word.length() + 1;
      } else {
        result.append("\n").append(word);
        currentLineLength = word.length() + 1;
      }
    }

    // Convert the result to an array by splitting on newline characters
    return result.toString().trim().split("\n");
  }

  public static int dpToPx(Context context, float dp) {
    final float scale = context.getResources().getDisplayMetrics().density;
    return (int) (dp * scale + 0.5f);
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
