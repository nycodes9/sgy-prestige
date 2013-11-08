package com.prestige;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: greg
 * Date: 6/26/13
 * Time: 10:12 AM
 */
public class DrawingView extends View {

    private Paint paint;
    private int lastX;
    private int lastY;
    private Canvas buffer;
    private Bitmap bitmap;
    private Paint bitmapPaint;
    private Firebase ref;
    private ChildEventListener listener;
    private int currentColor = 0xFFFF0000;
    private Path path;
    private Set<String> outstandingSegments;
    private Segment currentSegment;
    private Path childPath = new Path();
    
    private static final String TAG = DrawingView.class.getSimpleName();

    public DrawingView(Context context, Firebase ref) {
        super(context);

        outstandingSegments = new HashSet<String>();
        path = new Path();
        this.ref = ref;

        listener = ref.addChildEventListener(new ChildEventListener() {
            /**
             *
             * @param dataSnapshot The data we need to construct a new Segment
             * @param previousChildName Supplied for ordering, but we don't really care about ordering in this app
             */
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
            	
            	Log.d(TAG, "onChildAdded : ");
            	
                String name = dataSnapshot.getName();
                // To prevent lag, we draw our own segments as they are created. As a result, we need to check to make
                // sure this event is a segment drawn by another user before we draw it
                if (!outstandingSegments.contains(name)) {
                    // Deserialize the data into our Segment class
                    Segment segment = dataSnapshot.getValue(Segment.class);
                    drawSegment(segment, paintFromColor(segment.getColor()));
                    // Tell the view to redraw itself
                    invalidate();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildChanged : ");
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            	Log.d(TAG, "onChildRemoved : ");
            	cleanCanvas();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            	Log.d(TAG, "onChildMoved : ");
            }

            @Override
            public void onCancelled() {
            	Log.d(TAG, "onCancelled : ");
            }
        });


        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(0xFFFF0000);
        paint.setStyle(Paint.Style.STROKE);

		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(12);
        
        bitmapPaint = new Paint(Paint.DITHER_FLAG);
    }

    public void cleanup() {
        ref.removeEventListener(listener);
    }

    public void cleanCanvas(){
    	ref.setValue(null);
    	
    	buffer.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    	invalidate();
    }
    
    public void setColor(int color) {
        currentColor = color;
        paint.setColor(color);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        buffer = new Canvas(bitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFFFFFFF);

        canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);

        canvas.drawPath(path, paint);
    }

    private Paint paintFromColor(int color) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setDither(true);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
		p.setStrokeCap(Paint.Cap.ROUND);
		p.setStrokeWidth(12);
        return p;
    }

    private void drawSegment(Segment segment, Paint p) {
        childPath.reset();
        List<Point> points = segment.getPoints();
        Point current = points.get(0);
        childPath.moveTo(current.x, current.y);
        Point next = null;
        for (int i = 1; i < points.size(); ++i) {
            next = points.get(i);
            childPath.quadTo(current.x, current.y, ((next.x + current.x)) / 2, ((next.y + current.y)) / 2);
            current = next;
        }
        if (next != null) {
            childPath.lineTo(next.x, next.y);
        }
        buffer.drawPath(childPath, p);
    }

    private void onTouchStart(float x, float y) {
        path.reset();
        path.moveTo(x, y);
        currentSegment = new Segment(currentColor);
        lastX = (int)x ;
        lastY = (int)y ;
        currentSegment.addPoint(lastX, lastY);
    }

    private void onTouchMove(float x, float y) {

        int x1 = (int)x ;
        int y1 = (int)y ;

        float dx = Math.abs(x1 - lastX);
        float dy = Math.abs(y1 - lastY);
        if (dx >= 1 || dy >= 1) {
            path.quadTo(lastX, lastY, ((x1 + lastX)) / 2, ((y1 + lastY)) /2);
            lastX = x1;
            lastY = y1;
            currentSegment.addPoint(lastX, lastY);
        }
    }

    private void onTouchEnd() {
        path.lineTo(lastX, lastY);
        buffer.drawPath(path, paint);
        path.reset();
        Firebase segmentRef = ref.push();
        final String segmentName = segmentRef.getName();
        outstandingSegments.add(segmentName);
        // Save our segment into Firebase. This will let other clients see the data and add it to their own canvases.
        // Also make a note of the outstanding segment name so we don't do a duplicate draw in our onChildAdded callback.
        // We can remove the name from outstandingSegments once the completion listener is triggered, since we will have
        // received the child added event by then.
        segmentRef.setValue(currentSegment, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError error, Firebase ref) {
                outstandingSegments.remove(segmentName);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                onTouchEnd();
                invalidate();
                break;
        }
        return true;
    }

}
