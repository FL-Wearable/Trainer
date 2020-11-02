package fl.wearable.autosport.menu;


import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;
import fl.wearable.autosport.R;
import me.samlss.timomenu.TimoMenu;
import me.samlss.timomenu.animation.ScaleItemAnimation;
import me.samlss.timomenu.interfaces.OnTimoItemClickListener;
import me.samlss.timomenu.interfaces.TimoMenuListener;
import me.samlss.timomenu.view.TimoItemView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

import static android.content.ContentValues.TAG;
import static fl.wearable.autosport.lib.Constants.COMMA_DELIMITER;
import static fl.wearable.autosport.lib.Constants.NEW_LINE_SEPARATOR;

public class CenterActivity extends android.app.Activity {
    private TimoMenu mTimoMenu;
    private Integer inferenceResult;
    private long fileName;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inferenceResult = getIntent().getIntExtra("inferenceResult",0);
        fileName =  getIntent().getLongExtra("filename",0);
        android.util.Log.d(TAG, "currentCSVName now 2 is " + fileName);

        setContentView(fl.wearable.autosport.R.layout.activity_center);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(fl.wearable.autosport.R.color.colorPrimary));
        }
        String result = getString(R.string.result, getString(MenuHelper.ROW_TEXT[0][inferenceResult]),0);
        android.util.Log.d(TAG, "show inference result is " + inferenceResult);
        android.widget.Button btn = (android.widget.Button) findViewById(R.id.show);
        btn.setText(result);
        init();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            if (mTimoMenu.isShowing()){
                mTimoMenu.dismiss();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void init(){

        int itemViewWidth = (getWindow().getWindowManager().getDefaultDisplay().getWidth() - 40) / 3;

        mTimoMenu =  new TimoMenu.Builder(this)
                .setGravity(Gravity.CENTER)
                .setTimoMenuListener(new TimoMenuListener() {
                    @Override
                    public void onShow() {
                        Toast.makeText(getApplicationContext(), "Show", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDismiss() {
                        Toast.makeText(getApplicationContext(), "Dismiss", Toast.LENGTH_SHORT).show();
                    }
                })
                .setTimoItemClickListener(new OnTimoItemClickListener() {
                    @Override
                    public void onItemClick(int row, int index, TimoItemView view) {
                        Toast.makeText(getApplicationContext(), String.format("%s selected~", getString(MenuHelper.ROW_TEXT[row][index])), Toast.LENGTH_SHORT).show();
                        // TODO: write user correction to file
                        try {
                            appendCol(fileName,index);
                        } catch (java.io.FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setMenuMargin(new Rect(20, 20, 20, 20))
                .setMenuPadding(new Rect(0, 10, 0, 10))
                .addRow(ScaleItemAnimation.create(), MenuHelper.getTopList(itemViewWidth))
                //.addRow(ScaleItemAnimation.create(), MenuHelper.getBottomList(itemViewWidth))
                .build();
    }

    public void onShow(View view) {
        mTimoMenu.show();
    }

    public void appendCol(long fileName, Integer data) throws java.io.FileNotFoundException {

        String lineSep = NEW_LINE_SEPARATOR;
        String output = "";
        android.util.Log.d(TAG, "currentCSVName now is " + fileName);
        android.util.Log.d(TAG, "user correction now is " + data);

        try{
            java.io.BufferedReader br = new BufferedReader(new FileReader((new File(getFilesDir(), fileName + ".csv"))));
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                output += line + COMMA_DELIMITER + data + lineSep;
            }
            br.close();
            FileWriter fw = new FileWriter(new File(getFilesDir(), fileName + ".csv"), false);
            fw.write(output);
            //fw.flush();
            fw.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
