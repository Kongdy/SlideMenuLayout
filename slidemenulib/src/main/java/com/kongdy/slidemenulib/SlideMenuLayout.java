package com.kongdy.slidemenulib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * @author kongdy
 * @date 2017/12/18 17:32
 * @describe 侧滑栏
 **/
public class SlideMenuLayout extends ViewGroup {

    private final static long DEFAULT_ANIMATION_TIME = 249;
    private final static float MAX_DRAG_FACTOR = 5f / 7f;
    private final static float DEFAULT_SCALE_RATE = 1f / 5f;
    /**
     * is playing animation
     */
    private final static int VIEW_MODE_ANIM = 0x01;
    /**
     * is on close
     */
    private final static int SLIDE_MODE_CLOSE = 0x02;
    /**
     * is on touch down
     */
    private final static int VIEW_MODE_TOUCH = 0x03;
    /**
     * is on open
     */
    private final static int SLIDE_MODE_OPEN = 0x04;
    /**
     * is on drag
     */
    private final static int VIEW_MODE_DRAG = 0x05;
    /**
     * is idle mode
     */
    private final static int VIEW_MODE_IDLE = 0x06;

    private View slideMenuView;
    private View contentView;
    private int slideMenuId;
    private int contentViewId;

    private float slideOffset = 0f;

    private int viewMode = SLIDE_MODE_CLOSE;
    private int slideMode = SLIDE_MODE_CLOSE;
    private float preTouchX = 0;
    private float preTouchY = 0;
    private float slideMenuParallaxOffset = 0.5f;
    private boolean haveScaleMode = false;
    private int touchSlop = 0; // 最小滑动距离
    private boolean isClickEvent = true;

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
        // nothing to do
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        touchSlop = vc.getScaledTouchSlop();

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
            } else if (index == R.styleable.SlideMenuLayout_sml_parallax_offset) {
                slideMenuParallaxOffset = ta.getFloat(index, 0.5f);
            } else if (index == R.styleable.SlideMenuLayout_sml_scale_mode) {
                haveScaleMode = ta.getBoolean(index, false);
            }
        }

        ta.recycle();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // handle weather intercept touch event
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                Rect rect = new Rect();
                contentView.getDrawingRect(rect);

                final int touchDownX = (int) ev.getX();
                final int touchDownY = (int) ev.getY();

                if (rect.contains(touchDownX, touchDownY)) {
                    viewMode = VIEW_MODE_TOUCH;
                    return true;
                }
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                if (viewMode == VIEW_MODE_DRAG)
                    return true;
                if (viewMode == VIEW_MODE_TOUCH) {
                    Rect rect = new Rect();
                    contentView.getDrawingRect(rect);

                    final int touchDownX = (int) ev.getX();
                    final int touchDownY = (int) ev.getY();

                    if (rect.contains(touchDownX, touchDownY)) {
                        viewMode = VIEW_MODE_DRAG;
                        final ViewParent viewParent = getParent();
                        if (viewParent != null)
                            viewParent.requestDisallowInterceptTouchEvent(false);
                        return true;
                    } else {
                        resetTouchMode();
                    }
                }
            }
            break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private void resetTouchMode() {
        if (viewMode == VIEW_MODE_TOUCH || viewMode == VIEW_MODE_DRAG) {
            viewMode = VIEW_MODE_IDLE;
            final ViewParent viewParent = getParent();
            if (viewParent != null)
                viewParent.requestDisallowInterceptTouchEvent(false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                preTouchX = event.getX();
                preTouchY = event.getY();
                isClickEvent = true;
                break;
            case MotionEvent.ACTION_MOVE: {
                final float currentTouchX = event.getX();
                final float offsetX = currentTouchX - preTouchX;
                if(Math.abs(offsetX) > touchSlop || !isClickEvent) {
                    isClickEvent = false;
                    int contentLeft = contentView.getLeft();
                    int preCalcLeft = (int) (contentLeft + offsetX);
                    if (preCalcLeft >= 0 && preCalcLeft <= getWidth() * MAX_DRAG_FACTOR) {
                        slideOffset = preCalcLeft / (getWidth() * MAX_DRAG_FACTOR);
                        reDraw();
                    }
                    preTouchX = currentTouchX;
                } else {
                    isClickEvent = true;
                }
            }
            break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                Rect contentViewRect = new Rect();
                contentView.getDrawingRect(contentViewRect);
                if(isClickEvent && isOpen() && contentViewRect.contains((int)event.getX(),(int)event.getY())) {
                    animToClose();
                } else {
                    int contentLeft = contentView.getLeft();
                    final int currentWidth = getWidth();
                    final int halfWidth = currentWidth / 2;
                    int animFactor = (contentLeft + halfWidth) / currentWidth;
                    if (animFactor > 0) {
                        animToOpen();
                    } else {
                        animToClose();
                    }
                    resetTouchMode();
                }
            }

            break;
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int parentMeasureWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int parentMeasureHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (contentView != null) {
            if(haveScaleMode) {
                final float tempScale = (1 - slideOffset * DEFAULT_SCALE_RATE);
                contentView.setScaleX(tempScale);
                contentView.setScaleY(tempScale);
            }
            contentView.measure(MeasureSpec.makeMeasureSpec(parentMeasureWidth, MeasureSpec.EXACTLY
            ), MeasureSpec.makeMeasureSpec(parentMeasureHeight, MeasureSpec.EXACTLY));
        }
        if (slideMenuView != null) {
            slideMenuView.measure(MeasureSpec.makeMeasureSpec((int) (parentMeasureWidth * MAX_DRAG_FACTOR), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(parentMeasureHeight, MeasureSpec.EXACTLY));
        }
    }

    public boolean isOpen() {
        return slideMode == SLIDE_MODE_OPEN;
    }

    public void animToClose() {
        if (viewMode == VIEW_MODE_ANIM)
            return;
        viewMode = VIEW_MODE_ANIM;
        Animator valueAnimator = createValueAnim(slideOffset, 0f, SLIDE_MODE_CLOSE);
        valueAnimator.start();
    }

    public void animToOpen() {
        if (viewMode == VIEW_MODE_ANIM)
            return;
        viewMode = VIEW_MODE_ANIM;
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
                reDraw();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                viewMode = VIEW_MODE_IDLE;
                slideMode = result_mode;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                viewMode = VIEW_MODE_IDLE;
                slideMode = result_mode;
            }
        });
        return valueAnimator;
    }

    private void reDraw() {
        requestLayout();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // to layout menu view and content view
        if (contentView != null) {
            int contentHeight = contentView.getMeasuredHeight();
            final int contentLeft = (int) (l + slideOffset * MAX_DRAG_FACTOR * (r - l));
            final int contentRight = contentLeft + contentView.getMeasuredWidth();
            final int contentTop = t + (b - t - contentHeight) / 2;
            final int contentBottom = contentTop + contentHeight;
            contentView.layout(contentLeft, contentTop, contentRight, contentBottom);
        }
        if (slideMenuView != null) {
            final int slideMenuWidth = slideMenuView.getMeasuredWidth();
            final int slideMenuHeight = slideMenuView.getMeasuredHeight();
            // 视觉滚动差效果
            final int menuLeft = (int) (l - (1 - slideOffset) * MAX_DRAG_FACTOR * (r - l) * slideMenuParallaxOffset);
            final int menuRight = menuLeft + slideMenuWidth;
            slideMenuView.layout(menuLeft, t, menuRight, t + slideMenuHeight);
        }
    }


}
