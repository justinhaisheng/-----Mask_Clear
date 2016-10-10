# -----Mask_Clear
#####前言废话
一直以来都是从事应用层的开发，也没有什么机会接触到怎么修改系统层源码怎么编译整个系统，直到现在在的公司，是从事手机研发和智能后视镜的研发才有这个机会和亲眼目睹一整套从修改底源码定制root到编译系统出来再到应用层的开发是怎么工作起来的，因为自己也蠢蠢欲动开始初次尝试这些工作，有点小激动...........,废话不说了开始，过程很痛苦.....
#####未修改前这个原生是长什么样子的

![](http://upload-images.jianshu.io/upload_images/2197978-268986d802818591.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


#####需求需要修改的样子

![](http://upload-images.jianshu.io/upload_images/2197978-1246d358bd09a8e0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#####思路
* 在应用层先实现这个dialog的效果
* 找到恢复出厂设置所在的源码位置，把我们实现好的dialog替换原生的dialog

#####在应用层实现，这里只粘贴部分代码


######自定义的progress
```java
package com.haisheng.music.circularprogress;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.haisheng.music.R;

/*
*
*@author luhaisheng  实现跟原生loading效果一样的progress  CircularProgressView
*@time 2016/10/10 11:40
*/
public class CircularProgressView extends View {

    private static final Interpolator ANGLE_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator SWEEP_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private int angleAnimatorDuration;

    private int sweepAnimatorDuration;

    private int minSweepAngle;

    private float mBorderWidth;

    private final RectF fBounds = new RectF();
    private ObjectAnimator mObjectAnimatorSweep;
    private ObjectAnimator mObjectAnimatorAngle;
    private boolean mModeAppearing = true;
    private Paint mPaint;
    private float mCurrentGlobalAngleOffset;
    private float mCurrentGlobalAngle;

    private float mCurrentSweepAngle;
    private boolean mRunning;
    private int[] mColors;
    private int mCurrentColorIndex;
    private int mNextColorIndex;

    public CircularProgressView(Context context) {
        this(context, null);
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.CircularProgressView,
                defStyleAttr, 0);

        mBorderWidth = a.getDimension(
                R.styleable.CircularProgressView_borderWidth,
                5);

        angleAnimatorDuration = a.getInt(
                R.styleable.CircularProgressView_angleAnimationDurationMillis,
                2000);

        sweepAnimatorDuration = a.getInt(
                R.styleable.CircularProgressView_sweepAnimationDurationMillis,
                900);

        minSweepAngle = a.getInt(
                R.styleable.CircularProgressView_minSweepAngle,
                30);

        int colorArrayId = a.getResourceId(R.styleable.CircularProgressView_colorSequence,
                R.array.circular_default_color_sequence);
        if (isInEditMode()) {
            mColors = new int[4];
            mColors[0] = getResources().getColor(R.color.circular_blue);
            mColors[1] = getResources().getColor(R.color.circular_blue);
            mColors[2] = getResources().getColor(R.color.circular_blue);
            mColors[3] = getResources().getColor(R.color.circular_blue);
        } else {
            mColors = getResources().getIntArray(colorArrayId);
        }
        a.recycle();

        mCurrentColorIndex = 0;
        mNextColorIndex = 1;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Cap.ROUND);
        mPaint.setStrokeWidth(mBorderWidth);
        mPaint.setColor(mColors[mCurrentColorIndex]);

        setupAnimations();
    }

    private void innerStart() {
        if (isRunning()) {
            return;
        }
        mRunning = true;
        mObjectAnimatorAngle.start();
        mObjectAnimatorSweep.start();
        invalidate();
    }

    private void innerStop() {
        if (!isRunning()) {
            return;
        }
        mRunning = false;
        mObjectAnimatorAngle.cancel();
        mObjectAnimatorSweep.cancel();
        invalidate();
    }

    private boolean isRunning() {
        return mRunning;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            innerStart();
        } else {
            innerStop();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        innerStart();
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        innerStop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fBounds.left = mBorderWidth / 2f + .5f;
        fBounds.right = w - mBorderWidth / 2f - .5f;
        fBounds.top = mBorderWidth / 2f + .5f;
        fBounds.bottom = h - mBorderWidth / 2f - .5f;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        float startAngle = mCurrentGlobalAngle - mCurrentGlobalAngleOffset;
        float sweepAngle = mCurrentSweepAngle;
        if (mModeAppearing) {
            mPaint.setColor(gradient(mColors[mCurrentColorIndex], mColors[mNextColorIndex],
                    mCurrentSweepAngle / (360 - minSweepAngle * 2)));
            sweepAngle += minSweepAngle;
        } else {
            startAngle = startAngle + sweepAngle;
            sweepAngle = 360 - sweepAngle - minSweepAngle;
        }
        canvas.drawArc(fBounds, startAngle, sweepAngle, false, mPaint);
    }

    private static int gradient(int color1, int color2, float p) {
        int r1 = (color1 & 0xff0000) >> 16;
        int g1 = (color1 & 0xff00) >> 8;
        int b1 = color1 & 0xff;
        int r2 = (color2 & 0xff0000) >> 16;
        int g2 = (color2 & 0xff00) >> 8;
        int b2 = color2 & 0xff;
        int newr = (int) (r2 * p + r1 * (1 - p));
        int newg = (int) (g2 * p + g1 * (1 - p));
        int newb = (int) (b2 * p + b1 * (1 - p));
        return Color.argb(255, newr, newg, newb);
    }

    private void toggleAppearingMode() {
        mModeAppearing = !mModeAppearing;
        if (mModeAppearing) {
            mCurrentColorIndex = ++mCurrentColorIndex % mColors.length;
            mNextColorIndex = ++mNextColorIndex % mColors.length;
            mCurrentGlobalAngleOffset = (mCurrentGlobalAngleOffset + minSweepAngle * 2) % 360;
        }
    }

    private Property<CircularProgressView, Float> mAngleProperty = new Property<CircularProgressView, Float>(Float.class, "angle") {
        @Override
        public Float get(CircularProgressView object) {
            return object.getCurrentGlobalAngle();
        }

        @Override
        public void set(CircularProgressView object, Float value) {
            object.setCurrentGlobalAngle(value);
        }
    };

    private Property<CircularProgressView, Float> mSweepProperty = new Property<CircularProgressView, Float>(Float.class, "arc") {
        @Override
        public Float get(CircularProgressView object) {
            return object.getCurrentSweepAngle();
        }

        @Override
        public void set(CircularProgressView object, Float value) {
            object.setCurrentSweepAngle(value);
        }
    };

    private void setupAnimations() {
        mObjectAnimatorAngle = ObjectAnimator.ofFloat(this, mAngleProperty, 360f);
        mObjectAnimatorAngle.setInterpolator(ANGLE_INTERPOLATOR);
        mObjectAnimatorAngle.setDuration(angleAnimatorDuration);
        mObjectAnimatorAngle.setRepeatMode(ValueAnimator.RESTART);
        mObjectAnimatorAngle.setRepeatCount(ValueAnimator.INFINITE);

        mObjectAnimatorSweep = ObjectAnimator.ofFloat(this, mSweepProperty, 360f - minSweepAngle * 2);
        mObjectAnimatorSweep.setInterpolator(SWEEP_INTERPOLATOR);
        mObjectAnimatorSweep.setDuration(sweepAnimatorDuration);
        mObjectAnimatorSweep.setRepeatMode(ValueAnimator.RESTART);
        mObjectAnimatorSweep.setRepeatCount(ValueAnimator.INFINITE);
        mObjectAnimatorSweep.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                toggleAppearingMode();
            }
        });
    }

    public void setCurrentGlobalAngle(float currentGlobalAngle) {
        mCurrentGlobalAngle = currentGlobalAngle;
        invalidate();
    }

    public float getCurrentGlobalAngle() {
        return mCurrentGlobalAngle;
    }

    public void setCurrentSweepAngle(float currentSweepAngle) {
        mCurrentSweepAngle = currentSweepAngle;
        invalidate();
    }

    public float getCurrentSweepAngle() {
        return mCurrentSweepAngle;
    }
}

```

######自定义的CustomWaitDialog
```java
package com.haisheng.music.circularprogress;

import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;

import com.haisheng.music.R;


public class CustomWaitDialog extends Dialog {
	
	private TextView nTextView;
	private CharSequence nText;

	private CustomWaitDialog(Context context) {
		this(context, 0);
	}

	private CustomWaitDialog(Context context, int theme) {
		super(context, R.style.CustomDialogHolo);

		setContentView(R.layout.custom_wait_dialog_layout);
		nTextView = (TextView) findViewById(R.id.text_off_or_reboot);

		setCancelable(false);
		setCanceledOnTouchOutside(false);
	}

	public void setText(CharSequence text) {
		nText = text;
	}

	@Override
	public void show() {

		nTextView.setText(nText);

		super.show();

	}

	public static class Builder {
		private CustomWaitDialog mDialog;

		public Builder(Context context) {
			mDialog = new CustomWaitDialog(context);
		}

		public Builder setText(CharSequence text) {
			mDialog.setText(text);
			return this;
		}

		public CustomWaitDialog build() {
			return mDialog;
		}

		public void setCanceledOnTouchOutside(boolean flag) {
			mDialog.setCanceledOnTouchOutside(flag);
		}
	}

}

```
```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
              android:layout_width="240dp"
              android:layout_height="112dp"
              android:background="@drawable/laun_po_bg"

    >

    <com.example.lu.myapplication.CircularProgressView
        android:id="@+id/pv"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:layout_centerVertical="true"
        android:layout_marginLeft="32dp"
        app:borderWidth="3.34dp"
        app:colorSequence="@array/circular_default_color_sequence"
        app:angleAnimationDurationMillis="2000"
        app:sweepAnimationDurationMillis="900"
         />

    <TextView
        android:id="@+id/text_off_or_reboot"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_toRightOf="@+id/pv"
        android:layout_marginLeft="20dp"
        android:layout_centerVertical="true"
        android:text="正在恢复出厂设置"
        android:textColor="#666666"
        android:textSize="16sp"/>

</RelativeLayout>

```
######CustomWaitDialog的简单使用
```java
new CustomWaitDialog.Builder(this).setText("正在恢复出厂设置").build().show();
```
######效果

![](http://upload-images.jianshu.io/upload_images/2197978-2e6e9b14bdbd4ab7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

######在应用层实现很简单，也能实现预期的效果，接下来查看源码恢复出厂设置的代码所在的位置，在这里我所修改的是MTK提供的android 6.0源码，其他的改动位置估计也差不多，但要实践才知道。

* 找到位置\alps\frameworks\base\services\core\java\com\android\server\power目录下的ShutdownThread.java文件

* 分析这个类的源码。这里顺便说 ，我还不知道有什么比较好的ide查看源码，所以我是用vim打开的。
1、查看beginShutdownSequence()代码
    ```java

         .....
         ......
        // Throw up a system dialog to indicate the device is rebooting / shutting down.
        ProgressDialog pd = new ProgressDialog(context);

        // Path 1: Reboot to recovery and install the update
        //   Condition: mRebootReason == REBOOT_RECOVERY and mRebootUpdate == True
        //   (mRebootUpdate is set by checking if /cache/recovery/uncrypt_file exists.)
        //   UI: progress bar
        //
        // Path 2: Reboot to recovery for factory reset
        //   Condition: mRebootReason == REBOOT_RECOVERY
        //   UI: spinning circle only (no progress bar)
        //
        // Path 3: Regular reboot / shutdown
        //   Condition: Otherwise
        //   UI: spinning circle only (no progress bar)
        if (PowerManager.REBOOT_RECOVERY.equals(mRebootReason)) {
            mRebootUpdate = new File(UNCRYPT_PACKAGE_FILE).exists();
            if (mRebootUpdate) {
                pd.setTitle(context.getText(com.android.internal.R.string.reboot_to_update_title));
                pd.setMessage(context.getText(
                        com.android.internal.R.string.reboot_to_update_prepare));
                pd.setMax(100);
                pd.setProgressNumberFormat(null);
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pd.setProgress(0);
                pd.setIndeterminate(false);
            } else {
                // Factory reset path. Set the dialog message accordingly.
                pd.setTitle(context.getText(com.android.internal.R.string.reboot_to_reset_title));
                pd.setMessage(context.getText(
                        com.android.internal.R.string.reboot_to_reset_message));
                pd.setIndeterminate(true);
            }
        } else {
            pd.setTitle(context.getText(com.android.internal.R.string.power_off));
            pd.setMessage(context.getText(com.android.internal.R.string.shutdown_progress));
            pd.setIndeterminate(true);
        }
        pd.setCancelable(false);
        pd.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);

        // start the thread that initiates shutdown
        sInstance.mContext = context;
        sInstance.mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        sInstance.mHandler = new Handler() {
        };

        beginAnimationTime = 0;
        boolean mShutOffAnimation = configShutdownAnimation(context);
        int screenTurnOffTime = getScreenTurnOffTime(context);
        synchronized (mEnableAnimatingSync) {
            if (mEnableAnimating) {
                if (mShutOffAnimation) {
                    Log.d(TAG, "mIBootAnim.isCustBootAnim() is true");
                    bootanimCust(context);
                } else {
                    pd.show();

                    sInstance.mProgressDialog = pd;
                }
                sInstance.mHandler.postDelayed(mDelayDim, screenTurnOffTime);
            }
        }
```
 分析：ProgressDialog  是系统的dialog,然后稍微往下看到.show()方法才是重点，这里说明了什么时候show出来，show出来之后又把这个dialog赋给了sInstance.mProgressDialog，接下来就要找这个sInstance.mProgressDialog，在什么时候给dismiss掉。

 2、查看running()代码
```java
......
......
            //To void previous UI flick caused by shutdown animation stopping before BKL turning off
            if (sInstance.mProgressDialog != null) {
                sInstance.mProgressDialog.dismiss();
            } else if (beginAnimationTime > 0) {
                Log.i(TAG, "service.bootanim.exit = 1");
                SystemProperties.set("service.bootanim.exit", "1");
            }
.......
.......
```
 分析：顺利找到dismiss的位置。接下来就是修改这两个位置

######开始修改ShutdownThread.java 这个类的源码
* 申明全局变量CustomWaitDialog
  ```java
      // IPO
    private static CustomWaitDialog pd = null;
   ```
* 修改 beginShutdownSequence（）方法

```java

       ......   
       ......
       //2016-9-24 luhaisheng

        // Throw up a system dialog to indicate the device is rebooting / shutting down.
       // ProgressDialog pd = new ProgressDialog(context);

        // Path 1: Reboot to recovery and install the update
        //   Condition: mRebootReason == REBOOT_RECOVERY and mRebootUpdate == True
        //   (mRebootUpdate is set by checking if /cache/recovery/uncrypt_file exists.)
        //   UI: progress bar
        //
        // Path 2: Reboot to recovery for factory reset
        //   Condition: mRebootReason == REBOOT_RECOVERY
        //   UI: spinning circle only (no progress bar)
        //
        // Path 3: Regular reboot / shutdown
        //   Condition: Otherwise
        //   UI: spinning circle only (no progress bar)
        if (PowerManager.REBOOT_RECOVERY.equals(mRebootReason)) {
            mRebootUpdate = new File(UNCRYPT_PACKAGE_FILE).exists();
            if (mRebootUpdate) {
         //       pd.setTitle(context.getText(com.android.internal.R.string.reboot_to_update_title));
          //      pd.setMessage(context.getText(
                  //      com.android.internal.R.string.reboot_to_update_prepare));
           //     pd.setMax(100);
            //    pd.setProgressNumberFormat(null);
            //    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            //    pd.setProgress(0);
             //   pd.setIndeterminate(false);
            } else {
                // Factory reset path. Set the dialog message accordingly.
             //   pd.setTitle(context.getText(com.android.internal.R.string.reboot_to_reset_title));
             //   pd.setMessage(context.getText(
              //          com.android.internal.R.string.reboot_to_reset_message));
              //  pd.setIndeterminate(true);
            }
        } else {
          //  pd.setTitle(context.getText(com.android.internal.R.string.power_off));
          //  pd.setMessage(context.getText(com.android.internal.R.string.shutdown_progress));
          //  pd.setIndeterminate(true);
        }
      //  pd.setCancelable(false);
      //  pd.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);

        // start the thread that initiates shutdown
        sInstance.mContext = context;
        sInstance.mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        sInstance.mHandler = new Handler() {
        };

        beginAnimationTime = 0;
        boolean mShutOffAnimation = configShutdownAnimation(context);
        int screenTurnOffTime = getScreenTurnOffTime(context);
        synchronized (mEnableAnimatingSync) {
            if (mEnableAnimating) {
                if (mShutOffAnimation) {
                    Log.d(TAG, "mIBootAnim.isCustBootAnim() is true");
                    bootanimCust(context);
                } else {
                     pd = new CustomWaitDialog.Builder(context).build();
                    //luhaisheng 2016/10/10
                    if(mReboot){
                    	pd.setText(context.getText(com.android.internal.R.string.custom_text_reboot_ing));
                    }else{
                    	pd.setText(context.getText(com.android.internal.R.string.custom_text_off_ing));
                    	}
                    pd.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                    /* To fix video+UI+blur flick issue */
                    pd.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    pd.show();
			
       //luhaisheng 2016/10/10
                  //  pd.show();

                  //  sInstance.mProgressDialog = pd;
                }
                sInstance.mHandler.postDelayed(mDelayDim, screenTurnOffTime);
            }
        }
```
在这里主要修改三个地方，第一个是注释掉原来pd的地方，第二个是在
```java
if(mReboot){ 
pd.setText(context.getText(com.android.internal.R.string.custom_text_reboot_ing)); 
}else{ 
pd.setText(context.getText(com.android.internal.R.string.custom_text_off_ing)); }
```
通过mReboot判断是否重启，第三个是注释掉原来的show方法。

* 修改 running()
```java
            //To void previous UI flick caused by shutdown animation stopping before BKL turning off
          //  if (sInstance.mProgressDialog != null) {
           //     sInstance.mProgressDialog.dismiss();
            //2016-09-24 luhaisheng
             if(pd!=null){
                 pd.dismiss();
                 pd=null;
            } else if (beginAnimationTime > 0) {
                Log.i(TAG, "service.bootanim.exit = 1");
                SystemProperties.set("service.bootanim.exit", "1");
            }
```
在原来的判断dimiss方法上修改。以上就是修改ShutdownThread.java所做的工作，看起来也不简单，因为要分析源码，而且不能像修改应用层那么便捷因为你还没有引用资源，自定义的类，还要编译系统，刷机才能看到最终的效果，反正继续往下做，至于为什么这么修改，有时间的同学自己研究下源码，看这么修改是否有问题，也可以有其他的修改方式。

######引入自定义类 CircularProgressView和CustomWaitDialog
 *在alps\frameworks\base\services\core\java\com\android\server\power目录下放置
CircularProgressView.java与CustomWaitDialog.java，也就是与修改的ShutdownThread.java同级。

*编辑CircularProgressView.java 修改导入的包名和引入的R。修改如下
```java
package com.android.server.power;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.android.internal.R;
.....
```
分析：就修改了两个地方，package 是com.android.server.power，和导入的R是com.android.internal.R，不然肯定出错。
同理CustomWaitDialog.java 也是这么改的。

######添加资源文件和修改资源文件(最最最麻烦的地方)
* 在alps\frameworks\base\core\res\res\drawable-hdpi目录下放置资源图片，我这里只需要放置一张

![](http://upload-images.jianshu.io/upload_images/2197978-dd52ceb7342a9a55.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

* 为添加的资源图片文件添加id(id 是自己手动添加的 没有ide 自动导入 很麻烦)，编辑alps\frameworks\base\core\res\res\values 目录下public.xml的文件，自增一个type="drawable" 的item。我的是
```xml
<public type="drawable" name="laun_po_bg"  id="0x010800b6"/>
```
* 为布局文件中的控件TextView申明id  text_off_or_reboot。编译alps\frameworks\base\core\res\res\目录下ids.xml的文件，编辑如下：
```xml
<!--2016 1009 luhaisheng-->
  <item type="id" name="text_off_or_reboot" />

  <item type="id" name="pv" />
```
所以你自己定义的控件id都需要再ids.xml下做申明，还需要在public.xml下添加id,添加如下：
```xml
      <public type="id" name="text_off_or_reboot" id="0x0102003e"/>
      <public type="id" name="pv" id="0x0102003f"/>
```
* 与添加控件的id类似，所以自定义的style,layout,string,color等，都需要在相应的目录下申明，然后在public.xml下添加id。我的public.xml如下所示：
```xml

      <public type="style" name="CustomDialogHolo" id="0x010302e0"/>
      <public type="layout" name="custom_wait_dialog_layout" id="0x01090018"/>
      <public type="id" name="text_off_or_reboot" id="0x0102003e"/>
      <public type="id" name="pv" id="0x0102003f"/>

      <public type="string" name="custom_text_off_ing" id="0x01040019"/>
      <public type="string" name="custom_text_reboot_ing" id="0x01040020"/>
      <public type="drawable" name="laun_po_bg"  id="0x010800b6"/>
     
      <public type="attr" name="borderWidth" id="0x010104f2" />
       <public type="attr" name="colorSequence" id="0x010104f3" />
       <public type="attr" name="sweepAnimationDurationMillis" id="0x010104f4" />
       <public type="attr" name="angleAnimationDurationMillis" id="0x010104f5" />
        <public type="attr" name="minSweepAngle" id="0x010104f6" />

      <public type="array" name="circular_default_color_sequence" id="0x01070006" />
      
       <public type="color" name="circular_blue" id="0x0106001c" />
```
#####等资源添加确定无误后，其实谁也不能确定，那么就开执行编译吧，因为出错看错就好了，像这种一般都是添加资源时候漏了申明某个id,或者id冲突之类的，我就错了好几十次了，保存好所有修改，后在framework/base/core/res/res/ 下mm编译一次，无误后在跟目录下make update-api，update成功之后就可以编译整个系统了。
   * 在跟目录下执行 source build/envsetup.sh
   * lunch 找到与你硬件相对于的版本
   * make -j8 开始编译整个系统　

#####这个过程下来从update到编译这个系统我的计算机花了差不多两个小时，从中还有不少的错误，一直到正确出来刷机花了一天的时间，所以搞这方面的真的很需要耐心。至于系统怎么编译，这个不是我说所的重点。不懂的请自己google。接下来我传上，我修改源码的所以涉及到的文件和资源到github。https://github.com/justinhaisheng/-----Mask_Clear

  

















