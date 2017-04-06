package demo;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.jason.pulltorefreshrecyclerview.R;

import java.util.ArrayList;

/**
 * Created by Jason on 2017/4/3.
 */

public class DemoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<People> list;

    public DemoAdapter(ArrayList<People> list){
        this.list=list;
    }

    public void setList(ArrayList<People> list){
        this.list=list;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item,parent,false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ItemHolder itemHolder= (ItemHolder) holder;
        itemHolder.firstName.setText(list.get(position).getFirstName());
        itemHolder.lastName.setText(list.get(position).getLastName());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private static class ItemHolder extends RecyclerView.ViewHolder{
        private TextView firstName;
        private TextView lastName;

        public ItemHolder(View itemView) {
            super(itemView);
            firstName= (TextView) itemView.findViewById(R.id.Item_FirstName);
            lastName= (TextView) itemView.findViewById(R.id.Item_LastName);
        }
    }

}
