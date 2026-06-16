package armmel.home;

import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;

// based on https://github.com/pedromassango/doubleClick
//
public class DoubleClick implements View.OnClickListener {
  private DoubleClickListener doubleClickListener;
  private AdapterView<?> parent;
  private int position;
  private Handler mHandler = new Handler();
  private boolean isBusy = false;
  private int clicks = 0;

  public DoubleClick(AdapterView<?> parent, int position) {
    this.parent = parent;
    this.position = position;
  }

  @Override
  public void onClick(View v) {
    if (!isBusy) {
      isBusy = true;
      if (clicks == 0) {
        final DoubleClickListener doc = doubleClickListener;
        mHandler.postDelayed(
            new Runnable() {
              @Override
              public void run() {
                if (clicks >= 2) {
                  doc.onDoubleClick(parent, v, position);
                }
                if (clicks == 1) {
                  doc.onSingleClick(parent, v, position);
                }
              }
            },
            200L);
      }
      clicks++;
      isBusy = false;
    }
  }

  public void setDoubleClickListener(DoubleClickListener d) {
    this.doubleClickListener = d;
  }

  public interface DoubleClickListener {
    public void onDoubleClick(AdapterView<?> parent, View v, int position);

    public void onSingleClick(AdapterView<?> parent, View v, int position);
  }
}
