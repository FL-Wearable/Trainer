package fl.wearable.autosport.lib;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import fl.wearable.autosport.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileItemAdapter extends RecyclerView.Adapter<FileItemAdapter.ViewHolder> {
    private static final String TAG = FileItemAdapter.class.getSimpleName();

    private static final int TAG_FILE_ID = 0x12345678;

    private List<FileItem> mFileItems = new ArrayList<>();
    private SimpleDateFormat sdfDate = new SimpleDateFormat("dd. MMM yyyy", Locale.getDefault()),
        sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private RequestListener requestListener;

    public static interface RequestListener {
        void onRequest(String fileName);
    }

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }

    public void setFiles(File ... files) {
        mFileItems.clear();

        for (File file : files) {
            try {
                mFileItems.add(new FileItem(file));
            } catch (Exception ex) {
                Log.d(TAG, "Failed to load the file " + file + " due to " + ex.getClass().getSimpleName() + " " + ex.getMessage());

                //for (StackTraceElement stack :  ex.getStackTrace()) {
                    //Log.d(TAG, stack.getFileName() + " " + " " + stack.getClassName() + " " + stack.getMethodName() + " " + stack.getLineNumber());
                //}
                Log.d(TAG, ex.getStackTrace()[0].getFileName() + " " +  ex.getStackTrace()[0].getLineNumber());

            }
        }

        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View fileItemView = inflater.inflate(R.layout.file_item_entry, parent, false);
        ViewHolder viewHolder = new ViewHolder(fileItemView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem fileItem = mFileItems.get(position);
        holder.mRootLayout.setTag(TAG_FILE_ID, fileItem.getFileName());

        float totalDurationSeconds = (fileItem.getStopNanoseconds() - fileItem.getStartNanoseconds()) / 1000f / 1000f / 1000f;
        int durationHours = (int)(totalDurationSeconds / 3600);
        int durationMinutes = (int)(totalDurationSeconds / 60 - durationHours * 60);
        int durationSeconds = (int)(totalDurationSeconds - durationMinutes * 60 - durationHours * 3600);

        Date startDateTime = new Date(fileItem.getStartTimestampRtc());
        holder.mTextFirstLine.setText(sdfDate.format(startDateTime) + " " + sdfTime.format(startDateTime));
        holder.mTextSecondLine.setText(String.format("%d:%02d:%02d", durationHours, durationMinutes, durationSeconds) + " // " + String.format("%.1f kB", fileItem.getFileSize() / 1024f));

        holder.mTextThirdLine.setText("❤ Φ " + String.format("%.0f bpm ↑ %d bpm", fileItem.getAvgHeartRate(), fileItem.getMaxHeartRate()));
        holder.mTextFourthLine.setText("\uD83D\uDE80 Φ " + String.format("%.1f", fileItem.getAvgSpeed() * 3.6f) + "km/h ↑ " + String.format("%.0f", fileItem.getTotalAscent()) + " m ↓ " + String.format("%.0f", fileItem.getTotalDescent()) + " m");
    }

    @Override
    public int getItemCount() {
        return mFileItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private View mRootLayout;
        private TextView mTextFirstLine, mTextSecondLine, mTextThirdLine, mTextFourthLine;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mRootLayout = itemView.findViewById(R.id.rootLayout);
            mTextFirstLine = itemView.findViewById(R.id.textFirstLine);
            mTextSecondLine = itemView.findViewById(R.id.textSecondLine);
            mTextThirdLine = itemView.findViewById(R.id.textThirdLine);
            mTextFourthLine = itemView.findViewById(R.id.textFourthLine);

            mRootLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("milt", "On click " + mRootLayout.getTag(TAG_FILE_ID));
                }
            });

            mRootLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Object tag = mRootLayout.getTag(TAG_FILE_ID);

                    Log.i("milt", "Long click " + tag);

                    if (tag != null) {
                        if (requestListener != null) {
                            requestListener.onRequest(tag.toString());
                        }
                    }

                    return true;
                }
            });
        }
    }
}
