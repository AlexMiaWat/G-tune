package russianapp.tools.guitar_tunings.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import russianapp.tools.guitar_tunings.R;

import static android.content.ContentValues.TAG;

public class DialView extends SurfaceView implements SurfaceHolder.Callback {

    int svw, tph;

	class DialThread extends Thread {
		
		private SurfaceHolder mSurfaceHolder;
		private boolean isRunning = false;
		private float mValue = -90;
		private float mPos = -90;
        private Paint paint;
		
		/** The background image. */
		private Bitmap mBackgroundImage;
		
		public DialThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
			mSurfaceHolder = surfaceHolder;
			mBackgroundImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.dial);
		}
		
		@Override
		public void run() {
			while(isRunning) {
				Canvas c = null;
				try {
					c = mSurfaceHolder.lockCanvas();
					synchronized (mSurfaceHolder) {
						doDraw(c);
						this.sleep(0,5);
					}
				} catch (InterruptedException e) {
					android.os.Process.killProcess(android.os.Process.myPid());
				} finally {
					if (c != null) {
						try {
							mSurfaceHolder.unlockCanvasAndPost(c);
						} catch (IllegalStateException ee) {
							android.os.Process.killProcess(android.os.Process.myPid());
						}
                    } else {
						android.os.Process.killProcess(android.os.Process.myPid());
					}
				}
			}
		}
		
		private void doDraw(Canvas canvas) {

                Paint linePaint = new Paint();
                linePaint.setAntiAlias(true);
                linePaint.setARGB(255, 200, 200, 255);
                linePaint.setStrokeWidth(6f);

                int _width = mBackgroundImage.getWidth();
                int _height = mBackgroundImage.getHeight();

                ///////////////////////////////////////
                // Draw background image
                //3:
                float scaleWidth;
                float scaleHeight;
                //
                try{
                    scaleWidth = ((float) canvas.getWidth()) / _width;
                    scaleHeight = ((float) canvas.getHeight()) / _height;
                }catch (NullPointerException e) {
                    scaleWidth = 1;
                    scaleHeight = ((float)1.0830325);
                }
                ///////////////////////////////////////

                // create a matrix for the manipulation
                Matrix matrix = new Matrix();
                // resize the bit map
                matrix.postScale(scaleWidth, scaleHeight);
                // recreate the new Bitmap
                Bitmap resizedBitmap = Bitmap.createBitmap(mBackgroundImage, 0, 0, _width, _height, matrix, true);

				try{
                    if (resizedBitmap != null) {
                        paint = new Paint();
                        canvas.drawBitmap(resizedBitmap, 0, 0, paint);

                        //1:
                        //Rect src = new Rect(0, 0, _width + svw, _height + tph);
                        //Rect dest = new Rect(0, 0, canvas.getWidth() , canvas.getHeight() );
                        //canvas.drawBitmap(mBackgroundImage, src, dest, null);

                        //0:
                        //canvas.drawBitmap(mBackgroundImage, 0, 0, null);

                        // Draw needle
                        float startX = canvas.getWidth() / 2;
                        float stopX = canvas.getWidth() / 2;
                        float startY = canvas.getHeight() * 0.99f;
                        float stopY = canvas.getHeight() * 0.12f;
                        updatePosition();
                        canvas.save();
                        //canvas.rotate(mValue, startX, startY);
                        canvas.rotate(mPos, startX, startY);
                        canvas.drawLine(startX, startY, stopX, stopY, linePaint);
                        canvas.restore();
                    }else{
                        Log.i(TAG, "Сашка - Какашка!");
                        //Thread.currentThread().interrupt();
						android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }catch (NullPointerException e) {

                    Log.i(TAG, "Сашка - Какашка!");
                    //Thread.currentThread().interrupt();
					android.os.Process.killProcess(android.os.Process.myPid());
                }
           // }
        }

		private void updatePosition() {
			if (Math.abs(mValue - mPos) > 2) {
				if (mValue > mPos)
					mPos += 2f;
				if (mValue < mPos)
					mPos -= 2f;
			} else {
				mPos = mValue;
			}
		}
		
		public void setSurfaceSize(int width, int height) {
			synchronized(mSurfaceHolder) {
				// TODO: It might be nice to store these values.
				// TODO: Center the image in the available space.
				
				/*
				 * Fit the background image to the width of the surface unless the surface
				 * width is greater than twice the height, in this case fit the background
				 * image to the height.
				 */
				if (width > height*2) {
					mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, height*2, height, true);
				} else {
					mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, width, width/2, true);
				}
			}
		}
		
		public void setRunning(boolean b) {
			isRunning = b;
		}
		
		public void setValue(float f, int sv, int tp) {
			if (f < -90)
	    		mValue = -90;
			else if (f > 90)
	    		mValue = 90;
			else
				mValue = f;

            svw = sv;
            tph = tp;
		}
	} // End DialThread class.

	private DialThread mThread;
	
	public DialView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		mThread = new DialThread(holder, context, new Handler());
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mThread.setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		try {
			if (!mThread.isRunning) {
				mThread.setRunning(true);
				mThread.start();
			}
		} catch (Exception e) {}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		boolean retry = false;
		mThread.setRunning(false);
		while (retry) {
			try {
				mThread.join();
				retry = false;
			} catch (InterruptedException e) {
				// nothing
			}
		}
	}

	public void update(float value, int _sv, int _tp) {
		mThread.setValue(value, _sv, _tp);
	}
}
