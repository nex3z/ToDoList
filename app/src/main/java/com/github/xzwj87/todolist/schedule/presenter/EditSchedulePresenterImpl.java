package com.github.xzwj87.todolist.schedule.presenter;


import android.database.Cursor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.xzwj87.todolist.schedule.interactor.DefaultSubscriber;
import com.github.xzwj87.todolist.schedule.interactor.QueryUseCase;
import com.github.xzwj87.todolist.schedule.interactor.UpdateUseCase;
import com.github.xzwj87.todolist.schedule.interactor.mapper.ScheduleContentValuesDataMapper;
import com.github.xzwj87.todolist.schedule.interactor.mapper.ScheduleModelDataMapper;
import com.github.xzwj87.todolist.schedule.ui.AddScheduleView;
import com.github.xzwj87.todolist.schedule.ui.model.ScheduleModel;
import com.github.xzwj87.todolist.schedule.utility.ScheduleUtility;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class EditSchedulePresenterImpl implements AddSchedulePresenter {

    private static final String LOG_TAG = EditSchedulePresenterImpl.class.getSimpleName();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("E MMM d, yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("kk:mm");

    private static final long MILLISECONDS_IN_10_MINUTES = 10 * 60 * 1000;
    private static final long MILLISECONDS_IN_30_MINUTES = MILLISECONDS_IN_10_MINUTES * 3;
    private static final long MILLISECONDS_IN_1_HOUR = MILLISECONDS_IN_30_MINUTES * 2;

    private UpdateUseCase mUpdateUseCase;
    private QueryUseCase mQueryUseCase;
    private ScheduleContentValuesDataMapper mContentValueMapper;
    private ScheduleModelDataMapper mModelMapper;
    private AddScheduleView mView;

    private ScheduleModel mSchedule;

    public EditSchedulePresenterImpl() {}

    public EditSchedulePresenterImpl(UpdateUseCase updateUseCase, QueryUseCase queryUseCase,
                                     ScheduleContentValuesDataMapper contentValueMapper,
                                     ScheduleModelDataMapper modelDataMapper) {
        mUpdateUseCase = updateUseCase;
        mQueryUseCase = queryUseCase;
        mContentValueMapper = contentValueMapper;
        mModelMapper = modelDataMapper;
    }

    @Override
    public void setView(@NonNull AddScheduleView view) {
        mView = view;
    }

    @Override
    public void initialize() {
        loadSchedule();
    }

    @Override
    public void setStartDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mSchedule.getScheduleStart());
        mView.showPickStartDateDlg(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void setEndDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mSchedule.getScheduleEnd());
        mView.showPickEndDateDlg(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void setStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mSchedule.getScheduleStart());
        mView.showPickStartTimeDlg(calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    @Override
    public void setEndTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mSchedule.getScheduleEnd());
        mView.showPickEndTimeDlg(calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    @Override
    public void setAlarmType() {
        mView.showPickAlarmTypeDlg(mSchedule.getAlarmType());
    }

    @Override
    public void setScheduleType() {
        mView.showPickScheduleTypeDlg(mSchedule.getType());
    }

    @Override
    public void onTitleSet(String title) {
        mSchedule.setTitle(title);
    }

    @Override
    public void onStartDateSet(int year, int monthOfYear, int dayOfMonth) {
        Date startDate = updateDateFromBase(mSchedule.getScheduleStart(),
                year, monthOfYear, dayOfMonth);
        mSchedule.setScheduleStart(startDate);
        mView.updateStartDateDisplay(DATE_FORMAT.format(mSchedule.getScheduleStart()));
    }

    @Override
    public void onStartTimeSet(int hourOfDay, int minute, int second) {
        Date startTime = updateTimeFromBase(mSchedule.getScheduleStart(),
                hourOfDay, minute, second);
        mSchedule.setScheduleStart(startTime);
        mView.updateStartTimeDisplay(TIME_FORMAT.format(mSchedule.getScheduleStart()));
    }

    @Override
    public void onEndDateSet(int year, int monthOfYear, int dayOfMonth) {
        Date endDate = updateDateFromBase(mSchedule.getScheduleEnd(),
                year, monthOfYear, dayOfMonth);
        mSchedule.setScheduleEnd(endDate);
        mView.updateEndDateDisplay(DATE_FORMAT.format(mSchedule.getScheduleEnd()));
    }

    @Override
    public void onEndTimeSet(int hourOfDay, int minute, int second) {
        Date endTime = updateTimeFromBase(mSchedule.getScheduleEnd(),
                hourOfDay, minute, second);
        mSchedule.setScheduleEnd(endTime);
        mView.updateEndTimeDisplay(TIME_FORMAT.format(mSchedule.getScheduleEnd()));
    }

    @Override
    public void onAlarmTypeSet(@ScheduleModel.AlarmType String alarmType) {
        Log.v(LOG_TAG, "onAlarmTypeSet(): alarmType = " + alarmType);
        mSchedule.setAlarmType(alarmType);

        Date alarmTime = getAlarmTimeByType(mSchedule.getScheduleStart(), alarmType);
        mSchedule.setAlarmTime(alarmTime);

        mView.updateAlarmTypeDisplay(
                ScheduleUtility.getAlarmTypeText(mSchedule.getAlarmType()));
    }

    @Override
    public void onScheduleTypeSet(@ScheduleModel.AlarmType String scheduleType) {
        Log.v(LOG_TAG, "onScheduleTypeSet(): scheduleType = " + scheduleType);
        mSchedule.setType(scheduleType);
        mView.updateScheduleTypeDisplay(
                ScheduleUtility.getScheduleTypeText(mSchedule.getType()));
    }

    @Override
    public void onNoteSet(String note) {
        mSchedule.setNote(note);
    }

    @Override
    public void resume() {}

    @Override
    public void pause() {}

    @Override
    public void destroy() {
        mView = null;
        mUpdateUseCase.unsubscribe();
    }

    @Override
    public void onSave() {
        mUpdateUseCase.execute(
                mSchedule.getId(), mContentValueMapper.transform(mSchedule),
                new UpdateScheduleSubscriber());
    }

    private void updateScheduleToView(ScheduleModel schedule) {
        mView.updateStartDateDisplay(DATE_FORMAT.format(schedule.getScheduleStart()));
        mView.updateEndDateDisplay(DATE_FORMAT.format(schedule.getScheduleEnd()));

        mView.updateStartTimeDisplay(TIME_FORMAT.format(schedule.getScheduleStart()));
        mView.updateEndTimeDisplay(TIME_FORMAT.format(schedule.getScheduleEnd()));

        mView.updateAlarmTypeDisplay(
                ScheduleUtility.getAlarmTypeText(schedule.getAlarmType()));

        mView.updateScheduleTypeDisplay(
                ScheduleUtility.getScheduleTypeText(schedule.getType()));

        mView.updateScheduleTitle(schedule.getTitle());
        mView.updateScheduleNote(schedule.getNote());
    }

    private void loadSchedule() {
        mQueryUseCase.execute(new ScheduleDetailsSubscriber());
    }

    private final class ScheduleDetailsSubscriber extends DefaultSubscriber<Cursor> {

        @Override public void onCompleted() {}

        @Override public void onError(Throwable e) {}

        @Override public void onNext(Cursor cursor) {
            cursor.moveToFirst();
            ScheduleModel scheduleModel = mModelMapper.transform(cursor);
            Log.v(LOG_TAG, "onNext(): scheduleModel = " + scheduleModel);
            cursor.close();
            mSchedule = scheduleModel;
            updateScheduleToView(scheduleModel);
        }
    }

    private final class UpdateScheduleSubscriber extends DefaultSubscriber<Integer> {

        @Override public void onCompleted() {}

        @Override public void onError(Throwable e) {}

        @Override public void onNext(Integer updated) {
            Log.v(LOG_TAG, "onNext(): updated = " + updated);
        }
    }

    private Date getAlarmTimeByType(Date schedule, @ScheduleModel.AlarmType String alarmType) {
        switch (alarmType) {
            case ScheduleModel.ALARM_10_MINUTES_BEFORE:
                return new Date(schedule.getTime() - MILLISECONDS_IN_10_MINUTES);
            case ScheduleModel.ALARM_30_MINUTES_BEFORE:
                return new Date(schedule.getTime() - MILLISECONDS_IN_30_MINUTES);
            case ScheduleModel.ALARM_1_HOUR_BEFORE:
                return new Date(schedule.getTime() - MILLISECONDS_IN_1_HOUR);
            default:
                return new Date(schedule.getTime());
        }
    }

    private Date updateDateFromBase(Date base, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        return calendar.getTime();
    }

    private Date updateTimeFromBase(Date base, int hourOfDay, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);

        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);

        return calendar.getTime();
    }


}