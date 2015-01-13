package com.srbs.wearabletest.wear;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by srbs on 1/7/15.
 */
public class DrawingCanvas extends View implements OnTouchListener,
        PathModelListener {
    private PathModel pathModel;
    private Paint paint = new Paint();
    private Paint touchCirclePaint = new Paint();
    private Paint guideLinePaint = new Paint();
    private OCRActivity currentActivity;
    private boolean showGuide = false;

    public DrawingCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.BLACK);
        touchCirclePaint.setColor(Color.BLACK);
        touchCirclePaint.setStyle(Paint.Style.FILL);
        touchCirclePaint.setStrokeWidth(3);
        guideLinePaint.setColor(Color.BLACK);
        guideLinePaint.setStyle(Paint.Style.STROKE);
        guideLinePaint.setPathEffect(new DashPathEffect(new float[]{2, 5}, 0));

        setBackgroundColor(Color.WHITE);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setHapticFeedbackEnabled(true);
        setLongClickable(true);
        setOnTouchListener(this);
    }

    @Override
    public void onDraw(Canvas canvas) {
        initModelIfNeeded();
        canvas.drawLines(pathModel.getPathArray(), paint);
        Point pointToHighlight = null;
        if (pathModel.currentRecommendedPoint != null) {
            pointToHighlight = pathModel.currentRecommendedPoint;
        } else if (pathModel.currentPoint != null) {
            pointToHighlight = pathModel.currentPoint;
        }
        if (pointToHighlight != null) {
            highlightPoint(canvas, pointToHighlight);
            // Draw guide lines
            if (showGuide) {
                Path path = new Path();
                path.moveTo(0, pointToHighlight.y);
                path.lineTo(canvas.getWidth(), pointToHighlight.y);
                path.moveTo(pointToHighlight.x, 0);
                path.lineTo(pointToHighlight.x, canvas.getHeight());
                path.lineTo(canvas.getWidth(), pointToHighlight.y);
                canvas.drawPath(path, guideLinePaint);
            }
        }
    }

    private void highlightPoint(Canvas canvas, Point point) {
        int radius = currentActivity.getResources().getInteger(R.integer.touch_circle_radius);
        canvas.drawCircle(point.x, point.y, radius, touchCirclePaint);
    }

    public boolean onTouch(View view, MotionEvent event) {
        initModelIfNeeded();
        int action = event.getActionMasked();
        Point point = new Point(event.getX(), event.getY());

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            currentActivity.recognize(pathModel.points);
        } else if(action == MotionEvent.ACTION_DOWN) {
            // Typing new character. Clear existing path
            pathModel.clear();
        } else {
            pathModel.addPoint(point);
        }
        invalidate();
        return true;
    }

    public void onPointAdded() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    /**
     * TODO(saurabh):
     * This method is a hack because the activity is not available
     * in the constructor. Find a way to detect when activity is
     * ready.
     */
    private void initModelIfNeeded() {
        if (pathModel != null) {
            return;
        }
        currentActivity = (OCRActivity) getContext();
        pathModel = new PathModel(currentActivity);
        pathModel.onPointAdded(this);
    }
}

interface PathModelListener {
    void onPointAdded();
}

/**
 *
 * @author saurabh
 *
 * This model listens for new points of touch and
 * adds them to the path when the point of touch
 * does not vary for too long.
 */
class PathModel {
    static final String LOGGER_TAG = "PathModel";
    private long MAX_ALLOWED_DIST = 10;
    private long TIMER_WAIT_MILLISECONDS = 300;

    private final String timerWaitPrefKey;
    private final String distThresholdPrefKey;
    private final int timerWaitDefault;
    private final int distThresholdDefault;

    private OCRActivity currentActivity;
    private Timer timer;
    private ArrayList<PathModelListener> listeners =
            new ArrayList<PathModelListener> ();
    // Required because anonymous listeners for SharedPreferences get
    // garbage collected.
    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    TIMER_WAIT_MILLISECONDS = sharedPreferences.getLong(timerWaitPrefKey,
                            timerWaitDefault);
                    MAX_ALLOWED_DIST = sharedPreferences.getLong(distThresholdPrefKey,
                            distThresholdDefault);
                }
            };

    public List<Point> points = new ArrayList<Point>();
    public Point currentPoint;
    public Point currentRecommendedPoint;

    PathModel(OCRActivity activity) {
        currentActivity = activity;
        timerWaitPrefKey = currentActivity.getResources().getString(R.string.timer_wait_preference_key);
        distThresholdPrefKey = currentActivity.getResources().getString(R.string.dist_threshold_preference_key);
        timerWaitDefault = currentActivity.getResources().getInteger(R.integer.timer_wait_default);
        distThresholdDefault = currentActivity.getResources().getInteger(R.integer.dist_threshold_default);
        currentActivity.prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);
    }

    public void onPointAdded(PathModelListener listener) {
        listeners.add(listener);
    }

    public void addPoint(Point p) {
        currentPoint = p;
        points.add(p);
    }

    /**
     * Returns flattened list of path points which can
     * be directly rendered on the canvas.
     */
    public float[] getPathArray() {
        if (points.isEmpty()) {
            return new float[0];
        }
        int size = 0;
        if (currentPoint == null && currentRecommendedPoint == null) {
            size = points.size()*4;
        } else {
            size = (points.size() + 1)*4;
        }
        float[] arr = new float[size];
        int index = 0;
        for(; index < points.size() - 1; index++) {
            arr[index*4 + 0] = points.get(index).x;
            arr[index*4 + 1] = points.get(index).y;
            arr[index*4 + 2] = points.get(index + 1).x;
            arr[index*4 + 3] = points.get(index + 1).y;
        }
        if (currentRecommendedPoint != null) {
            arr[index*4 + 0] = points.get(index).x;
            arr[index*4 + 1] = points.get(index).y;
            arr[index*4 + 2] = currentRecommendedPoint.x;
            arr[index*4 + 3] = currentRecommendedPoint.y;
        } else if (currentPoint != null) {
            arr[index*4 + 0] = points.get(index).x;
            arr[index*4 + 1] = points.get(index).y;
            arr[index*4 + 2] = currentPoint.x;
            arr[index*4 + 3] = currentPoint.y;
        }
        return arr;
    }

    private void resetTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                if (currentRecommendedPoint != null) {
                    points.add(currentRecommendedPoint);
                    for(PathModelListener listener: listeners) {
                        listener.onPointAdded();
                    }
                    Log.i("Adding point", "Adding point");
                }
            }
        }, TIMER_WAIT_MILLISECONDS);
    }

    void clear() {
        points.clear();
        currentPoint = null;
        currentRecommendedPoint = null;
    }
}

class Point {
    float x, y;
    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }
    public Point() {
        x = y = 0;
    }
    double distance(Point target) {
        double x2 = (target.x-x)*(target.x-x);
        double y2 = (target.y-y)*(target.y-y);
        return Math.sqrt(x2 + y2);
    }
}
