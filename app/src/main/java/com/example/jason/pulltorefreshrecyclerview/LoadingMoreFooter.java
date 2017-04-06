package com.example.jason.pulltorefreshrecyclerview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by Jason on 2017/4/5.
 */

/**
 * 加载更多底部
 */
public class LoadingMoreFooter extends LinearLayout {
    private LinearLayout container;//内部容器
    private TextView hintTextView;//加载更多提示文字
    private ProgressBar progressBar;//加载更多进度条

    public static final int STATE_LOADING=0;//标志正在加载中
    public static final int STATE_COMPLETE=1;//标志加载完成
    public static final int STATE_NOMORE=2;//标志没有更多内容

    private int measureHeight;//表示底部UI布局高度

    public LoadingMoreFooter(Context context) {
        this(context,null);
    }

    public LoadingMoreFooter(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LoadingMoreFooter(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        container= (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.loadingmore_footer,null);//初始化容器视图
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(container,
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        hintTextView= (TextView) container.findViewById(R.id.LoadingMoreFooter_HintTextView);
        progressBar= (ProgressBar) container.findViewById(R.id.LoadingMoreFooter_ProgressBar);

        measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);//调用measure测试底部视图
        measureHeight=getMeasuredHeight();//获取底部视图高度
    }

    public void onStateChange(int state){
        switch (state){
            case STATE_LOADING:
                progressBar.setVisibility(View.VISIBLE);
                hintTextView.setText(R.string.LoadingMore_Footer_Hint_Loading);
                break;
            case STATE_COMPLETE:
                progressBar.setVisibility(View.GONE);
                hintTextView.setText(R.string.LoadingMore_Footer_Hint_Complete);
                break;
            case STATE_NOMORE:
                progressBar.setVisibility(View.GONE);
                hintTextView.setText(R.string.LoadingMore_Footer_Hint_NoMore);
                break;
        }
    }

    public int getMeasureHeight() {
        return measureHeight;
    }
}
