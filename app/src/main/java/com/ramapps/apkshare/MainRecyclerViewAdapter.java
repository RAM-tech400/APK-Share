package com.ramapps.apkshare;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<MainRecyclerViewAdapter.CustomViewHolder> {

    private Context context;
    private List<PackageInfo> packagesInfo;

    public MainRecyclerViewAdapter(Context context, List<PackageInfo> packagesInfo) {
        this.context = context;
        this.packagesInfo = packagesInfo;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CustomViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.main_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        holder.bind(packagesInfo.get(position));
    }

    @Override
    public int getItemCount() {
        return packagesInfo.size();
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageViewIcon;
        private final TextView textViewAppName;
        private final TextView textViewAppDetail;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.listItemImageViewAppIcon);
            textViewAppName = itemView.findViewById(R.id.mainListTextViewAppName);
            textViewAppDetail = itemView.findViewById(R.id.mainListTextViewAppDetail);
        }

        public void bind(PackageInfo packageInfo){
            textViewAppName.setSelected(true);
            try {
                imageViewIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(packageInfo.packageName));
                textViewAppName.setText(context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo));
                textViewAppDetail.setText(packageInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public TextView getTextViewAppDetail() {
            return textViewAppDetail;
        }

        public TextView getTextViewAppName() {
            return textViewAppName;
        }

        public ImageView getImageViewIcon() {
            return imageViewIcon;
        }
    }
}
