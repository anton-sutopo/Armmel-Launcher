package armmel.home;

import android.view.ViewGroup;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.AdapterView;
public class GridLayout extends AdapterView<ApplicationAdapter>  {   
    private int numRows = 6;
    private int numColumns = 5;
    private ApplicationAdapter adapter;
    private DoubleClick.DoubleClickListener doubleClickListener;
    private DoubleClick doubleClick;
    private AdapterView.OnItemLongClickListener onItemLongClickListener;
    public GridLayout(Context context) {
        super(context);
    }

    public GridLayout(Context context, AttributeSet attrs) {
        super(context,attrs);
    }

    public GridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
    }
    @Override
    public View getSelectedView() {
        return null; 
    }
    @Override
    public void setSelection(int position) {
    
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = 0;
        int childTop = 0;
        int childWidth = getWidth() /numColumns;
        int childHeight = getHeight()/numRows;
        int childCount = getChildCount();
        for(int i = 0; i< childCount;i++) {
            View child = getChildAt(i);
            child.layout(childLeft,childTop,childLeft+childWidth, childTop+childHeight);

            childLeft += childWidth;
            if((i+1) % numColumns == 0) {
                childLeft = 0;
                childTop += childHeight;
            }
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize/numColumns, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize/numRows, MeasureSpec.EXACTLY);
        measureChildren(childWidthMeasureSpec, childHeightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
        requestLayout();
    }

    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
        requestLayout();
    }
    @Override 
    public ApplicationAdapter getAdapter() {
        return adapter;
    }
    @Override 
    public void setAdapter(ApplicationAdapter adapter) {
        this.adapter = adapter;
        populateViews();
    }
    private void populateViews() {
        removeAllViewsInLayout();
        for(int i = 0; i < adapter.getCount();i++) {
            View view = adapter.getView(i,null,this);
            final int position = i;
            doubleClick =new DoubleClick(GridLayout.this, position); 
            doubleClick.setDoubleClickListener(new DoubleClick.DoubleClickListener() {
                public void onDoubleClick(AdapterView<?> parent, View v, int position) {
                    if(doubleClickListener != null) {
                        doubleClickListener.onDoubleClick(parent, v, position);
                    }
                }
                public void onSingleClick(AdapterView<?> parent, View v, int position) {
                    if(doubleClickListener != null) {
                        doubleClickListener.onSingleClick(parent,v,position);
                    }
                }

            });
            view.setOnClickListener(doubleClick);
            /*view.setOnClickListener(new View.OnClickListener() { 
                @Override 
                public void onClick(View v) {
                    if(onItemClickListener != null) {
                       onItemClickListener.onItemClick(GridLayout.this,v,position,position); 
                    }
                }
            });*/
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(onItemLongClickListener != null) {
                        return onItemLongClickListener.onItemLongClick(GridLayout.this,v,position,position);
                    }
                    return false;
                }
            });
            addViewInLayout(view, i, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT),true);
        }
        requestLayout();
    }
    
    public void setOnItemDoubleClickListener(DoubleClick.DoubleClickListener listener) {
        this.doubleClickListener = listener;
    }
    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
      this.onItemLongClickListener = listener;  
    }
}

