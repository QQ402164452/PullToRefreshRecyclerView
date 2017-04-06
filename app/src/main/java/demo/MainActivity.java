package demo;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.widget.Toast;

import com.example.jason.pulltorefreshrecyclerview.OnRefreshListener;
import com.example.jason.pulltorefreshrecyclerview.PullToRefreshRecyclerView;
import com.example.jason.pulltorefreshrecyclerview.R;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    PullToRefreshRecyclerView recyclerView;
    private DemoAdapter adapter;
    private ArrayList<People> list;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler=new Handler();

        list=new ArrayList<>();
        list.add(new People("Jason","Chen"));
        list.add(new People("Sarah","Michel"));
        list.add(new People("Michelle",""));
        list.add(new People("Amy","Thomas"));
        list.add(new People("Mary","Williams"));
        list.add(new People("Kim","Davis"));
        list.add(new People("John","Evans"));
        list.add(new People("David","Jones"));
        list.add(new People("Michael","Johnson"));
        list.add(new People("Thomas","Jones"));
        list.add(new People("Michelle",""));
        list.add(new People("Amy","Thomas"));
        list.add(new People("Mary","Williams"));
        list.add(new People("Thomas","Jones"));
        adapter=new DemoAdapter(list);

        recyclerView= (PullToRefreshRecyclerView) findViewById(R.id.PullToRefreshRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"刷新成功",Toast.LENGTH_SHORT).show();
                        recyclerView.refreshComplete();
                    }
                },2000);
            }

            @Override
            public void onRefreshTimeOut() {
                handler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onLoadMore() {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        list.add(new People("1111","1111"));
                        list.add(new People("2222","2222"));
                        list.add(new People("3333","3333"));
                        list.add(new People("4444","4444"));
                        list.add(new People("5555","5555"));
                        adapter.notifyDataSetChanged();

                        recyclerView.loadingMoreComplete();
                        Toast.makeText(MainActivity.this, "加载更多成功", Toast.LENGTH_SHORT).show();
                        recyclerView.setNoMore(true);
                    }
                },2000);
            }

            @Override
            public void onLoadMoreTimeOut() {
                handler.removeCallbacksAndMessages(null);
            }
        });
    }
}
