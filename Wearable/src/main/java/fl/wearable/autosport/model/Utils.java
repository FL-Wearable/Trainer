package fl.wearable.autosport.model;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    public static String assetFilePath(Context context, String paramFile) {
        File dir = new File(context.getFilesDir(),"sync");
        if (!dir.exists()){
            dir.mkdir();
        }
        File paramfile = new File(dir, paramFile);
        //File file = new File(context.getFilesDir(), paramFile);

        if (!paramfile.exists()){
            try {
                paramfile.createNewFile();
                try (InputStream is = context.getAssets().open(paramFile)) {
                    try (OutputStream os = new FileOutputStream(paramfile)) {
                        byte[] buffer = new byte[4 * 1024];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                        os.flush();
                    }
                } catch (IOException e) {
                    Log.e("pytorchandroid", "Error process asset " + paramFile + " to file path");
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        return paramfile.getAbsolutePath();
    }
    //classifier = new Classifier(Utils.assetFilePath(this,"script_model.pt"));

}