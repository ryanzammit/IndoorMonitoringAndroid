package se.miun.com.indoormonitoring;

import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends BaseActivity {


    @Override
    protected void onCreateWithLayout(@Nullable Bundle savedInstanceState, @LayoutRes int layout) {
        super.onCreateWithLayout(savedInstanceState, R.layout.activity_generic_toolbar);

        setToolbarTitle("Home");
    }
}
