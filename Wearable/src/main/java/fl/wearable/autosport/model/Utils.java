package fl.wearable.autosport.model;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.content.ContentValues.TAG;

public class Utils {
    public static String assetFilePath(Context context, String assetName) {
        File dir = new File(context.getFilesDir(),"sync");

        File file = new File(dir, assetName);
        Log.d(TAG, file.getAbsolutePath());
        return file.getAbsolutePath();

        /*try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e("pytorchandroid", "Error process asset " + assetName + " to file path");
        }
        return null;*/
    }
    //classifier = new Classifier(Utils.assetFilePath(this,"script_model.pt"));

}