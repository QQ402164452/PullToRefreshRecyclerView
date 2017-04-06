package com.example.jason.pulltorefreshrecyclerview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Jason on 2017/4/2.
 */

/**
 * RecyclerView刷新头部
 */
public class RefreshHeader extends LinearLayout implements BaseRefreshHeader {
    private RelativeLayout container;//内部容器
    private ImageView arrowImageView;//指示箭头
    private TextView hintTextView;//提示文字
    private ProgressBar progressBar;//进度提示

    private Animation rotateDownAnim;//向下旋转动画
    private Animation rotateUpAnim;//向上旋转动画

    private final int ROTATE_ANIM_DURATION=200;//动画持续时间
    private int mState=STATE_NORMAL;//当前头部状态
    private int measuredHeight;//刷新头部的高度

    private Handler handler;

    public RefreshHeader(Context context) {
        this(context,null);
    }

    public RefreshHeader(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RefreshHeader(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){//初始化
        container= (RelativeLayout) LayoutInflater.from(getContext()).inflate(R.layout.refresh_header,null);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(container,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                0));

        arrowImageView= (ImageView) container.findViewById(R.id.PullToRefresh_Header_ArrowImageView);
        hintTextView= (TextView) container.findViewById(R.id.PullToRefresh_Header_HintTextView);
        progressBar= (ProgressBar) container.findViewById(R.id.PullToRefresh_Header_ProgressBar);

        rotateUpAnim=new RotateAnimation(0.0f,-180.0f,
                Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);//RELATIVE_TO_SELF是相对
        rotateUpAnim.setDuration(ROTATE_ANIM_DURATION);
        rotateUpAnim.setFillAfter(true);

        rotateDownAnim=new RotateAnimation(-180.0f,0.0f,
                Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        rotateDownAnim.setDuration(ROTATE_ANIM_DURATION);
        rotateDownAnim.setFillAfter(true);

        measure(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);//调用measure进行测量
        measuredHeight=getMeasuredHeight();//获取刷新头部的标准高度

        handler=new Handler();
    }

    @Override
    public void onMove(float delta) {//主要方法 调用此方法进行刷新头部高度的变化
            if(getVisibleHeight()>0||delta>0){
                setVisibleHeight((int)delta+getVisibleHeight());//改变刷新头部的高度
                if(mState<=STATE_RELEASE){
                    if(getVisibleHeight()>measuredHeight){
                        onStateChange(STATE_RELEASE);//如果下拉高度 大于 标准高度，将状态设置为 释放刷新
                    }else{
                        onStateChange(STATE_NORMAL);//如果下拉高度 小于 标准高度，将状态设置为 普通状态
                    }
                }
            }
    }

    @Override
    public void onComplete() {//下拉刷新完成
        onStateChange(STATE_COMPLETE);//将状态设置为 刷新完成
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                reset();//延迟进行重置
            }
        },200);
    }

    @Override
    public boolean onRelease() {//手指离开屏幕 即进行释放时，触发此方法
        boolean isOnRefresh=false;//标志位，判断是否是在刷新触发用户的接口回调事件

        int height=getVisibleHeight();
        if(height==0){
            isOnRefresh=false;
        }

        if(height>=measuredHeight&&mState==STATE_RELEASE){//如果下拉高度大于标准高度，并且是释放刷新状态
            onStateChange(STATE_REFRESHING);//将状态设置为刷新状态
            isOnRefresh=true;
        }

        if(mState!=STATE_REFRESHING){//如果手指释放时，不是正在刷新状态，将头部高度设置为0
            smoothScrollTo(0);
        }

        if(mState==STATE_REFRESHING){//如果手指释放时，是正在刷新状态，将头部高度设置为标准高度
            smoothScrollTo(measuredHeight);
        }
        return isOnRefresh;
    }

    @Override
    public void onStateChange(int state) {//状态改变时触发此方法
        if(mState==state){//注意state是最新状态，mState是上一次的状态
            return;
        }

        switch (state){
            case STATE_NORMAL:
                arrowImageView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                hintTextView.setText(R.string.PullToRefresh_Header_Hint_Normal);

//                if(mState==STATE_REFRESHING){//当由正在刷新状态转变为普通状态时
//                    arrowImageView.clearAnimation();
//                }
                if(mState==STATE_RELEASE){//当从滑动释放状态转变为普通状态时
                    arrowImageView.clearAnimation();
                    arrowImageView.startAnimation(rotateDownAnim);//将箭头转向下
                }
                break;
            case STATE_RELEASE:
                arrowImageView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                if(mState==STATE_NORMAL){//从普通状态转变为滑动释放状态
                    arrowImageView.clearAnimation();
                    arrowImageView.startAnimation(rotateUpAnim);//将箭头转向上
                }
                hintTextView.setText(R.string.PullToRefresh_Header_Hint_Release);
                break;
            case STATE_REFRESHING:
                arrowImageView.clearAnimation();
                arrowImageView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                smoothScrollTo(measuredHeight);//将头部高度设置为标准高度
                hintTextView.setText(R.string.PullToRefresh_Header_Hint_Refreshing);
                break;
            case STATE_COMPLETE:
                arrowImageView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                hintTextView.setText(R.string.PullToRefresh_Header_Hint_Complete);
                break;
        }
        mState=state;
    }

    private void smoothScrollTo(int destHeight){//线性动画 改变 刷新头部的高度
        ValueAnimator animator=ValueAnimator.ofInt(getVisibleHeight(),destHeight);//使用属性动画
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {//在动画运行中监听动画数值的改变
                setVisibleHeight((int)animation.getAnimatedValue());//用动画线性改变的数值  动态改变 刷新头部的高度
            }
        });
        animator.setDuration(300).start();
    }

    public int getVisibleHeight(){
        return container.getLayoutParams().height;//获取刷新头部的实时高度
    }

    public void setVisibleHeight(int height){//实时改变刷新头部的实时高度
        if(height<0){
            height=0;
        }
        LinearLayout.LayoutParams params= (LayoutParams) container.getLayoutParams();
        params.height=height;
        container.setLayoutParams(params);
    }

    public void reset(){//重置刷新头部的状态 将刷新头部的状态重置为隐藏
        smoothScrollTo(0);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onStateChange(STATE_NORMAL);
            }
        },500);
    }

    public int getState(){
        return mState;
    }
}
