package me.yluo.androidble.color;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import me.yluo.androidble.Utils;

public class ColorPicker extends View {

    private static final int[] COLORS = new int[]{
            0xFFFF0000, 0xFFFF00FF, 0xFF0000FF,
            0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00,
            0xFFFF0000};
    private Paint mColorWheelPaint;
    private Paint mPointerHaloPaint;
    private Paint mPointPaint;

    private int colorWheelWidth;
    private int colorWheelRadius;
    private RectF mColorWheelRectangle = new RectF();
    private RectF mCenterRectangle = new RectF();
    private int pointHoloWidth;
    private int pointRadius;
    private int centerPointRadius;
    private boolean mUserIsMovingPointer = false;
    private int mColor, mOldColor;
    private float mTranslationOffset;
    private float mSlopX;
    private float mSlopY;
    private float mAngle;
    private ColorChangeListener colorChangeListener;

    private RgbBar mArgbBar = null;

    public ColorPicker(Context context) {
        super(context);
        init();
    }

    public ColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }


    private void init() {
        setColor(0xff39c5bb);
        Shader s = new SweepGradient(0, 0, COLORS, null);
        mColorWheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mColorWheelPaint.setShader(s);
        mColorWheelPaint.setStyle(Paint.Style.STROKE);
        mColorWheelPaint.setStrokeWidth(colorWheelWidth);

        mPointerHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointerHaloPaint.setColor(Color.BLACK);
        mPointerHaloPaint.setAlpha(0x50);

        mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointPaint.setColor(angleToColor(mAngle));
        mPointPaint.setStyle(Paint.Style.FILL);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int intrinsicSize = Utils.dp2px(getContext(), 120);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(intrinsicSize, heightSize);
        } else {
            height = intrinsicSize;
        }

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(intrinsicSize, widthSize);
        } else {
            width = intrinsicSize;
        }


        int min = Math.min(width, height);
        setMeasuredDimension(min, min);
        mTranslationOffset = min * 0.5f;
        pointHoloWidth = Utils.dp2px(getContext(), 2);
        pointRadius = Utils.dp2px(getContext(), 10);
        colorWheelWidth = pointRadius / 2;
        colorWheelRadius = min / 2 - colorWheelWidth - pointRadius - pointHoloWidth;
        mColorWheelRectangle.set(-colorWheelRadius, -colorWheelRadius, colorWheelRadius, colorWheelRadius);
        mColorWheelPaint.setStrokeWidth(colorWheelWidth);
        centerPointRadius = colorWheelRadius - pointRadius - pointHoloWidth - Utils.dp2px(getContext(), 12);
        mCenterRectangle.set(-centerPointRadius, -centerPointRadius, centerPointRadius, centerPointRadius);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mColor != mOldColor) {
            mOldColor = mColor;
            if (colorChangeListener != null) {
                colorChangeListener.onColorChange(mColor);
            }
        }
        mPointPaint.setColor(mColor);
        canvas.translate(mTranslationOffset, mTranslationOffset);
        canvas.drawOval(mColorWheelRectangle, mColorWheelPaint);
        float[] pointerPosition = calculatePointerPosition(mAngle);
        canvas.drawCircle(pointerPosition[0], pointerPosition[1], pointRadius + 15, mPointerHaloPaint);

        canvas.drawCircle(pointerPosition[0], pointerPosition[1], pointRadius, mPointPaint);
        canvas.drawArc(mCenterRectangle, 0, 360, true, mPointPaint);
    }


    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color|0xff000000;
        mAngle = colorToAngle(mColor);
        changeArgbBarColor(mColor);
        invalidate();
    }

    public void setColorChangeListener(ColorChangeListener colorChangeListener) {
        this.colorChangeListener = colorChangeListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        float x = event.getX() - mTranslationOffset;
        float y = event.getY() - mTranslationOffset;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check whether the user pressed on the pointer.
                float[] pointerPosition = calculatePointerPosition(mAngle);
                int len = pointRadius + pointHoloWidth;
                if (x >= (pointerPosition[0] - len) && x <= (pointerPosition[0] + len)
                        && y >= (pointerPosition[1] - len) && y <= (pointerPosition[1] + len)) {
                    mSlopX = x - pointerPosition[0];
                    mSlopY = y - pointerPosition[1];
                    mUserIsMovingPointer = true;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mUserIsMovingPointer) {
                    mAngle = (float) Math.atan2(y - mSlopY, x - mSlopX);
                    mColor = angleToColor(mAngle);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mUserIsMovingPointer = false;
                invalidate();
                break;
        }

        changeArgbBarColor(mColor);
        return true;
    }

    private float[] calculatePointerPosition(float angle) {
        float x = (float) (colorWheelRadius * Math.cos(angle));
        float y = (float) (colorWheelRadius * Math.sin(angle));
        return new float[]{x, y};
    }

    private float colorToAngle(int color) {
        float[] colors = new float[3];
        Color.colorToHSV(color, colors);
        return (float) Math.toRadians(-colors[0]);
    }

    private int angleToColor(float angle) {
        float unit = (float) (angle / (2 * Math.PI));
        if (unit < 0) {
            unit += 1;
        }

        if (unit <= 0) {
            mColor = COLORS[0];
            return COLORS[0];
        }
        if (unit >= 1) {
            mColor = COLORS[COLORS.length - 1];
            return COLORS[COLORS.length - 1];
        }
        float p = unit * (COLORS.length - 1);
        int i = (int) p;
        p -= i;
        int c0 = COLORS[i];
        int c1 = COLORS[i + 1];
        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);
        mColor = Color.argb(a, r, g, b);
        return Color.argb(a, r, g, b);
    }


    public void addArgbBar(RgbBar bar) {
        mArgbBar = bar;
        mArgbBar.setColorPicker(this);
        mArgbBar.setColor(mColor);
    }

    public void changeArgbBarColor(int color) {
        if (mArgbBar != null && mArgbBar.getColor() != color) {
            mArgbBar.setColor(color);
        }
    }

    public interface ColorChangeListener {
        void onColorChange(int color);
    }
}
