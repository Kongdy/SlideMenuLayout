package com.kongdy.slidemenulib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author kongdy
 * @date 2017/12/18 17:32
 * @describe 侧滑栏
 **/
public class SlideMenuLayout extends ViewGroup {

    private final static long DEFAULT_ANIMATION_TIME = 300L;
    private final static float MAX_DRAG_FACTOR = 4f / 5f;
    /**
     * is playing animation
     */
    private final static int SLIDE_MODE_ANIM = 0x01;
    /**
     * is on close
     */
    private final static int SLIDE_MODE_CLOSE = 0x02;
    /**
     * is on touch drag
     */
    private final static int SLIDE_MODE_DRAG = 0x03;
    /**
     * is on open
     */
    private final static int SLIDE_MODE_OPEN = 0x04;


    private View slideMenuView;
    private View contentView;
    private int slideMenuId;
    private int contentViewId;

    private float slideOffset = 0f;

    private int slideMode = SLIDE_MODE_CLOSE;

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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        slideMenuView = findViewById(slideMenuId);
        contentView = findViewById(contentViewId);

        if (null != contentView)
            bringChildToFront(contentView);
    }

    private void applyAttr(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.SlideMenuLayout);

        contentViewId = View.NO_ID;
        slideMenuId = View.NO_ID;
        for (int i = 0; i < ta.getIndexCount(); i++) {
            int index = ta.getIndex(i);
            if (index == R.styleable.SlideMenuLayout_sml_content_id) {
                contentViewId = ta.getResourceId(index, View.NO_ID);
            } else if (index == R.styleable.SlideMenuLayout_sml_menu_id) {
                slideMenuId = ta.getResourceId(index, View.NO_ID);
            }
        }

        ta.recycle();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // handle weather intercept touch event
        switch (ev.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int parentMeasureWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int parentMeasureHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (contentView != null) {
            contentView.measure(MeasureSpec.makeMeasureSpec(parentMeasureWidth, MeasureSpec.EXACTLY
            ), MeasureSpec.makeMeasureSpec(parentMeasureHeight, MeasureSpec.EXACTLY));
        }
        if (slideMenuView != null) {
            slideMenuView.measure(MeasureSpec.makeMeasureSpec((int) (parentMeasureWidth*MAX_DRAG_FACTOR), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(parentMeasureHeight, MeasureSpec.EXACTLY));
        }
    }

    public boolean isOpen() {
        return slideMode == SLIDE_MODE_OPEN;
    }

    public void animToClose() {
        if (slideMode == SLIDE_MODE_ANIM)
            return;
        slideMode = SLIDE_MODE_ANIM;
        Animator valueAnimator = createValueAnim(slideOffset, 0f, SLIDE_MODE_CLOSE);
        valueAnimator.start();
    }

    public void animToOpen() {
        if (slideMode == SLIDE_MODE_ANIM)
            return;
        slideMode = SLIDE_MODE_ANIM;
        Animator valueAnimator = createValueAnim(slideOffset, 1f, SLIDE_MODE_OPEN);
        valueAnimator.start();
    }

    private Animator createValueAnim(float startValue, float endValue, final int result_mode) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(startValue, endValue);
        valueAnimator.setDuration(DEFAULT_ANIMATION_TIME);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                slideOffset = (float) animation.getAnimatedValue();
                requestLayout();
                postInvalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                slideMode = result_mode;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                slideMode = result_mode;
            }
        });
        return valueAnimator;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // to layout menu view and content view
        if (contentView != null) {
            final int contentLeft = (int) (l+slideOffset * MAX_DRAG_FACTOR * (r - l));
            final int contentRight = contentLeft + contentView.getMeasuredWidth();
            contentView.layout(contentLeft, t, contentRight, b);
        }
        if (slideMenuView != null) {
            final int slideMenuWidth = slideMenuView.getMeasuredWidth();
            final int slideMenuHeight = slideMenuView.getMeasuredHeight();
            slideMenuView.layout(l, t, l+slideMenuWidth, t+slideMenuHeight);
        }
    }


    @Override
    public void addView(View child, int index, LayoutParams params) {
        super.addView(child, index, params);
    }


}
