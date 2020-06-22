package com.inledco.exoterra.main.groups;

import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.alibaba.fastjson.JSON;
import com.inledco.exoterra.GlobalSettings;
import com.inledco.exoterra.R;
import com.inledco.exoterra.aliot.AliotServer;
import com.inledco.exoterra.aliot.HttpCallback;
import com.inledco.exoterra.aliot.UserApi;
import com.inledco.exoterra.base.BaseFragment;
import com.inledco.exoterra.manager.GroupManager;
import com.inledco.exoterra.util.FavouriteUtil;
import com.inledco.exoterra.util.GroupUtil;
import com.inledco.exoterra.util.TimeFormatUtil;
import com.inledco.exoterra.view.AdvancedTextInputEditText;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class AddHabitatFragment extends BaseFragment {
    private TextView add_habitat_systime;
    private TextInputLayout add_habitat_til;
    private AdvancedTextInputEditText add_habitat_name;
    private AdvancedTextInputEditText add_habitat_time;
    private AdvancedTextInputEditText add_habitat_sunrise;
    private AdvancedTextInputEditText add_habitat_sunset;
    private Switch add_habitat_favourite;
    private Button add_habitat_back;
    private Button add_habitat_save;

    private String mIconName = GroupUtil.getDefaultIconName();

    private final int mOffset = TimeZone.getDefault().getRawOffset() / 60000;
    private final BroadcastReceiver mTimeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_TIME_TICK:
                    refreshTime();
                    break;
            }
        }
    };

    private DateFormat mDateFormat;
    private DateFormat mTimeFormat;

    private int mZone = TimeZone.getDefault().getRawOffset()/60000;
    private int mSunrise = 360;
    private int mSunset = 1080;

    private static final String KEY_FAVOURITE = "favourite";

    public static AddHabitatFragment newInstance(final boolean favourite) {
        Bundle args = new Bundle();
        args.putBoolean(KEY_FAVOURITE, favourite);
        AddHabitatFragment fragment = new AddHabitatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        initData();
        initEvent();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mTimeChangeReceiver);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_add_habitat;
    }

    @Override
    protected void initView(View view) {
        add_habitat_systime = view.findViewById(R.id.add_habitat_systime);
        add_habitat_til = view.findViewById(R.id.add_habitat_til);
        add_habitat_name = view.findViewById(R.id.add_habitat_name);
        add_habitat_time = view.findViewById(R.id.add_habitat_time);
        add_habitat_sunrise = view.findViewById(R.id.add_habitat_sunrise);
        add_habitat_sunset = view.findViewById(R.id.add_habitat_sunset);
        add_habitat_favourite = view.findViewById(R.id.add_habitat_favourite);
        add_habitat_back = view.findViewById(R.id.add_habitat_back);
        add_habitat_save = view.findViewById(R.id.add_habitat_save);

        add_habitat_name.requestFocus();
        add_habitat_name.bindTextInputLayout(add_habitat_til);
        add_habitat_name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_type_1, 0, R.drawable.ic_edit_white_24dp, 0);
        add_habitat_time.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_edit_white_24dp, 0);
        add_habitat_sunrise.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_edit_white_24dp, 0);
        add_habitat_sunset.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_edit_white_24dp, 0);
    }

    @Override
    protected void initData() {
        Bundle args = getArguments();
        if (args != null) {
            add_habitat_favourite.setChecked(args.getBoolean(KEY_FAVOURITE));
        }
        mDateFormat = GlobalSettings.getDateTimeFormat();
        mTimeFormat = GlobalSettings.getTimeFormat();
        add_habitat_sunrise.setText(TimeFormatUtil.formatMinutesTime(mTimeFormat, mSunrise));
        add_habitat_sunset.setText(TimeFormatUtil.formatMinutesTime(mTimeFormat, mSunset));
        refreshTime();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        getActivity().registerReceiver(mTimeChangeReceiver, filter);
    }

    @Override
    protected void initEvent() {
        add_habitat_name.setDrawableRightClickListener(new AdvancedTextInputEditText.DrawableRightClickListener() {
            @Override
            public void onDrawableRightClick() {
                showGroupIconDialog();
            }
        });
        add_habitat_time.setDrawableRightClickListener(new AdvancedTextInputEditText.DrawableRightClickListener() {
            @Override
            public void onDrawableRightClick() {
                long time = System.currentTimeMillis();
                int habitatTime = (int) ((time / 60000 + mZone) % 1440);
                showTimePickerDialog(habitatTime, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        int currTime = (int) ((System.currentTimeMillis() / 60000) % 1440);
                        int setTime = hourOfDay*60 + minute;
                        int zone = setTime - currTime;
                        if (zone < -720) {
                            zone += 1440;
                        } else if (zone > 720) {
                            zone -= 1440;
                        }
                        mZone = zone;
                        refreshTime();
                    }
                });
            }
        });

        add_habitat_sunrise.setDrawableRightClickListener(new AdvancedTextInputEditText.DrawableRightClickListener() {
            @Override
            public void onDrawableRightClick() {
                showTimePickerDialog(mSunrise, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mSunrise = hourOfDay*60+minute;
                        long time = ((1440+mSunrise - mOffset) % 1440) * 60000;
                        add_habitat_sunrise.setText(mTimeFormat.format(time));
                    }
                });
            }
        });

        add_habitat_sunset.setDrawableRightClickListener(new AdvancedTextInputEditText.DrawableRightClickListener() {
            @Override
            public void onDrawableRightClick() {
                showTimePickerDialog(mSunset, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mSunset = hourOfDay*60+minute;
                        long time = ((1440+mSunset - mOffset) % 1440) * 60000;
                        add_habitat_sunset.setText(mTimeFormat.format(time));
                    }
                });
            }
        });

        add_habitat_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        add_habitat_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = add_habitat_name.getText().toString();
                if (TextUtils.isEmpty(name)) {
                    add_habitat_til.setError(getString(R.string.input_empty));
                    return;
                }
                createHome(name, mIconName, mZone, mSunrise, mSunset);
            }
        });
    }

    private void refreshTime() {
        long current = System.currentTimeMillis();
        long time = current + (mZone - mOffset) * 60000;
        add_habitat_systime.setText(mDateFormat.format(current));
        add_habitat_time.setText(mDateFormat.format(time));
    }

    private void showTimePickerDialog(int time, final TimePickerDialog.OnTimeSetListener listener) {
        TimePickerDialog  dialog = new TimePickerDialog(getContext(), listener, time/60, time%60, GlobalSettings.is24HourFormat());
        dialog.show();
    }

    private void createHome(final String name, final String iconName, final int zone, final int sunrise, final int sunset) {
        Map<String, Integer> map = new HashMap<>();
        map.put("zone", zone);
        map.put("sunrise", sunrise);
        map.put("sunset", sunset);
        final UserApi.GroupRequest request = new UserApi.GroupRequest();
        request.name = name;
        request.remark1 = JSON.toJSONString(map);
        request.remark2 = iconName;
        AliotServer.getInstance().createGroup(request, new HttpCallback<UserApi.GroupResponse>() {
            @Override
            public void onError(String error) {
                dismissLoadDialog();
                showToast(error);
            }

            @Override
            public void onSuccess(UserApi.GroupResponse result) {
                Log.e(TAG, "onSuccess: " + JSON.toJSONString(result));
                if (add_habitat_favourite.isChecked()) {
                    FavouriteUtil.addFavourite(getContext(), result.data.groupid);
                }
                GroupManager.getInstance().getGroups();
                dismissLoadDialog();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                });
            }
        });
        showLoadDialog();
    }

    private void showGroupIconDialog() {
        GroupIconDialog dialog = new GroupIconDialog(getContext()) {
            @Override
            public void onChoose(String name, int res) {
                mIconName = name;
                add_habitat_name.setCompoundDrawablesRelativeWithIntrinsicBounds(res, 0, R.drawable.ic_edit_white_24dp, 0);
            }
        };
        dialog.init(mIconName);
        dialog.show();
    }
}
