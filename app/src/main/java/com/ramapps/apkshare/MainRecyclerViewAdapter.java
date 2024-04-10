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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.function.Consumer;

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<MainRecyclerViewAdapter.CustomViewHolder> {

    private Context context;
    private List<PackageInfo> packagesInfo;
    private List<Boolean> selectionTracker;

    public MainRecyclerViewAdapter(Context context, List<PackageInfo> packagesInfo, List<Boolean> selectionTracker) {
        this.context = context;
        this.packagesInfo = packagesInfo;
        this.selectionTracker = selectionTracker;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CustomViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.main_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        final int index = position;
        holder.bind(packagesInfo.get(index));
        holder.getCardViewContainer().setChecked(selectionTracker.get(index));
        holder.getCardViewContainer().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedCount = 0;
                for (Boolean b : selectionTracker) {
                    if(b) selectedCount += 1;
                }
                if(!selectionTracker.get(index)) {
                    if(selectedCount < 5){
                        selectionTracker.set(index, true);
                        selectedCount++;
                    } else {
                        Toast.makeText(context, "You can share 5 apps synchronized only.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    selectionTracker.set(index, false);
                    selectedCount--;
                }
                if(selectedCount > 0) {
                    MainActivity.fabSend.show();
                } else {
                    MainActivity.fabSend.hide();
                }
                ((MaterialCardView) v).setChecked(selectionTracker.get(index));
            }
        });
    }

    @Override
    public int getItemCount() {
        return packagesInfo.size();
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardViewContainer;
        private final ImageView imageViewIcon;
        private final TextView textViewAppName;
        private final TextView textViewAppDetail;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            cardViewContainer = itemView.findViewById(R.id.listItemCardViewContainer);
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

        public MaterialCardView getCardViewContainer() {
            return cardViewContainer;
        }
    }
}
