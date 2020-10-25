package fl.wearable.autosport.lib;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.wear.widget.drawer.WearableActionDrawerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import fl.wearable.autosport.R;

public class DrawerTestActivity extends WearableActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_test);

        mTextView = findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();

        /*MyFragment myFragment = new MyFragment();
        android.app.Fra
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, mPlanetFragment).commit();*/

        WearableNavigationDrawerView mWearableNavigationDrawer = findViewById(R.id.top_navigation_drawer);
        mWearableNavigationDrawer.setAdapter(new NavigationAdapter());
        TextView t1 = new TextView(this), t2 = new TextView(this), t3 = new TextView(this);
        t1.setText("pierwszy\npierwszy\npierwszy\npierwszy\npierwszy\npierwszy\npierwszy\npierwszy");
        t2.setText("drugi");
        t3.setText("trzeci");

        View view = this.getLayoutInflater().inflate(R.layout.main, null);
        //mWearableNavigationDrawer.addView(view);


        WearableActionDrawerView mWearableActionDrawer = findViewById(R.id.bottom_action_drawer);
        mWearableActionDrawer.getController().peekDrawer();

        mWearableActionDrawer.setDrawerContent(view);
        //mWearableActionDrawer.setOnMenuItemClickListener(this);
    }

    static class NavigationAdapter extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {
        private final String[] labels = new String[]{"jeden", "dwa", "trzy", "cztery"};

        @Override
        public CharSequence getItemText(int pos) {
            return labels[pos];
        }

        @Override
        public Drawable getItemDrawable(int pos) {

            return null;
        }

        @Override
        public int getCount() {
            return labels.length;
        }
    }

    static class MyFragment extends Fragment {
        public MyFragment() {
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.main, container, false);
        }
    }
}
