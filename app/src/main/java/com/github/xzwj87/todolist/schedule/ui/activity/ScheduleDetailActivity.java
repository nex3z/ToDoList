package com.github.xzwj87.todolist.schedule.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.github.xzwj87.todolist.R;
import com.github.xzwj87.todolist.schedule.internal.di.HasComponent;
import com.github.xzwj87.todolist.schedule.internal.di.component.DaggerScheduleComponent;
import com.github.xzwj87.todolist.schedule.internal.di.component.ScheduleComponent;
import com.github.xzwj87.todolist.schedule.internal.di.module.ScheduleModule;
import com.github.xzwj87.todolist.schedule.ui.fragment.ScheduleDetailFragment;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class ScheduleDetailActivity extends BaseActivity implements HasComponent<ScheduleComponent> {
    private static final String LOG_TAG = ScheduleDetailActivity.class.getSimpleName();

    public static final String SCHEDULE_ID = "id";

    private long mScheduleId;
    private ScheduleComponent mScheduleComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_detail);

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp);
        }

        if (savedInstanceState == null) {
            mScheduleId = getIntent().getLongExtra(SCHEDULE_ID, 0);
            Log.v(LOG_TAG, "onCreate(): mScheduleId = " + mScheduleId);

            ScheduleDetailFragment fragment = ScheduleDetailFragment.newInstance(mScheduleId);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.schedule_detail_container, fragment)
                    .commit();

            initializeInjector();
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, ScheduleListActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public ScheduleComponent getComponent() {
        return mScheduleComponent;
    }

    private void initializeInjector() {
        mScheduleComponent = DaggerScheduleComponent.builder()
                .appComponent(getApplicationComponent())
                .activityModule(getActivityModule())
                .scheduleModule(new ScheduleModule(mScheduleId))
                .build();
    }

    @OnClick(R.id.fab)
    public void editSchedule(View view) {
        Log.v(LOG_TAG, "editSchedule()");
        Intent intent = new Intent(this, AddScheduleActivity.class);
        intent.putExtra(AddScheduleActivity.SCHEDULE_ID, mScheduleId);
        startActivity(intent);
    }
}
