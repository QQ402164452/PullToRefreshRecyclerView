package com.example.jason.pulltorefreshrecyclerview;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Jason on 2017/4/3.
 */

public class PullToRefreshRecyclerView  extends RecyclerView{
    private boolean pullToRefreshEnabled=true;//下拉刷新 开关
    private boolean loadingMoreEnabled=true;//上拉刷新 开关
    private boolean isLoadingMore=false;//是否正在加载更多
    private boolean isNoMore=false;//是否有更多内容

    private static final int TYPE_REFRESH_HEADER=10000;//刷新头部 类型标号
    private static final int TYPE_LOADINGMORE_FOOTER=10001;//加载更多底部 类型标号
    private static final float DRAG_RATE=3;//拖动阻力系数
    private int TIMEOUT=30000;//刷新超时时间 如果超过这个时间还没刷新完成就会关闭刷新并提示刷新过程中出现问题
    private int DEFAULT_DURATION = 500;//底部超时上滑动画时间

    private WrapAdapter mWrapAdapter;//内部Adapter
    private RefreshHeader refreshHeader;//刷新头部
    private LoadingMoreFooter loadingMoreFooter;//加载更多底部
    private AdapterDataObserver dataObserver;//数据监听器

    private float lastY=-1;

    private OnRefreshListener onRefreshListener;

    private Handler handler;

    public PullToRefreshRecyclerView(Context context) {
        this(context,null);
    }

