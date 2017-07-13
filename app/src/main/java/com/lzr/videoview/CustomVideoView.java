package com.lzr.videoview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by Administrator on 2017/7/13.
 */

public class CustomVideoView extends VideoView {
    private int defaultWidth = 1080;
    private int defaultHeight = 1920;

    public CustomVideoView(Context context) {
        super(context);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获取测量结果，第一个参数是默认值，第二个测量结果
        int width = getDefaultSize(defaultWidth,widthMeasureSpec);
        int height = getDefaultSize(defaultHeight,heightMeasureSpec);
//        Log.e("lzr","width=="+width+",height=="+height);
        setMeasuredDimension(width,height);
    }
}
