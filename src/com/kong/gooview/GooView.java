package com.kong.gooview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

public class GooView extends View{
	private Paint paint;
	public GooView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public GooView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public GooView(Context context) {
		super(context);
		init();
	}
	
	private void init(){
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);//设置抗锯齿
		paint.setColor(Color.RED);
	}
	private float dragRadius = 12f;//拖拽圆的半径
	private float stickyRadius = 12f;//固定圆的半径
	private PointF dragCenter = new PointF(100f, 120f);//拖拽圆的圆心
	private PointF stickyCenter = new PointF(150f, 120f);//固定圆的圆心
	
	private PointF[] stickyPoint = {new PointF(150f, 108f),new PointF(150f, 132f)};
	private PointF[] dragPoint = {new PointF(100f, 108f),new PointF(100f, 132f)};
	
	private PointF controlPoint = new PointF(125f, 120f);
	private double lineK;//斜率
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//整体画布往上偏移
		canvas.translate(0, -Utils.getStatusBarHeight(getResources()));
		
		stickyRadius = getStickyRadius();
		
		float xOffset = dragCenter.x - stickyCenter.x;
		float yOffset = dragCenter.y - stickyCenter.y;
		if(xOffset!=0){
			lineK = yOffset/xOffset;
		}
		dragPoint = GeometryUtil.getIntersectionPoints(dragCenter, dragRadius, lineK);
		stickyPoint = GeometryUtil.getIntersectionPoints(stickyCenter, stickyRadius, lineK);
		
		//动态计算控制点
		controlPoint = GeometryUtil.getPointByPercent(dragCenter, stickyCenter, 0.618f);
		
		canvas.drawCircle(dragCenter.x, dragCenter.y, dragRadius, paint);//绘制拖拽圆
		
		if(!isDragOutOfRange){
			canvas.drawCircle(stickyCenter.x, stickyCenter.y, stickyRadius, paint);//绘制固定圆
			Path path = new Path();
			path.moveTo(stickyPoint[0].x, stickyPoint[0].y);//设置起点
			path.quadTo(controlPoint.x, controlPoint.y, dragPoint[0].x, dragPoint[0].y);//使用贝塞尔曲线连接
			path.lineTo(dragPoint[1].x, dragPoint[1].y);
			path.quadTo(controlPoint.x, controlPoint.y, stickyPoint[1].x, stickyPoint[1].y);
			canvas.drawPath(path, paint);
		}
		
		paint.setStyle(Style.STROKE);
		canvas.drawCircle(stickyCenter.x, stickyCenter.y, maxDistance, paint);
		paint.setStyle(Style.FILL);
	}
	
	private float maxDistance = 80;
	/**
	 * 动态求出固定圆的半径
	 */
	private float getStickyRadius(){
		float radius;
		float centerDistance = GeometryUtil.getDistanceBetween2Points(dragCenter, stickyCenter);
		float fraction = centerDistance/maxDistance;//圆心距离占总距离的百分比
		radius = GeometryUtil.evaluateValue(fraction, 12f, 4f);
		return radius;
	}
	
	private boolean isDragOutOfRange = false;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			isDragOutOfRange = false;
			dragCenter.set(event.getRawX(), event.getRawY());
			break;
		case MotionEvent.ACTION_MOVE:
			dragCenter.set(event.getRawX(), event.getRawY());
			
			if(GeometryUtil.getDistanceBetween2Points(dragCenter, stickyCenter)>maxDistance){
				isDragOutOfRange = true;
			}
			
			break;
		case MotionEvent.ACTION_UP:
			if(GeometryUtil.getDistanceBetween2Points(dragCenter, stickyCenter)>maxDistance){
				dragCenter.set(stickyCenter.x, stickyCenter.y);
			}else {
				if(isDragOutOfRange){
					dragCenter.set(stickyCenter.x, stickyCenter.y);
				}else {
					ValueAnimator valueAnimator = ObjectAnimator.ofFloat(1);
					final PointF startPointF = new PointF(dragCenter.x, dragCenter.y);
					valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
						@Override
						public void onAnimationUpdate(ValueAnimator animator) {
							float animatedFraction = animator.getAnimatedFraction();
							Log.e("tag", "animatedFraction: "+animatedFraction);
							PointF pointF = GeometryUtil.getPointByPercent(startPointF, stickyCenter, animatedFraction);
							dragCenter.set(pointF);
							invalidate();
						}
					});
					valueAnimator.setDuration(500);
					valueAnimator.setInterpolator(new OvershootInterpolator(3));
					valueAnimator.start();
				}
			}
			break;
		}
		invalidate();
		return true;
	}

}
