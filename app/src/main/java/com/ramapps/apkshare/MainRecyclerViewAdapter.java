package com.ramapps.apkshare;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.List;

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<MainRecyclerViewAdapter.CustomViewHolder> {

    private Context context;
    private List<PackageInfo> packagesInfo;
    private List<Boolean> selectionTracker;
    private int columnCount = 0;

    public MainRecyclerViewAdapter(Context context, List<PackageInfo> packagesInfo, List<Boolean> selectionTracker) {
        this.context = context;
        this.packagesInfo = packagesInfo;
        this.selectionTracker = selectionTracker;
        columnCount = context.getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getInt(MainActivity.PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1;
    }
    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (columnCount == 1) {
            return new CustomViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.main_list_single_column_item, parent, false));
        }
        return new CustomViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.main_list_multi_column_item, parent, false));
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
        holder.getCardViewContainer().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int action = context.getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getInt(MainActivity.PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0);
                if (action == 1) {
                    Intent intent = new Intent(Intent.ACTION_DELETE);
                    intent.setData(Uri.fromParts("package", packagesInfo.get(index).packageName, null));
                    context.startActivity(intent);
                } else if (action == 2) {
                    File file = new File(packagesInfo.get(index).applicationInfo.publicSourceDir);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, ".provider", file));
                    context.startActivity(Intent.createChooser(intent, "Share apk file via: "));
                } else {
                    context.startActivity(context.getPackageManager().getLaunchIntentForPackage(packagesInfo.get(index).packageName));
                }
                return false;
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
                int quickInfoSettings = context.getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getInt(MainActivity.PREFERENCES_SETTINGS_QUICK_INFO, 1);
                if (quickInfoSettings == 1) {
                    textViewAppDetail.setText(packageInfo.packageName);
                } else if (quickInfoSettings == 2) {
                    textViewAppDetail.setText(packageInfo.versionCode + "");
                } else if (quickInfoSettings == 3) {
                    textViewAppDetail.setText(packageInfo.versionName);
                } else {
                    textViewAppDetail.setVisibility(View.GONE);
                }
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
