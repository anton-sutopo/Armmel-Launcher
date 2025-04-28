package armmel.home;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import android.graphics.Bitmap;

public class ApplicationAdapter extends ArrayAdapter<ApplicationInfo> {

    private final MainActivity context;
    private final ArrayList<ApplicationInfo> mobileValues;
    private final Rect mOldBounds = new Rect();
    public  final int startPos;
    public ApplicationAdapter(MainActivity context, ArrayList<ApplicationInfo> apps, int startPos) {

        super(context, 0, apps);
        this.context = context;
        this.mobileValues = apps;
        this.startPos = startPos;

    }
    //	public ApplicationAdapter(Context context, String[] mobileValues) {
    //		this.context = context;
    //		this.mobileValues = mobileValues;
    //	}
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ApplicationInfo info = mobileValues.get(position);

        if (convertView == null) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.application, parent, false);
            convertView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                        ));
        }
        Bitmap icon = info.icon;
        if (info.isCalendar()) {
            CustomImageView iv = (CustomImageView) convertView.findViewById(R.id.icon_image);
            //iv.setImageDrawable(icon);
            iv.setImageResource(R.drawable.ios);
            context.calendar = iv;
            iv.setCalendar(true);
        } else if (info.isClock()) {
            CustomImageView iv = (CustomImageView) convertView.findViewById(R.id.icon_image);
            iv.setImageResource(R.drawable.clock);
            context.clock = iv;
            iv.setClock(true);
        }  else {
            final CustomImageView iv = (CustomImageView) convertView.findViewById(R.id.icon_image);
            iv.setImage(icon);
        }
        final OutlineTextView textView = (OutlineTextView) convertView.findViewById(R.id.icon_text);
        textView.setText(info.title);
        return convertView;
    }

    @Override    
    public int getCount() {
        return mobileValues.size();
    }
}
