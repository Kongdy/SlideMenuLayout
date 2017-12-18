package com.kongdy.slidemenulib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * @author kongdy
 * @date 2017/12/18 17:32
 * @describe 侧滑栏
 **/
public class SlideMenuLayout extends ViewGroup {

    public SlideMenuLayout(Context context) {
        this(context, null);
    }

    public SlideMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SlideMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initComponent();
        applyAttr(attrs);
    }

    private void initComponent() {

    }

    private void applyAttr(AttributeSet attrs) {

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }
}
