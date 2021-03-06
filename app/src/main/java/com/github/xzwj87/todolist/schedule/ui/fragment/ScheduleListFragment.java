package com.github.xzwj87.todolist.schedule.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.xzwj87.todolist.R;
import com.github.xzwj87.todolist.schedule.interactor.UseCase;
import com.github.xzwj87.todolist.schedule.interactor.mapper.ScheduleModelDataMapper;
import com.github.xzwj87.todolist.schedule.interactor.query.GetAllScheduleArg;
import com.github.xzwj87.todolist.schedule.interactor.query.GetScheduleListByTypeArg;
import com.github.xzwj87.todolist.schedule.interactor.query.SearchScheduleArg;
import com.github.xzwj87.todolist.schedule.internal.di.component.ScheduleComponent;
import com.github.xzwj87.todolist.schedule.presenter.ScheduleListPresenter;
import com.github.xzwj87.todolist.schedule.presenter.ScheduleListPresenterImpl;
import com.github.xzwj87.todolist.schedule.ui.ScheduleListView;
import com.github.xzwj87.todolist.schedule.ui.adapter.ScheduleAdapter;
import com.github.xzwj87.todolist.schedule.ui.model.ScheduleModel;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ScheduleListFragment extends BaseFragment implements
        ScheduleAdapter.DataSource, ScheduleListView {
    private static final String LOG_TAG = ScheduleListFragment.class.getSimpleName();

    public static final String SCHEDULE_TYPE_DONE = "done";

    private static final String SCHEDULE_TYPE = "schedule_type";
    private static final String QUERY = "query";

    private String mScheduleType;
    private Callbacks mCallbacks = sDummyCallbacks;
    private ScheduleAdapter mScheduleAdapter;
    private ScheduleListPresenter mScheduleListPresenter;
    private boolean mIsSearchMode = false;
    private String mQuery;
    private boolean mSwipeMarkAsDone = true;
    private int mLastRemovedPosition = -1;

    @Inject @Named("markScheduleAsDone") UseCase mMarkScheduleAsDone;
    @Inject @Named("getAllSchedule") UseCase mGetAllSchedule;
    @Inject @Named("getScheduleListByType") UseCase mGetScheduleListByType;
    @Inject @Named("searchSchedule") UseCase mSearchSchedule;
    @Inject ScheduleModelDataMapper mMapper;

    @Bind(R.id.rv_schedule_list) RecyclerView mRvScheduleList;

    public interface Callbacks {
        void onItemSelected(long id, ScheduleAdapter.ViewHolder vh);
    }

    private static Callbacks sDummyCallbacks = (id, vh) -> { };

    public ScheduleListFragment() {}

    public static ScheduleListFragment newInstanceByType(String scheduleType) {
        ScheduleListFragment fragment = new ScheduleListFragment();

        Bundle args = new Bundle();
        args.putString(SCHEDULE_TYPE, scheduleType);
        fragment.setArguments(args);

        return fragment;
    }

    public static ScheduleListFragment newInstanceByQuery(String query) {
        ScheduleListFragment fragment = new ScheduleListFragment();

        Bundle args = new Bundle();
        args.putString(QUERY, query);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_schedule_list, container, false);
        ButterKnife.bind(this, rootView);

        mSwipeMarkAsDone = true;
        Bundle arguments = getArguments();
        if (arguments != null) {
            if (arguments.containsKey(SCHEDULE_TYPE)) {
                mScheduleType = arguments.getString(SCHEDULE_TYPE);
                Log.v(LOG_TAG, "onCreateView(): mScheduleType = " + mScheduleType);
                if (mScheduleType != null && mScheduleType.equals(SCHEDULE_TYPE_DONE)) {
                    mSwipeMarkAsDone = false;
                }
                Log.v(LOG_TAG, "onCreateView(): mScheduleType = " + mScheduleType +
                        ", mSwipeMarkAsDone = " + mSwipeMarkAsDone);
            } else  if (arguments.containsKey(QUERY)) {
                mIsSearchMode = true;
                mQuery = arguments.getString(QUERY);
                Log.v(LOG_TAG, "onCreateView(): mQuery = " + mQuery);
            }

        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initialize();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onResume() {
        super.onResume();
        mScheduleListPresenter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScheduleListPresenter.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScheduleListPresenter.destroy();
    }

    @Override
    public ScheduleModel getItemAtPosition(int position) {
        return mScheduleListPresenter.getScheduleAtPosition(position);
    }

    @Override
    public int getItemCount() {
        return mScheduleListPresenter.getScheduleItemCount();
    }

    @Override
    public void renderScheduleList() {
        Log.v(LOG_TAG, "renderScheduleList(): mLastRemovedPosition = " + mLastRemovedPosition);
        if (mLastRemovedPosition != -1) {
            mScheduleAdapter.notifyItemRemoved(mLastRemovedPosition);
            mLastRemovedPosition = -1;
        } else {
            mScheduleAdapter.notifyDataSetChanged();
        }

    }

    @SuppressWarnings("unchecked")
    private void initialize() {
        getComponent(ScheduleComponent.class).inject(this);

        UseCase getListUseCase;
        if (mIsSearchMode) {
            getListUseCase = mSearchSchedule.init(new SearchScheduleArg(mQuery));
        } else {
            if (mScheduleType != null) {
                if (mScheduleType.equals(SCHEDULE_TYPE_DONE)) {
                    getListUseCase = mGetAllSchedule.init(
                            new GetAllScheduleArg(ScheduleModel.DONE));
                } else {
                    getListUseCase = mGetScheduleListByType.init(
                            new GetScheduleListByTypeArg(mScheduleType, ScheduleModel.UNDONE));
                }
            } else {
                getListUseCase = mGetAllSchedule.init(new GetAllScheduleArg(ScheduleModel.UNDONE));
            }
        }

        mScheduleListPresenter = new ScheduleListPresenterImpl(
                getListUseCase, mMarkScheduleAsDone, mMapper);
        mScheduleListPresenter.setView(this);

        setupRecyclerView();

        loadScheduleListData();
    }

    private void loadScheduleListData() {
        mScheduleListPresenter.initialize();
    }

    private void setupRecyclerView() {
        mScheduleAdapter = new ScheduleAdapter(this);
        mScheduleAdapter.setOnItemClickListener((position, vh) -> {
            long id = mScheduleListPresenter.getScheduleAtPosition(position).getId();
            Log.v(LOG_TAG, "onItemClick(): position = " + position + ", id = " + id);
            mCallbacks.onItemSelected(id, vh);
        });
        mRvScheduleList.setAdapter(mScheduleAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRvScheduleList.setLayoutManager(layoutManager);

        mRvScheduleList.setItemAnimator(new DefaultItemAnimator());
        mRvScheduleList.setHasFixedSize(true);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        return false;
                    }
                    @Override
                    public boolean isItemViewSwipeEnabled() {
                        return !mIsSearchMode;
                    }
                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        Log.v(LOG_TAG, "onSwiped(): position = " + position +
                                 ", direction = " + direction);
                        long id = mScheduleAdapter.getItemId(position);
                        mScheduleListPresenter.markAsDone(new long[] {id}, mSwipeMarkAsDone);
                        mLastRemovedPosition = position;
                        showSnackBarNotification(id, mSwipeMarkAsDone);
                    }
                });
        itemTouchHelper.attachToRecyclerView(mRvScheduleList);
    }

    private void showSnackBarNotification(long id, boolean undoMarkAsDone) {
        String message = undoMarkAsDone ?
                getString(R.string.marked_done) : getString(R.string.marked_undone);
        Snackbar snackbar = Snackbar.make(mRvScheduleList, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(getString(R.string.undo), v -> {
            mScheduleListPresenter.markAsDone(new long[] {id}, !undoMarkAsDone);
        });
        snackbar.show();
    }
}
