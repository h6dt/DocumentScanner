package com.tdh7.documentscanner.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;

import com.tdh7.documentscanner.R;

public class CaptureView extends View {
    private static final String TAG = "CaptureView";

    public static final int MODE_AUTO_CAPTURE = 0;
    public static final int MODE_MANUAL_CAPTURE = 1;

    public int getCaptureMode() {
        return mCaptureMode;
    }

    public void setCaptureMode(int captureMode) {
        mCaptureMode = captureMode;

        if(mCaptureMode==MODE_AUTO_CAPTURE && !mAutoAnimator.isRunning()) {
            mCurrentActiveValue = 0;
            mAutoAnimator.start();
        } else if(mCaptureMode!=MODE_AUTO_CAPTURE && mAutoAnimator.isRunning()) {
            mCurrentActiveValue = 0;
            mAutoAnimator.cancel();
        }
        update();
    }

    public void onCaptured() {
        if(mActiveAnimator.isRunning()) mAutoAnimator.cancel();
        mCurrentActiveValue = 0;
        mActiveAnimator.start();
    }

    private int mCaptureMode = MODE_AUTO_CAPTURE;

    Paint mPaint;
    private int mOutsideColor = 0xffff9500;
    private int mAutolineColor = 0xffff9500;
    private int mInsideColor = 0xffff9500;
    private int mInsideActiveColor = 0xffffaa00;

    private float mStrokeOutlinePercent = 5f/60;
    private float mDistancePercent = 3f/60;
    private float mDistanceActivePercent = 5f/60;
    private int mAnimateSwitchDuration = 350;

    public int getAnimateSwitchDuration() {
        return mAnimateSwitchDuration;
    }

    public void setAnimateSwitchDuration(int animateSwitchDuration) {
        mAnimateSwitchDuration = animateSwitchDuration;
        mActiveAnimator.setDuration(animateSwitchDuration);
    }

    public int getAutoDurationPerRound() {
        return mAutoDurationPerRound;
    }

    public void setAutoDurationPerRound(int autoDurationPerRound) {
        mAutoDurationPerRound = autoDurationPerRound;
        mAutoAnimator.setDuration(autoDurationPerRound);
    }

    private int mAutoDurationPerRound = 600;

    public int getOutsideColor() {
        return mOutsideColor;
    }

    public void setOutsideColor(int outsideColor) {
        mOutsideColor = outsideColor;
    }

    public int getAutolineColor() {
        return mAutolineColor;
    }

    public void setAutolineColor(int autolineColor) {
        mAutolineColor = autolineColor;
    }

    public int getInsideColor() {
        return mInsideColor;
    }

    public void setInsideColor(int insideColor) {
        mInsideColor = insideColor;
    }



    public CaptureView(Context context) {
        super(context);
        init(null);
    }

    public CaptureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CaptureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void initValueAnimator() {
        mAutoAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAutoAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAutoAnimator.setStartDelay(100);
        mAutoAnimator.setInterpolator(new DecelerateInterpolator());

        mAutoAnimator.addUpdateListener(valueAnimator -> {
            mCurrentAutoValue =  2f * (float)valueAnimator.getAnimatedValue();
            update();
        });

        mActiveAnimator.setRepeatCount(1);
        mActiveAnimator.setRepeatMode(ValueAnimator.REVERSE);
       // mActiveAnimator.setInterpolator(new OvershootInterpolator());
        mActiveAnimator.addUpdateListener(valueAnimator -> {
            mCurrentActiveValue = (float) valueAnimator.getAnimatedValue();
            update();
        });
    }

    private void init(AttributeSet attrs) {
        initValueAnimator();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if(attrs!=null) {
            TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.CaptureView);
            mInsideColor = t.getColor(R.styleable.CaptureView_insideColor,0xffff9500);
            mInsideActiveColor = t.getColor(R.styleable.CaptureView_insideActiveColor,0xffffaa00);
            mOutsideColor = t.getColor(R.styleable.CaptureView_outlineColor,0xffff9500);
            mAutolineColor = t.getColor(R.styleable.CaptureView_autolineColor,0xffff9500);
            mStrokeOutlinePercent = t.getFloat(R.styleable.CaptureView_strokeOutlinePercent,5f/60);
            mDistancePercent = t.getFloat(R.styleable.CaptureView_distancePercent,3f/60);

            mDistanceActivePercent = t.getFloat(R.styleable.CaptureView_distanceActivePercent,5f/60);
            setAnimateSwitchDuration(t.getInteger(R.styleable.CaptureView_animateSwitchDuration,350));
            setAutoDurationPerRound(t.getInteger(R.styleable.CaptureView_autoDurationPerRound,600));
            setCaptureMode(t.getInt(R.styleable.CaptureView_captureMode,MODE_AUTO_CAPTURE));
            t.recycle();
        }


