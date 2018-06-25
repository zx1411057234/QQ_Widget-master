package com.demo.widget.goolview.ui;

import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.demo.widget.goolview.util.GeometryUtil;
import com.demo.widget.goolview.util.Utils;

/**
 * Created by Mercury on 2016/8/12.
 * 最初的粘性控件，也就是圆心固定在屏幕的某个部分，无法应用到列表中使用
 */
public class StickyTestView extends View {

    private Paint paint;        //绘制控件圆形的画笔
    private int statusBarHeight;    //状态栏高度
    private float mTempStickRadius;
    private boolean isOutOfRange=false;       //是否超出范围
    private boolean isDisappear=false;       //控件是否不可见
    private boolean drawEnabled = true;
    private Paint textPaint;    //绘制文字的画笔
    private String text = "1";  //控件内显示的文本

    private boolean DEBUG = true;

    public StickyTestView(Context context) {
        this(context,null);
    }

    public StickyTestView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickyTestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(15f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    PointF mDragCenter = new PointF(150f, 150f);        //拖拽圆圆心初始值（随手势变化）
    float mDragRadius = 14f;                            //拖拽圆半径
    PointF mStickCenter = new PointF(150f, 150f);       //固定圆圆心
    float mStickRadius = 10f;                           //固定圆半径（随手势变化）

    PointF[] mDragPoints = new PointF[]{        //拖拽圆的两个切点初始值
            new PointF(50f, 250f),      //点2
            new PointF(50f, 350f)       //点3
    };

    PointF[] mStickPoints = new PointF[]{       //固定圆的两个切点初始值
            new PointF(250f, 250f),     //点1
            new PointF(250f, 350f)      //点4
    };

    PointF mControlPoint = new PointF(150f, 300f);          //控制点
    float farestDistance = 80f;     //边界值，控制拖拽圆的拖拽范围

    @Override
    protected void onDraw(Canvas canvas) {
        //计算绘制图形所需的变量
        computePoints();

        //        canvas.save();
        //向上平移状态栏的高度
        canvas.translate(0, -statusBarHeight);

        //绘制内容
        drawContent(canvas);

        //        canvas.restore();

    }

    private void computePoints() {
        //1,计算固定圆的实时半径
        mTempStickRadius = computeStickRadius();

        //2,计算控制点坐标，为了绘制Path 贝塞尔曲线
        mControlPoint= GeometryUtil.getMiddlePoint(mDragCenter, mStickCenter);

        //3,计算四个切点坐标，为了绘制固定圆和拖拽圆之间的连接部分
        Double lineK = null;
        double yOffset = mStickCenter.y - mDragCenter.y;
        double xOffset = mStickCenter.x - mDragCenter.x;

        if (xOffset != 0) {
            lineK = yOffset / xOffset;
        }
        //得到拖拽圆的两个切点坐标
        mDragPoints = GeometryUtil.getIntersectionPoints(mDragCenter, mDragRadius, lineK);
        //得到固定圆的两个切点坐标
        mStickPoints = GeometryUtil.getIntersectionPoints(mStickCenter, mTempStickRadius, lineK);


    }

    FloatEvaluator floatEvaluator = new FloatEvaluator();

    //计算随手势变化的固定圆半径
    private float computeStickRadius() {
        //距离:0.0->80f  ——  半径:10f->4f
        //计算固定圆和拖拽圆圆心之间的距离，最大不超过给定的范围值
        float d = GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);
        d = Math.min(d, farestDistance);
        float percent = d / farestDistance;
        //根据距离百分比平滑算出实时的固定圆半径
        return floatEvaluator.evaluate(percent, mStickRadius, mStickRadius * 0.4f);
    }

    private void drawContent(Canvas canvas) {
        //绘制最大范围的一个圆，只是为了显示效果更直观，实际使用中不用绘制
        if (DEBUG) {
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(mStickCenter.x, mStickCenter.y, farestDistance, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        if (!isDisappear) {
            //如果还没有放手并且拖拽圆还在边界范围内
            if (!isOutOfRange) {

                //绘制连接部分 贝塞尔曲线
                Path path = new Path();
                path.moveTo(mStickPoints[0].x,mStickPoints[0].y);
                path.quadTo(mControlPoint.x, mControlPoint.y, mDragPoints[0].x, mDragPoints[0].y);
                path.lineTo(mDragPoints[1].x, mDragPoints[1].y);
                path.quadTo(mControlPoint.x, mControlPoint.y, mStickPoints[1].x, mStickPoints[1].y);
                canvas.drawPath(path, paint);

                //绘制固定圆
                canvas.drawCircle(mStickCenter.x,mStickCenter.y,mTempStickRadius,paint);
                Log.e("back", mStickCenter.x + "-----" + mStickCenter.y);
            }
            //绘制拖拽圆
            canvas.drawCircle(mDragCenter.x, mDragCenter.y, mDragRadius, paint);
            //绘制拖拽圆中的文字
            canvas.drawText(text,mDragCenter.x,mDragCenter.y+mDragRadius/3.0f, textPaint);
        }

    }

    public void setTextNumber(String textNumber) {
        text = textNumber;
    }

    public void backToLayout() {
        drawEnabled = true;
        isDisappear = false;
        isOutOfRange = false;
        mDragCenter = new PointF(150f, 150f);        //拖拽圆圆心初始值（随手势变化）
        mStickRadius = 10f;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x;
        float y;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //                isDisappear = false;
                //                isOutOfRange = false;
                //                x = event.getRawX();
                //                y = event.getRawY();
                //                updateDragCenter(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!drawEnabled) {
                    return true;
                }
                x = event.getRawX();
                y = event.getRawY();
                //更新拖拽圆圆心的坐标
                updateDragCenter(x, y);

                float distance = GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);
                if (distance > farestDistance) {
                    isOutOfRange = true;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                //本次不可绘制
                if (!drawEnabled) {
                    return true;
                }
                //只要在拖拽的过程中拖拽圆超出范围,而不管是不是抬手的时候超出了
                if (isOutOfRange) {
                    float d = GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);
                    if (d > farestDistance) {
                        isDisappear = true;
                        drawEnabled = false;
                        invalidate();
                    } else {
                        //                        updateDragCenter(mStickCenter.x, mStickCenter.y);
                        backToLayout();
                    }
                } else {
                    final PointF start = new PointF(mDragCenter.x, mDragCenter.y);
                    ValueAnimator animator = ValueAnimator.ofFloat(100f);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float fraction = animation.getAnimatedFraction();
                            PointF p = GeometryUtil.getPointByPercent(start, mStickCenter, fraction);
                            updateDragCenter(p.x, p.y);
                        }
                    });
                    animator.setInterpolator(new OvershootInterpolator(2));
                    animator.setDuration(500);
                    animator.start();
                }
                break;

        }
        return true;
    }

    private void updateDragCenter(float x, float y) {
        mDragCenter.set(x, y);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        statusBarHeight = Utils.getStatusBarHeight(this);
    }
}
