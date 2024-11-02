package armmel.home;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.TypedValue;
import java.lang.Float;
import android.util.DisplayMetrics;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import android.content.pm.PackageManager;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff;
public class IconPack {
    private static String folderPath = "arm";
    private String iconPackage = "";
    private PackageManager pm = null;
    private Resources iconPackRes = null;
    private List<Bitmap> mBackImages = new ArrayList<Bitmap>();
    private Bitmap mMaskImage = null;
    private Bitmap mFrontImage = null;
    private float mFactor = 1.0f;
    public IconPack() {
        iconPackage = ""; 
    }
    public IconPack(String iconPackage, PackageManager pm) {
        this.iconPackage = iconPackage;
        this.pm = pm;
        try {
            this.iconPackRes = pm.getResourcesForApplication(iconPackage);
        } catch(PackageManager.NameNotFoundException pn) {
            pn.printStackTrace();
        }
        if(this.iconPackRes != null) {
            loadNeededResource();
        }
    }
    private XmlPullParser loadXMLAppFilter() {
        XmlPullParser xpp = null;
        int appfilterid = iconPackRes.getIdentifier("appfilter", "xml", iconPackage);
        if (appfilterid > 0) {
            xpp = iconPackRes.getXml(appfilterid);
        } else {
            // no resource found, try to open it from assests folder
            try {
                InputStream appfilterstream = iconPackRes.getAssets().open("appfilter.xml");

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                xpp = factory.newPullParser();
                xpp.setInput(appfilterstream, "utf-8");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } 
        return xpp;
    }
    private void loadNeededResource() {
        XmlPullParser xpp = loadXMLAppFilter();
        if (xpp != null) {
            try {
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equals("iconback")) {
                            for(int i=0; i<xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).startsWith("img")) {
                                    String drawableName = xpp.getAttributeValue(i);
                                    Bitmap iconback = loadBitmap(drawableName);
                                    if (iconback != null)
                                        mBackImages.add(iconback);
                                }
                            }
                        } else if (xpp.getName().equals("iconmask")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1"))
                            {
                                String drawableName = xpp.getAttributeValue(0);
                                mMaskImage = loadBitmap(drawableName);
                            }
                        } else if (xpp.getName().equals("iconupon")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1"))
                            {
                                String drawableName = xpp.getAttributeValue(0);
                                mFrontImage = loadBitmap(drawableName);
                            }
                        } else if (xpp.getName().equals("scale")) {
                            // mFactor
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor")) {
                                mFactor = Float.valueOf(xpp.getAttributeValue(0));
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    private Bitmap loadBitmap(String drawableName) {
        int id = iconPackRes.getIdentifier(drawableName, "drawable", iconPackage);

        if (id > 0) {
            Drawable bitmap = iconPackRes.getDrawable(id);
            return convertToBitmap(bitmap,bitmap.getIntrinsicWidth(), bitmap.getIntrinsicHeight());
            //if (bitmap instanceof BitmapDrawable)
            //    return ((BitmapDrawable)bitmap).getBitmap();
        }
        return null;
    }
    public static int pxToDp(int px, Resources res) {
        return  Float.valueOf(px / res.getDisplayMetrics().density).intValue() ;
    }
    public static int PX(Resources res,float dips) {
        return (int) (dips * res.getDisplayMetrics().density + 0.5f);
    }
    public static int dpToPx(float dp, Resources res) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }
    public static Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) {
        Bitmap mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mutableBitmap);
        drawable.setBounds(0, 0, canvas.getWidth(),canvas.getHeight());
        drawable.draw(canvas);

        return mutableBitmap;
    }
    public Bitmap getBitmap(Context c, String packageName) {
        String photoPath = Utils.getRoot(c)+File.separator+folderPath+File.separator+packageName+".png"; 
        if(new File(photoPath).exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(photoPath, options); 
        }
        return null; 
    }
    private String stripComponent(String componentName) {
        int start = componentName.indexOf("{")+1;
        int end = componentName.indexOf("}",  start);
        String drawable ="";
        if (end > start) {
            drawable = componentName.substring(start,end).toLowerCase(Locale.getDefault()).replace(".","_").replace("/", "_");
        }
        return drawable;
    }
    private Bitmap getBitmapFromIconPack(String componentPackageName,Bitmap defaultBitmap) {
        XmlPullParser xpp = loadXMLAppFilter();
        if (xpp != null) {
            try {
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equals("item")) {
                            String componentName = null;
                            String drawableName = null;

                            for(int i=0; i<xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).equals("component")) {
                                    componentName = xpp.getAttributeValue(i);
                                } else if (xpp.getAttributeName(i).equals("drawable")) {
                                    drawableName = xpp.getAttributeValue(i);
                                }
                            }
                            if(componentName.equalsIgnoreCase(componentPackageName)) {
                                if(!drawableName.equalsIgnoreCase("")) {
                                    return generateBitmap(loadBitmap(drawableName));
                                }
                                String stripedComponent = stripComponent(componentName);
                                if (iconPackRes.getIdentifier(stripedComponent, "drawable", iconPackage) > 0)
                                    return generateBitmap(loadBitmap(stripedComponent)); 
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return generateBitmap(defaultBitmap);
    }
    private Bitmap generateBitmap(Bitmap defaultBitmap)
    {
        // if no support images in the icon pack return the bitmap itself
        if (mBackImages.size() == 0)
            return defaultBitmap;

        Random r = new Random();
        int backImageInd = r.nextInt(mBackImages.size());
        Bitmap backImage = mBackImages.get(backImageInd);
        int w = backImage.getWidth();
        int h = backImage.getHeight();

        // create a bitmap for the result
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(result);

        // draw the background first
        mCanvas.drawBitmap(backImage, 0, 0, null);

        // create a mutable mask bitmap with the same mask
        Bitmap scaledBitmap = defaultBitmap;
        if (defaultBitmap != null && (defaultBitmap.getWidth() > w || defaultBitmap.getHeight()> h))
            Bitmap.createScaledBitmap(defaultBitmap, (int)(w * mFactor), (int)(h * mFactor), false);

        if (mMaskImage != null) {
            // draw the scaled bitmap with mask
            Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(mutableMask);
            maskCanvas.drawBitmap(mMaskImage,0, 0, new Paint());

            // paint the bitmap with mask into the result
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            mCanvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth())/2, (h - scaledBitmap.getHeight())/2, null);
            mCanvas.drawBitmap(mutableMask, 0, 0, paint);
            paint.setXfermode(null);
        } else {
            mCanvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth())/2, (h - scaledBitmap.getHeight())/2, null);
        }

        // paint the front
        if (mFrontImage != null) {
            mCanvas.drawBitmap(mFrontImage, 0, 0, null);
        }
        return result;
    }
    public Bitmap getIcon(Context c, Resources resources,String packageName, String componentName, Drawable icon) {
        String photoPath = Utils.getRoot(c)+File.separator+folderPath; 
        if(!new File(photoPath).exists()) {
            new File(photoPath).mkdir(); 
        }
        Bitmap bitmap = null;

        int sizer =(int) resources.getDimension(android.R.dimen.app_icon_size); //getDefaultIconSize(resources);
        if(iconPackage.equalsIgnoreCase("")) {
            bitmap = convertToBitmap(icon,sizer, sizer);
        } else {
            bitmap = getBitmapFromIconPack(componentName,convertToBitmap(icon,sizer,sizer)); 
        }
        if(bitmap != null) {
            try(FileOutputStream file = new FileOutputStream(photoPath+File.separator+packageName+".png"); OutputStream os = new BufferedOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG,0,os);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }
}