        setWillNotDraw(false);
    }

    private int mDrawTop;
    private int mDrawBottom;
    private int mDrawLeft;
    private int mDrawRight;
    private int mDrawWidth;
    private int mDrawHeight;

    private int mCenterX;
    private int mCenterY;
    private float mRadius;
    private float mSize;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mDrawTop = getPaddingTop();
        mDrawBottom = h - getPaddingBottom();
        mDrawLeft = getPaddingStart();
        mDrawRight = w - getPaddingEnd();

       mDrawWidth = mDrawRight - mDrawLeft;
       mDrawHeight = mDrawBottom - mDrawTop;
       mSize = Math.min(mDrawWidth, mDrawHeight);
       mCenterX = mDrawLeft + mDrawWidth/2;
       mCenterY = mDrawTop + mDrawHeight/2;
       mRadius = mSize/2;

        if (mRectF==null)
            mRectF = new RectF(mDrawLeft,mDrawTop, mDrawRight,mDrawBottom);
        else {
            mRectF.left = mDrawLeft;
            mRectF.top = mDrawTop;
            mRectF.right = mDrawRight;
            mRectF.bottom = mDrawBottom;
        }
    }

    public static float interpolate(float start,float end, float f) {
        return start + f * (end - start);
    }

    float mCurrentActiveValue = 0;
    ValueAnimator mActiveAnimator = ValueAnimator.ofFloat(0,1);

    float mCurrentAutoValue = 0;
    ValueAnimator mAutoAnimator = ValueAnimator.ofFloat(0,1);

    private void update() {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawManualCapture(canvas);
        drawAutoCapture(canvas);
    }

    private void drawManualCapture(Canvas canvas) {
        float percent = 1;
        percent -= mStrokeOutlinePercent;

        if(mCaptureMode==MODE_MANUAL_CAPTURE) {
            mPaint.setColor(mOutsideColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mStrokeOutlinePercent*mRadius);
            canvas.drawCircle(mCenterX, mCenterY, mRadius - mStrokeOutlinePercent * mSize / 4, mPaint);
        }

        percent -= interpolate(mDistancePercent, mDistanceActivePercent,mCurrentActiveValue);

        if(mCurrentActiveValue==0)
        mPaint.setColor(mInsideColor);
        else mPaint.setColor(mInsideActiveColor);

        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(mCenterX,mCenterY, percent*mRadius,mPaint);
    }
    private RectF mRectF;

    private void drawAutoCapture(Canvas canvas){
        if(mCaptureMode==MODE_AUTO_CAPTURE) {
            mPaint.setColor(mAutolineColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mStrokeOutlinePercent*mRadius);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                if(mCurrentAutoValue<1)
                canvas.drawArc(
                        mDrawLeft + mStrokeOutlinePercent*mSize/4,
                        mDrawTop+mStrokeOutlinePercent*mSize/4,
                        mDrawRight - mStrokeOutlinePercent*mSize/4,
                        mDrawBottom - mStrokeOutlinePercent*mSize/4,
                        -90,
                        mCurrentAutoValue*360f,
                        false,mPaint);
                else
                    canvas.drawArc(
                            mDrawLeft + mStrokeOutlinePercent*mSize/4,
                            mDrawTop+mStrokeOutlinePercent*mSize/4,
                            mDrawRight - mStrokeOutlinePercent*mSize/4,
                            mDrawBottom - mStrokeOutlinePercent*mSize/4,
                            -90 + 360f*(mCurrentAutoValue -1 ),
                             360 - 360*(mCurrentAutoValue -1),
                            false,mPaint);
            } else {

                canvas.drawArc(mRectF, -90, 360, false, mPaint);
                mPaint.setAlpha(205);
                canvas.drawArc(mRectF,-90,mCurrentAutoValue*360f,false,mPaint);
            }
        }
    }
}