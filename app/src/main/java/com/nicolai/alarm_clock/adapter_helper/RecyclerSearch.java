package com.nicolai.alarm_clock.adapter_helper;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.nicolai.alarm_clock.ControlCenter;
import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.pojos.SearchPOJO;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Nicolai on 30.08.2017.
 */

public class RecyclerSearch extends RecyclerView.Adapter<RecyclerSearch.WeckerHolder> {

    private static List<SearchPOJO> mUser; //TODO warum für if in onClick static?
    protected static int position;
    public int id;
    private static boolean ready;
    public ControlCenter sc;
    private final RecyclerSearch.UserClicked mClickInterface;

    //Interface for clicks
    public interface UserClicked{
        void userClicked(String userId, String name, String image);
    }

    public static class WeckerHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView nameView;
        private ImageView imageView, info;

        private WeakReference<RecyclerSearch.UserClicked> mWeakRefernce;

        public WeckerHolder(View v, RecyclerSearch.UserClicked listener) {
            super(v);
            mWeakRefernce = new WeakReference<>(listener);

            nameView = (TextView) v.findViewById(R.id.user_username);
            imageView = v.findViewById(R.id.CCcircle_image);
            info = v.findViewById(R.id.requestInfo);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            String userId = mUser.get(getAdapterPosition()).getUserId();

            if(!userId.equals(FirebaseAuth.getInstance().getUid())){
                String name = mUser.get(getAdapterPosition()).getName();
                String image = mUser.get(getAdapterPosition()).getImage();
                mWeakRefernce.get().userClicked(userId, name, image);
            }else{
                Toast.makeText(itemView.getContext(), R.string.dasBistDu, Toast.LENGTH_SHORT).show();
            }
        }

        public void bindWecker(String name, final String image) {
            info.setVisibility(View.GONE);
            nameView.setText(name);
            if(!image.isEmpty()){
                Picasso.get().load(image).networkPolicy(NetworkPolicy.OFFLINE).into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        //Wurde offline gefunden
                    }

                    @Override
                    public void onError(Exception e) {
                        //Placholder erst ab version
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            //Muss online geladen werden
                            Picasso.get().load(image).placeholder(R.drawable.ic_account_circle_primary_24dp).into(imageView);
                        }else{
                            //Muss online geladen werden
                            Picasso.get().load(image).into(imageView);
                        }
                    }
                });
            }else{
                imageView.setImageResource(R.drawable.ic_account_circle_primary_24dp);
            }
        }
    }


    public RecyclerSearch(List<SearchPOJO> searchPOJOS, RecyclerSearch.UserClicked mClickInterface) {
        mUser = searchPOJOS;
        this.mClickInterface = mClickInterface;
    }

    @Override
    public WeckerHolder onCreateViewHolder(ViewGroup parent, int viewType) { //RecyclerAdapter.
        View inflatedView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_request_layout, parent, false);
        return new WeckerHolder(inflatedView, mClickInterface);
    }

    @Override
    public void onBindViewHolder(final RecyclerSearch.WeckerHolder holder, final int position) {

        String name = mUser.get(position).getName();
        String image = mUser.get(position).getImage();

        holder.bindWecker(name, image);
    }

    @Override
    public int getItemCount() {
        return mUser.size();
    }

    public void upgrade (List<SearchPOJO> searchPOJOS){
        mUser = searchPOJOS;
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

    }
}