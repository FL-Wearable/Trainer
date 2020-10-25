/*
 *  MIT License
 *
 *  Copyright (c) 2020 miltschek
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package fl.wearable.autosport.lib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

public class MeterView extends View {
    private static final String TAG = MeterView.class.getSimpleName();
    /**
     * The width of a segment, or rather of the segments' ring in pixels.
     */
    private final float SEGMENT_WIDTH = 10f;
    /**
     * The paint for covering segments to look like the background.
     */
    private final Paint bgPaint;
    /**
     * The last set size of the client area in pixels.
     */
    private int width, height;
    /**
     * The radius of the outer circle in pixels.
     */
    private float outRadius;
    /**
     * The radius of the inner circle in pixels.
     */
    private float inRadius;
    /**
     * The middle of the circle in pixels.
     */
    private float midX;
    private float midY;
    /**
     * Number of segments in total.
     */
    private int totalSegments;
    /**
     * The paint of each segment.
     */
    private Paint[] segmentPaint;
    /**
     * The angle step between the segments in degrees.
     */
    private float stepDeg;
    /**
     * The angle size of each segment in degrees.
     */
    private float segmentSizeDeg;
    /**
     * The value to be shown as a fraction of all segments.
     */
    private float value = 1f;
    /**
     * The flag indicating whether the screen is cut at the bottom.
     */
    private boolean chinDetected = false;
    /**
     * THe flag indicating whether the watch face is rounded.
     */
    private boolean roundFace = false;
    /**
     * The size of the chin in pixels.
     */
    private int chinSize;
    /**
     * The intersection points of the inner circle and the segments shifted by the chin.
     */
    private float chinX1, chinX2;
    /**
     * The angles of the intersection points of the inner circle and the segments shifted by the chin.
     */
    private float chinAngle1, chinAngle2;

    public MeterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(0xff000000);

        setSegments(40);
    }

    public final void setSegments(int totalSegments) {
        this.totalSegments = totalSegments;

        this.stepDeg = 360f / totalSegments;
        this.segmentSizeDeg = 0.7f * stepDeg;

        calculateSegmentPaint();

        invalidate();
    }

    public void setValue(float value) {
        this.value = value;
        invalidate();
    }

    private void calculateSegmentPaint() {
        segmentPaint = new Paint[totalSegments];
        for (int n = 0; n < segmentPaint.length; n++) {
            float proportion = (float) n / totalSegments;

            segmentPaint[n] = new Paint(Paint.ANTI_ALIAS_FLAG);
            segmentPaint[n].setStyle(Paint.Style.FILL);

            float rFactor = (proportion < 0.375f) ? (0.375f - proportion) / 0.375f :
                    (proportion < 0.5f) ? 0 :
                            (proportion < 0.675f) ? (proportion - 0.5f) / 0.175f :
                                    (proportion < 0.875f) ? 1 :
                                            1.875f - proportion;
            float gFactor = (proportion < 0.75f) ? 1 :
                    (proportion < 0.875f) ? (0.875f - proportion) / 0.125f :
                            0;
            float bFactor = (proportion < 0.375f) ? (0.375f - proportion) / 0.375f :
                    0;

            segmentPaint[n].setColor((0xff << 24) + ((int) (rFactor * 0xff) << 16) + ((int) (gFactor * 0xff) << 8) + (int) (bFactor * 0xff));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setBackgroundColor(0xff000000);

        int stop = (int) (value * totalSegments);
        if (totalSegments < stop) {
            stop = totalSegments;
        }

        for (int n = 0; n < stop; n++) {
            float posDeg = n * stepDeg - 90;
            //canvas.drawArc(2, 2, width - 2, height - 2, posDeg - segmentSizeDeg / 2f, segmentSizeDeg, true, segmentPaint[n]);
            canvas.drawArc(midX - outRadius, midY - outRadius, midX + outRadius, midY + outRadius, posDeg - segmentSizeDeg / 2f, segmentSizeDeg, true, segmentPaint[n]);
        }

        if (this.chinDetected) {
            // draw an arc over the segments not affected by the chin
            RectF oval = new RectF();
            oval.set(midX - inRadius, midY - inRadius, midX + inRadius, midY + inRadius);
            canvas.drawArc(oval, chinAngle1, chinAngle2, false, bgPaint);
            // draw a rectangle over the segments shifted by the chin
            canvas.drawRect(chinX1, midY, chinX2, this.height - chinSize - SEGMENT_WIDTH, bgPaint);
        } else if (roundFace) {
            // draw a circle over all segments
            canvas.drawCircle(midX, midY, inRadius, bgPaint);
        } else {
            // draw a rectangle over the segments assuming a rectangle face
            canvas.drawRect(SEGMENT_WIDTH, SEGMENT_WIDTH, this.width - SEGMENT_WIDTH, this.height - SEGMENT_WIDTH, bgPaint);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.width = w;
        this.height = h;
        int longer = (width > height) ? width : height;
        int shorter = (width < height) ? width : height;
        this.roundFace = getRootWindowInsets().isRound();
        if (roundFace) {
            this.outRadius = shorter / 2f;
            this.inRadius = outRadius - SEGMENT_WIDTH;
        } else {
            this.outRadius = (float) (longer * Math.sqrt(2) / 2);
            this.inRadius = shorter / 2f - SEGMENT_WIDTH;
        }

        this.midX = width / 2f;
        this.midY = height / 2f;

        this.chinDetected = getRootWindowInsets().getSystemWindowInsetBottom() > 0;
        if (this.chinDetected) {
            // calculate intersection points of the inner circle and the (chin + segments' width)
            this.chinSize = getRootWindowInsets().getSystemWindowInsetBottom();
            float bottomMidY = this.chinSize + SEGMENT_WIDTH - midY;
            float roots = inRadius * inRadius - bottomMidY * bottomMidY;
            if (roots >= 0) {
                float plusMinus = (float) Math.sqrt(roots);
                // chin's X1 and X2 are the intersection coordinates
                this.chinX1 = midX - plusMinus;
                this.chinX2 = midX + plusMinus;
                // the angle to the intersection points (positive value measured from the south)
                float angle = (float) (Math.asin(plusMinus / this.inRadius) * 180 / Math.PI);
                // chin angle 1 = beginning angle of the arc drawn later
                this.chinAngle1 = 90 + angle;
                // chin angle 2 = sweep angle of the arc drawn later
                this.chinAngle2 = 360 - 2 * angle;
            } else {
                // a not expected geometry case did happen, ignore chin in that case
                Log.d(TAG, "Can't calculate the roots - negative value.");
                this.chinDetected = false;
            }
        }
    }
}
