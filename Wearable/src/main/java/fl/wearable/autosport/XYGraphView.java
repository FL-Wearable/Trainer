package fl.wearable.autosport;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class XYGraphView extends View {

    private Paint paint, greenPaint, redPaint;
    private int w, h;
    private List<XYData> xyDataCollection = new ArrayList<>();

    public XYGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);

        greenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenPaint.setStyle(Paint.Style.FILL);
        greenPaint.setColor(Color.GREEN);

        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setStyle(Paint.Style.FILL);
        redPaint.setColor(Color.RED);

        //paint.setTextSize(textHeight);
        setMinimumHeight(70);
    }

    public XYData addDataSet(int color, float maxValue) {
        XYData xyData = new XYData(200, color, maxValue);

        xyDataCollection.add(xyData);
        return  xyData;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Log.i("milt/XY", "onDraw");

        //canvas.drawRect(0, 0,w - 1, h - 1, paint);
        final int scale = 6;
        final int xSpace = w / scale;

        for (XYData xyData : xyDataCollection) {
            int size = xyData.size();

            for (int n = 0; n < xyData.markers.length; n++) {
                float height = h - xyData.markers[n] * h;
                canvas.drawLine(0, height, w, height, xyData.markerPaints[n]);
            }

            if (size > 0) {
                Paint xyPaint = xyData.paint;

                // starting element to be drawn
                int offset = (xSpace >= size ? 0 : size - xSpace);
                // starting position on the graph to be drawn
                int begin = (xSpace >= size ? xSpace - size : 0);

                // guaranteed: offset <= size
                // so 'n' will not pass the stopIndex
                int n = xyData.startIndex + offset;
                if (n >= xyData.valuesNormalized.length) {
                    n -= xyData.valuesNormalized.length;
                }

                int xPosition = begin;

                while (true) {
                    float currentY = h - (xyData.valuesNormalized[n] * h);
                    canvas.drawRect(xPosition * scale, currentY, (xPosition + 1) * scale - 1, h, xyPaint);

                    if (n == xyData.stopIndex) {
                        break;
                    }

                    xPosition++;
                    n++;
                    if (n >= xyData.valuesNormalized.length) {
                        n = 0;
                    }
                }

                String header = String.valueOf(xyData.lastValue);
                Rect boundsHeader = new Rect(), boundsSubtitle = new Rect();
                /*xyData.paintHeader.getTextBounds(header, 0, header.length(), boundsHeader);
                canvas.drawText(header, 2, -boundsHeader.top + 2, xyData.paintHeaderBlend);
                canvas.drawText(header, 0, -boundsHeader.top, xyData.paintHeader);*/

                String subtitle = String.format("Ø %.0f", xyData.avgValue);
                xyData.paintSubtitle.getTextBounds(subtitle, 0, subtitle.length(), boundsSubtitle);
                canvas.drawText(subtitle, 2, /*boundsHeader.height() - */-boundsSubtitle.top + 5, xyData.paintSubtitleBlend);
                canvas.drawText(subtitle, 0, /*boundsHeader.height() - */-boundsSubtitle.top + 3, xyData.paintSubtitle);
            }
        }

        /*for (int n = 1; n < values.length; n++) {
            canvas.drawLine(n - 1, values[n - 1], n, values[n], paint);
        }*/
        //canvas.drawLine(0, 0, this.w / 2, this.h, this.paint);
        //canvas.drawOval(0, 0, this.w, this.h, paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.w = w;
        this.h = h;
        for (XYData xyData : xyDataCollection) {
            xyData.paintHeader.setTextSize(h / 2);
            xyData.paintSubtitle.setTextSize(h / 4);
            xyData.paintHeaderBlend.setTextSize(h / 2);
            xyData.paintSubtitleBlend.setTextSize(h / 4);
        }
    }

    public class XYData {
        private int startIndex, stopIndex;
        private float maxValue;
        private float[] valuesNormalized;
        private float avgValue;
        private int avgCount;
        private float lastValue;
        private float[] markers = new float[0];

        private final Paint paint, paintHeader, paintSubtitle, paintHeaderBlend, paintSubtitleBlend;
        private Paint[] markerPaints = new Paint[0];

        public XYData(int size, int color, float maxValue) {
            this.valuesNormalized = new float[size];
            this.paint = new Paint();
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.FILL_AND_STROKE);

            this.paintHeader = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.paintHeader.setColor(ColorUtils.blendARGB(color, Color.BLACK, 0.2f));
            this.paintHeader.setStyle(Paint.Style.FILL_AND_STROKE);
            this.paintHeader.setElegantTextHeight(true);
            this.paintHeader.setTypeface(Typeface.DEFAULT_BOLD);

            this.paintSubtitle = new Paint(this.paintHeader);

            this.paintHeaderBlend = new Paint(this.paintHeader);
            this.paintHeaderBlend.setColor(ColorUtils.blendARGB(this.paintHeader.getColor(), Color.BLACK, 0.8f));

            this.paintSubtitleBlend = new Paint(this.paintSubtitle);
            this.paintSubtitleBlend.setColor(ColorUtils.blendARGB(this.paintSubtitle.getColor(), Color.BLACK, 0.8f));

            this.maxValue = maxValue;
        }

        public void addMarker(int value, int color) {
            float[] oldMarkers = markers;
            markers = new float[markers.length + 1];
            markers[markers.length - 1] = value / maxValue;

            Paint[] oldMarkerPaints = markerPaints;
            markerPaints = new Paint[markerPaints.length + 1];
            Paint markerPaint = new Paint();
            markerPaint.setColor(color);
            markerPaint.setStyle(Paint.Style.FILL);
            markerPaints[markerPaints.length - 1] = markerPaint;

            for (int n = 0; n < oldMarkers.length; n++) {
                markers[n] = oldMarkers[n];
                markerPaints[n] = oldMarkerPaints[n];
            }
        }

        public void clear() {
            startIndex = 0;
            stopIndex = 0;
            avgCount = 0;
            avgValue = 0;
            lastValue = 0;
            XYGraphView.this.invalidate();
        }

        public int size() {
            return stopIndex - startIndex + ((stopIndex < startIndex) ? valuesNormalized.length : 0);
        }

        public void put(float value) {
            stopIndex++;
            // empty vector is a special case: stopIndex becomes 0
            // no shifting in such case
            if (stopIndex > 0) {
                // roll the stop index
                if (stopIndex >= valuesNormalized.length) {
                    stopIndex = 0;
                }

                // push the start index if the capacity reached
                if (stopIndex == startIndex) {
                    startIndex++;
                    // roll the start index
                    if (startIndex >= valuesNormalized.length) {
                        startIndex = 0;
                    }
                }
            }

            valuesNormalized[stopIndex] = value / maxValue;
            lastValue = value;
            avgValue = (float)avgCount++ / avgCount * avgValue + (float)value / avgCount;
            XYGraphView.this.invalidate();
        }

        public float getAvgValue() {
            return avgValue;
        }

        public float getLastValue() {
            return lastValue;
        }
    }
}
