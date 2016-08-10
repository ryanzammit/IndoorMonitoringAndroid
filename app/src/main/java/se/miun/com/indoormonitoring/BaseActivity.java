package se.miun.com.indoormonitoring;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Faustino on 10-8-2016.
 */
public class BaseActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onCreateWithLayout(savedInstanceState, 0);
        if (mToolbar != null) {
            initToolbar();
        }
    }

    private void initToolbar() {
        setSupportActionBar(mToolbar);
    }

    public void setToolbarTitle(String toolbarTitle) {
        if (mToolbar != null) {
            mToolbar.setTitle(toolbarTitle);
        }
    }

    @CallSuper
    protected void onCreateWithLayout(@Nullable Bundle savedInstanceState, @LayoutRes int layout) {
        if (layout != 0) {
            setContentView(layout);
            ButterKnife.bind(this);
        }
    }
}