    public PullToRefreshRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public PullToRefreshRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        dataObserver=new DataObserver();
        handler=new Handler();
        if(pullToRefreshEnabled){
            refreshHeader=new RefreshHeader(getContext());//获取刷新头部
        }
        if(loadingMoreEnabled){
            loadingMoreFooter=new LoadingMoreFooter(getContext());//获取加载更多底部
        }
    }

    public void reset(){
        refreshComplete();
        loadingMoreComplete();
    }

    private boolean isOnTop(){//判断刷新头部是否可见
        if(refreshHeader.getParent()!=null){//当刷新头部可见时，getParent()获取到的值不为null
            return true;
        }else{
            return false;
        }
    }

    public void refreshComplete(){
        handler.removeCallbacksAndMessages(null);
        refreshHeader.onComplete();
        setNoMore(false);
    }

    public void loadingMoreComplete(){
        handler.removeCallbacksAndMessages(null);
        isLoadingMore=false;
        loadingMoreFooter.onStateChange(LoadingMoreFooter.STATE_COMPLETE);
    }

    public void setNoMore(boolean noMore){
        handler.removeCallbacksAndMessages(null);
        isLoadingMore=false;
        isNoMore=noMore;
        loadingMoreFooter.onStateChange(noMore?LoadingMoreFooter.STATE_NOMORE:LoadingMoreFooter.STATE_LOADING);
    }

    @Override
    public void setAdapter(Adapter adapter){
        mWrapAdapter=new WrapAdapter(adapter);//使用内部Adapter包装用户的Adapter
        super.setAdapter(mWrapAdapter);
        adapter.registerAdapterDataObserver(dataObserver);//注册Adapter数据监听器
        dataObserver.onChanged();
    }

    @Override
    public Adapter getAdapter(){
        if(mWrapAdapter!=null){
            return mWrapAdapter.getOriginalAdapter();//获取内部包装的用户的Adapter
        }else{
            return null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev){//重点 监听触屏事件来触发头部的状态和实时高度
        if(lastY==-1){
            lastY=ev.getRawY();
        }
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                lastY=ev.getRawY();//获取点击的Y轴位置
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY=ev.getRawY()-lastY;//获取滑动的距离高度
                lastY=ev.getRawY();//获取点击的Y轴位置
                if(isOnTop()&&pullToRefreshEnabled){
                    refreshHeader.onMove(deltaY/DRAG_RATE);//实时改变刷新头部的高度
                    if(refreshHeader.getVisibleHeight()>0&&refreshHeader.getState()<=RefreshHeader.STATE_RELEASE){
                        return true;
                    }
                }
                break;
            default://手指离开屏幕 释放状态
                lastY=-1;
                if(isOnTop()&&pullToRefreshEnabled){
                    if(refreshHeader.onRelease()){//判断手指离开屏幕时的状态，决定是否调用用户监听器
                        if(onRefreshListener!=null){
                            onRefreshListener.onRefresh();

                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(),"刷新超时",Toast.LENGTH_SHORT).show();
                                    onRefreshListener.onRefreshTimeOut();
                                    reset();
                                }
                            },TIMEOUT);
                        }
                    }
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public void onScrollStateChanged(int state){//列表滚动监听  上拉加载更多底部就是通过监听滚动来判断是否触发事件的
        super.onScrollStateChanged(state);
        if(state==RecyclerView.SCROLL_STATE_IDLE&&//当列表滚动停止时
                onRefreshListener!=null&&//有设置监听回调接口
                !isLoadingMore&&//当前不是正在加载更多
                loadingMoreEnabled){//加载更多功能开启
            LayoutManager layoutManager=getLayoutManager();//获取布局管理器
            int lastVisibleItemPosition;//当前列表最后一项可见项 需要根据不同的布局管理器来获取
            if(layoutManager instanceof GridLayoutManager){
                lastVisibleItemPosition=((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
            }else if(layoutManager instanceof StaggeredGridLayoutManager){
                int[] into=new int[((StaggeredGridLayoutManager)layoutManager).getSpanCount()];//span是一行内被分成几列 into是一个数组 因为StaggeredGrid每一行的列数都是不一样的
                ((StaggeredGridLayoutManager)layoutManager).findLastVisibleItemPositions(into);
                lastVisibleItemPosition=findMaxSpan(into);
            }else{
                lastVisibleItemPosition=((LinearLayoutManager)layoutManager).findLastVisibleItemPosition();
            }

            if(layoutManager.getChildCount()>0&&//childView是列表中的可见项item
                    lastVisibleItemPosition>=layoutManager.getItemCount()-1&&
                    layoutManager.getItemCount()>=layoutManager.getChildCount()&&
                    !isNoMore&&//上拉可以加载内容
                    refreshHeader.getState()<RefreshHeader.STATE_REFRESHING){//刷新头部当前不是正在刷新
                isLoadingMore=true;
                loadingMoreFooter.onStateChange(LoadingMoreFooter.STATE_LOADING);
                onRefreshListener.onLoadMore();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {//超时事件处理
                        smoothScrollBy(0,-loadingMoreFooter.getMeasureHeight());
                        Toast.makeText(getContext(),"加载更多超时",Toast.LENGTH_SHORT).show();
                        onRefreshListener.onLoadMoreTimeOut();
                        handler.postDelayed(new Runnable() {//防止在滚动完成后 触发loadMore事件 必须等滚动动画结束后才设置isLoadingMore
                            @Override
                            public void run() {
                                isLoadingMore=false;
                            }
                        },DEFAULT_DURATION);
                    }
                },TIMEOUT);
            }
        }
    }

    private int findMaxSpan(int[] lastPositions){
        int max=lastPositions[0];
        for(int value:lastPositions){
            if(value>max){
                max=value;
            }
        }
        return max;
    }

    private class WrapAdapter extends Adapter<ViewHolder>{//内部Adapter  使用装饰者模式包装用户传进来的Adapter
        private Adapter adapter;


        private WrapAdapter(Adapter adapter){
            this.adapter=adapter;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType==TYPE_REFRESH_HEADER){//刷新头部类型
                return new SimpleViewHolder(refreshHeader);
            }else if(viewType==TYPE_LOADINGMORE_FOOTER){
                return new SimpleViewHolder(loadingMoreFooter);
            }
            return adapter.onCreateViewHolder(parent,viewType);//其它为自定义Adapter里面的Item类型
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if(isRefreshHeader(position)||isLoadingMoreFooter(position)){
                return;
            }
            int adjPosition=position-1;//减去刷新头部Item的数量1
            if(adapter!=null){
                if(adjPosition<adapter.getItemCount()){
                    adapter.onBindViewHolder(holder,adjPosition);
                }
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads){
            if(isRefreshHeader(position)||isLoadingMoreFooter(position)){
                return;
            }
            int adjPosition=position-1;//减去刷新头部Item的数量1
            if(adapter!=null){
                if(adjPosition<adapter.getItemCount()){
                    adapter.onBindViewHolder(holder,adjPosition,payloads);
                }
            }
        }

        @Override
        public int getItemCount() {
            if(loadingMoreEnabled){
                if(adapter!=null){
                    return adapter.getItemCount()+2;
                }else{
                    return 2;
                }
            }else{
                if(adapter!=null){
                    return adapter.getItemCount()+1;
                }else{
                    return 1;
                }
            }
        }

        @Override
        public int getItemViewType(int position){
            int adjPosition=position-1;//减去刷新头部Item的数量1
            if(isRefreshHeader(position)){
                return TYPE_REFRESH_HEADER;
            }
            if(isLoadingMoreFooter(position)){
                return TYPE_LOADINGMORE_FOOTER;
            }
            if(adapter!=null){
                if(adjPosition<adapter.getItemCount()){
                    return adapter.getItemViewType(adjPosition);
                }
            }
            return 0;
        }

        @Override
        public long getItemId(int position){
            if(adapter!=null&&position>=1){
                int adjPosition=position-1;//减去刷新头部Item的数量1
                if(adjPosition<adapter.getItemCount()){
                    return adapter.getItemId(adjPosition);
                }
            }
            return -1;
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView){
            super.onAttachedToRecyclerView(recyclerView);
            LayoutManager manager=getLayoutManager();
            if(manager instanceof GridLayoutManager){
                final GridLayoutManager gridLayoutManager= (GridLayoutManager) manager;
                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return isRefreshHeader(position)?gridLayoutManager.getSpanCount():1;
                    }
                });
            }
            if(adapter!=null){
                adapter.onAttachedToRecyclerView(recyclerView);
            }
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView){
            if(adapter!=null){
                adapter.onDetachedFromRecyclerView(recyclerView);
            }
        }

        @Override
        public void onViewAttachedToWindow(ViewHolder holder){
            super.onViewAttachedToWindow(holder);
            ViewGroup.LayoutParams layoutParams=holder.itemView.getLayoutParams();
            if(layoutParams!=null&&
                    layoutParams instanceof StaggeredGridLayoutManager.LayoutParams&&
                    isRefreshHeader(holder.getLayoutPosition())){
                StaggeredGridLayoutManager.LayoutParams params= (StaggeredGridLayoutManager.LayoutParams) layoutParams;
                params.setFullSpan(true);
            }
            if(adapter!=null){
                adapter.onViewAttachedToWindow(holder);
            }
        }

        @Override
        public void onViewDetachedFromWindow(ViewHolder holder){
            if(adapter!=null){
                adapter.onViewDetachedFromWindow(holder);
            }
        }

        @Override
        public void onViewRecycled(ViewHolder holder){
            if(adapter!=null){
                adapter.onViewRecycled(holder);
            }
        }

        @Override
        public boolean onFailedToRecycleView(ViewHolder holder){
            if(adapter!=null){
                return adapter.onFailedToRecycleView(holder);
            }
            return false;
        }

        @Override
        public void registerAdapterDataObserver(AdapterDataObserver observer){
            if(adapter!=null){
                adapter.registerAdapterDataObserver(observer);
            }
        }

        @Override
        public void unregisterAdapterDataObserver(AdapterDataObserver observer){
            if(adapter!=null){
                adapter.unregisterAdapterDataObserver(observer);
            }
        }

        public Adapter getOriginalAdapter(){
            return this.adapter;
        }

        public boolean isRefreshHeader(int position){
            return position==0;
        }

        public boolean isLoadingMoreFooter(int position){
            if(loadingMoreEnabled){
                return position==getItemCount()-1;
            }else{
                return false;
            }
        }

        private class SimpleViewHolder extends ViewHolder{

            public SimpleViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    private class DataObserver extends AdapterDataObserver{//Adapter数据监听器 与 WrapAdapter联动作用
        @Override
        public void onChanged(){
            if(mWrapAdapter!=null){
                mWrapAdapter.notifyDataSetChanged();
            }

        }

        @Override
        public void onItemRangeInserted(int positionStart,int itemCount){
            mWrapAdapter.notifyItemRangeInserted(positionStart,itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart,int itemCount){
            mWrapAdapter.notifyItemRangeChanged(positionStart,itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart,int itemCount,Object payload){
            mWrapAdapter.notifyItemRangeChanged(positionStart,itemCount,payload);
        }

        @Override
        public void onItemRangeRemoved(int positionStart,int itemCount){
            mWrapAdapter.notifyItemRangeRemoved(positionStart,itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition,int toPosition,int itemCount){
            mWrapAdapter.notifyItemMoved(fromPosition,toPosition);
        }
    }

    public boolean isPullToRefreshEnabled() {
        return pullToRefreshEnabled;
    }

    public void setPullToRefreshEnabled(boolean pullToRefreshEnabled) {
        this.pullToRefreshEnabled = pullToRefreshEnabled;
    }

    public void setLoadingMoreEnabled(boolean loadingMoreEnabled) {
        this.loadingMoreEnabled = loadingMoreEnabled;
        if(!loadingMoreEnabled){
            loadingMoreFooter.setVisibility(View.GONE);
        }
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    public void setTIMEOUT(int TIMEOUT) {
        this.TIMEOUT = TIMEOUT;
    }


}
