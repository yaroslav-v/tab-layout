package com.gibstudio.tablayoutexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.gibstudio.tablayout.TabLayout;

public class StaticTabsActivity extends AppCompatActivity implements TabLayout.OnTabAnimationListener {

    private TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_static_tabs);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // setup bottom tabs
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.title_tabbar_tab1)
                .setIcon(R.drawable.tabbar_tab1));
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.title_tabbar_tab2)
                .setIcon(R.drawable.tabbar_tab2));
        mTabLayout.setOnTabAnimationListener(this);

        // change colors programmatically
        mTabLayout.setBackgroundResource(R.color.colorPrimary);
        mTabLayout.setTabSelectedTextColor(getResources().getColor(R.color.colorAccent));
        mTabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.text_color_dark));
    }

    @Override
    public void onTabAnimationStart(TabLayout.Tab tab) {
//        Toast.makeText(this, R.string.toast_animation_start, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTabAnimationEnd(TabLayout.Tab tab) {
        Toast.makeText(this, R.string.toast_animation_end, Toast.LENGTH_LONG).show();
    }
}
