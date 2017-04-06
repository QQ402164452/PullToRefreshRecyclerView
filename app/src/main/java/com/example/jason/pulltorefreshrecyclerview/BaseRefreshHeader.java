package com.example.jason.pulltorefreshrecyclerview;

/**
 * Created by Jason on 2017/4/2.
 */

public interface BaseRefreshHeader {
    int STATE_NORMAL=0;
    int STATE_RELEASE=1;
    int STATE_REFRESHING=2;
    int STATE_COMPLETE=3;

    void onMove(float delta);
    void onComplete();
    boolean onRelease();
    void onStateChange(int state);
}
