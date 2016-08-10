package se.miun.com.indoormonitoring.activities;

import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.os.Bundle;

import se.miun.com.indoormonitoring.R;

public class MainActivity extends BaseActivity {


    @Override
    protected void onCreateWithLayout(@Nullable Bundle savedInstanceState, @LayoutRes int layout) {
        super.onCreateWithLayout(savedInstanceState, R.layout.activity_generic_toolbar);

        setToolbarTitle("Home");
    }
}
