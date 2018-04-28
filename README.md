# SlideMenuLayout
带有视觉滚动差的侧滑栏


# 前文

之前看到酷狗app的侧滑栏比较有有意思，带有视觉滚动差还有缩放效果，自己就尝试的实现了一个。
<br/>这个组件其实可以使用HorScrollView实现，但是使用HorScrollView终归还是要重写触摸事件的，并且HorScrollView对这个控件没有任何帮助，不如使用更轻量级的ViewGroup来实现。

# 我们先来看看效果

![带有视觉滚动差的侧滑菜单](http://img.blog.csdn.net/20180110143814542?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvdTAxNDMwMzAwMw==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)


# 如何实现视觉滚动差效果

我的实现方法比较笨，在layout根据一个滑动参数offset来进行layout的错位增量。
<br/>layout的代码如下

```java
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
```

一个contentView，一个menuView，分别进行layout，但是他们从哪里被控件获取到的呢？或者说，控件怎么知道哪个是contentView，哪个是menuVIew？这里，我采用了根据attr获取子控件id的方法。如下图所示

```xml
 <com.kongdy.slidemenulib.SlideMenuLayout
        android:id="@+id/sml_menu"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/colorAccent"
        app:sml_content_id="@+id/cl_content"
        app:sml_menu_id="@+id/cl_slide_menu"
        app:sml_scale_mode="true">

			<android.support.constraint.ConstraintLayout
	            android:id="@id/cl_content">
	            ...
			</android.support.constraint.ConstraintLayout>

			 <android.support.constraint.ConstraintLayout
		            android:id="@id/cl_slide_menu">
		            ...
            </android.support.constraint.ConstraintLayout>

        </com.kongdy.slidemenulib.SlideMenuLayout>
```

<br/>把menuView和contentView的控件id分别赋值到属性中。然而这里并没有结束，因为，我们在构造方法中获取到了两个id，但是我们并拿不到这两个控件，因为布局还没有inflate完毕。但是，还好，安卓为我们提供了这个方法。如下：

```java
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        slideMenuView = findViewById(slideMenuId);
        contentView = findViewById(contentViewId);

        if (null != contentView)
            bringChildToFront(contentView);
    }

```

这里还用到了bringChildToFront这个方法，这是viewGroup提供的一个方法，我们来看看这个方法：

```java
 @Override
    public void bringChildToFront(View child) {
        final int index = indexOfChild(child);
        if (index >= 0) {
            removeFromArray(index);
            addInArray(child, mChildrenCount);
            child.mParent = this;
            requestLayout();
            invalidate();
        }
    }
```

<br/>这个方法把目标子view从childiList中取出来，然后重新放到childList的最后端，那么viewGroup正在渲染它的时候，就会把它放到最后渲染上去，也就会显示在最上层。这么一来，就可以保证我们的contentView一直在我们当前viewGroup的最上层显示。

# 处理触摸事件

之前在[android图片裁剪拼接实现（二）：触摸实现](http://blog.csdn.net/u014303003/article/details/78921399) 中讲解了触摸的流程。在本控件中，viewGroup的分发机制已经很完善，我们不需要去重写dispatchTouchEvent，只需要去写onInterceptTouchEvent来判断是否去拦截。onInterceptTouchEvent的代码如下:

```java
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
```

<br/>这里首先判断了触摸落下的点是否在contentView之内，然后再判断首次滑动的的触摸点是否仍然在contentView之内，如果两个都符合的话，就调用requestDisallowInterceptTouchEvent方法请求父控件不要拦截自己接下来的触摸事件，并且返回true，此次的触摸事件交给viewGroup的touchEvent来处理。以下是touchEvent里面的处理代码:

```java

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

```

<br/>这里我们先计算出本次触摸点与上一次触摸点移动的距离offsetX，然后判断这个offsetX是否大于touchSlop，touchSlop是在构造方法中，从系统中获取到的滑动最小值。当超过这个值得时候，我们判定为滑动，并且将isClickEvent置为false，否则isClickEvent置为ture，相当于点击事件。随后进入拖动状态，我们要预计算出contentView的left值，如果这个值小于左边的边界，或者大于向右边的最大滑动距离都不被允许，虽然把这个preCalcLeft的预计算left根据参数计算成当前滑动的位移率来供全局使用。

# 动画

最后，我们在触摸抬起或者取消的时候，要做一个滑动动画，动画的实现方式很简单，我这里贴出代码即可：

```java
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
```

# 如何使用

首先在自己的项目的根目录的gradle下添加:

```java
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

<br/>随后添加依赖:

```java
dependencies {
		implementation 'com.github.Kongdy:SlideMenuLayout:1.0.7'
	}
```

<br/>在项目中,xml标签里面如下声明：
```xml
 <com.kongdy.slidemenulib.SlideMenuLayout
        android:id="@+id/sml_menu"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/colorAccent"
        app:sml_content_id="@+id/cl_content"
        app:sml_menu_id="@+id/cl_slide_menu"
        app:sml_scale_mode="true">

			<android.support.constraint.ConstraintLayout
	            android:id="@id/cl_content">
	            ...
			</android.support.constraint.ConstraintLayout>

			 <android.support.constraint.ConstraintLayout
		            android:id="@id/cl_slide_menu">
		            ...
            </android.support.constraint.ConstraintLayout>

        </com.kongdy.slidemenulib.SlideMenuLayout>
```

1. app:sml_content_id  内容控件id
2.  app:sml_menu_id 菜单控件id
3.  app:sml_scale_mode 是否启用内容控件随动缩放


# 常用方法

1. animToOpen() 执行打开菜单动画
2.  animToClose() 执行关闭菜单动画
3.  isOpen() 当前是否处于菜单打开状态


本文代码:[https://github.com/Kongdy/SlideMenuLayout](https://github.com/Kongdy/SlideMenuLayout)<br/>
个人github地址:[https://github.com/Kongdy](https://github.com/Kongdy)<br/>
个人掘金主页:[https://juejin.im/user/595a64def265da6c2153545b](https://juejin.im/user/595a64def265da6c2153545b)<br/>
csdn主页:[http://blog.csdn.net/u014303003](http://blog.csdn.net/u014303003)<br/>