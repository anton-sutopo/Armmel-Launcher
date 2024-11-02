/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package armmel.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.view.View;
import java.util.Calendar;
import java.util.Locale;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import java.util.Calendar;
import android.graphics.Typeface;
/**
 *
 * @author anton
 */
public class CustomImageView extends View {

    private boolean clock = false;
    private boolean calendar = false;
    public int weekday;
    private Calendar cal;
    Paint paint = new Paint();
    String clockText = "10:20";
    Rect bounds = new Rect();
    private Bitmap d;
    private Context context;
    public CustomImageView(Context context) {
        super(context);
        this.context= context;
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context= context;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
    }

    public CustomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context= context;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); //To change body of generated methods, choose Tools | Templates.
        if(d != null) {
            int width = 0;
            if(d.getWidth() >= d.getHeight()) {
                width = canvas.getWidth();
            } else {
                width = canvas.getHeight();
            }

            Rect dest = new Rect((canvas.getWidth() - width) / 2, (canvas.getHeight() - width) / 2, width, width);
            canvas.drawBitmap(d, null, dest, paint);
        }
        if (clock) {
            this.cal = Calendar.getInstance();
            float sec = cal.get(Calendar.SECOND);
            float min = cal.get(Calendar.MINUTE);
            float hour = cal.get(Calendar.HOUR_OF_DAY);
            int x = canvas.getWidth() / 2;
            int y = canvas.getHeight() / 2;
            int radius = Math.min(canvas.getHeight(),canvas.getWidth())/2;
            //draw hands
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(5f);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawLine(x, y, (float) (x + (radius * 0.5f) * Math.cos(Math.toRadians((hour / 12.0f * 360.0f) - 90f))),
                    (float) (y + (radius * 0.5f) * Math.sin(Math.toRadians((hour / 12.0f * 360.0f) - 90f))), paint);
            canvas.save();
            paint.setColor(Color.BLACK);
            canvas.drawLine(x, y, (float) (x + (radius * 0.6f) * Math.cos(Math.toRadians((min / 60.0f * 360.0f) - 90f))),
                    (float) (y + (radius * 0.6f) * Math.sin(Math.toRadians((min / 60.0f * 360.0f) - 90f))), paint);
            canvas.save();

            paint.setColor(Color.RED) ;
            paint.setStrokeWidth(2f);
            canvas.drawLine(x, y, (float) (x + (radius * 0.7f) * Math.cos(Math.toRadians((sec / 60.0f * 360.0f) - 90f))),
                    (float) (y + (radius * 0.7f) * Math.sin(Math.toRadians((sec / 60.0f * 360.0f) - 90f))), paint);
            canvas.drawCircle(x,y,5f,paint);
            postInvalidateDelayed(500);
        } else if (calendar) {
            String[] s = clockText.split(",");
            if (weekday == 1) {
                paint.setColor(Color.RED);
            } else {
                paint.setColor(Color.BLACK);
            }
            paint.setTextSize(IconPack.dpToPx(10,context.getResources()));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2)) - IconPack.dpToPx(15,context.getResources());
            canvas.drawText(s[0], (canvas.getWidth()) / 2, yPos, paint);
            paint.setTextSize(IconPack.dpToPx(24,context.getResources()));
            paint.setColor(Color.BLACK);
            Typeface f = paint.getTypeface();
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2)) + 10;
            canvas.drawText(s[1], (canvas.getWidth()) / 2, yPos, paint);
            paint.setTypeface(f);
        }
    }

    public void setClock(boolean clock) {
        this.clock = clock;
        Calendar c = Calendar.getInstance();
        //String date = c.get(Calendar.YEAR) + "" + c.get(Calendar.MONTH) + "" + c.get(Calendar.DATE);
        String time = String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", c.get(Calendar.MINUTE));
        this.clockText = time;
    }

    public void setCalendar(boolean calendar) {
        this.calendar = calendar;
        Calendar c = Calendar.getInstance();
        String date = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) + "," + c.get(Calendar.DATE);
        weekday = c.get(Calendar.DAY_OF_WEEK);
        //String time = String.format("%02d", c.get(Calendar.HOUR)) + ":" + String.format("%02d", c.get(Calendar.MINUTE));
        this.clockText = date;
    }
    public void setImage(Bitmap d) {
        this.d = d; 
    }
    public void setImageResource(int id) {
        Drawable d = context.getResources().getDrawable(id,context.getTheme()); 
        setImage(IconPack.convertToBitmap(d,d.getIntrinsicWidth(),d.getIntrinsicHeight())); 
    }
}
