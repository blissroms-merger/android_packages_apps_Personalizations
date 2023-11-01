/*
 * Copyright (C) 2016-2022 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rising.settings.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.rising.settings.preferences.SystemSettingListPreference;
import com.rising.settings.preferences.SystemSettingSeekBarPreference;
import com.rising.settings.utils.DeviceUtils;
import com.rising.settings.utils.TelephonyUtils;

import lineageos.providers.LineageSettings;

import java.util.List;

import com.android.internal.util.rising.systemUtils;

@SearchIndexable
public class StatusBar extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "StatusBar";

    private static final String QUICK_PULLDOWN = "qs_quick_pulldown";
    //private static final String KEY_SHOW_ROAMING = "roaming_indicator_icon";
    //private static final String KEY_SHOW_FOURG = "show_fourg_icon";
    //private static final String KEY_SHOW_DATA_DISABLED = "data_disabled_icon";
    //private static final String KEY_USE_OLD_MOBILETYPE = "use_old_mobiletype";
    private static final String KEY_STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String KEY_STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String KEY_STATUS_BAR_BATTERY_TEXT_CHARGING = "status_bar_battery_text_charging";

    private static final String KEY_STATUS_BAR_PRIVACY_CAMERA = "enable_camera_privacy_indicator";
    private static final String KEY_STATUS_BAR_PRIVACY_LOC = "enable_location_privacy_indicator";
    private static final String KEY_STATUS_BAR_PRIVACY_MEDIA = "enable_projection_privacy_indicator";

    private static final int PULLDOWN_DIR_NONE = 0;
    private static final int PULLDOWN_DIR_RIGHT = 1;
    private static final int PULLDOWN_DIR_LEFT = 2;
    //private static final int PULLDOWN_DIR_ALWAYS = 3;

    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_TEXT = 4;
    private static final int BATTERY_STYLE_HIDDEN = 5;
    private static final int BATTERY_STYLE_IOS16 = 18;

    private static final int BATTERY_PERCENT_HIDDEN = 0;
    private static final int BATTERY_PERCENT_INSIDE = 1;
    private static final int BATTERY_PERCENT_RIGHT = 2;
    private static final int BATTERY_PERCENT_LEFT = 3;

    private SystemSettingListPreference mQuickPulldown;
    private SystemSettingListPreference mBatteryPercent;
    private SystemSettingListPreference mBatteryStyle;
    //private SwitchPreference mShowRoaming;
    //private SwitchPreference mShowFourg;
    //private SwitchPreference mDataDisabled;
    //private SwitchPreference mOldMobileType;
    private SwitchPreference mBatteryTextCharging;
    private Preference mCombinedSignalIcons;
    private Preference mPrivacyCam;
    private Preference mPrivacyLoc;
    private Preference mPrivacyMedia;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rising_settings_statusbar);

        ContentResolver resolver = getActivity().getContentResolver();
        Context mContext = getActivity();

        final PreferenceScreen prefScreen = getPreferenceScreen();

        //mShowRoaming = (SwitchPreference) findPreference(KEY_SHOW_ROAMING);
        //mShowFourg = (SwitchPreference) findPreference(KEY_SHOW_FOURG);
        //mDataDisabled = (SwitchPreference) findPreference(KEY_SHOW_DATA_DISABLED);
        //mOldMobileType = (SwitchPreference) findPreference(KEY_USE_OLD_MOBILETYPE);

        //if (!TelephonyUtils.isVoiceCapable(getActivity())) {
           // prefScreen.removePreference(mShowRoaming);
            //prefScreen.removePreference(mShowFourg);
           // prefScreen.removePreference(mDataDisabled);
           // prefScreen.removePreference(mOldMobileType);
       // } else {
            //boolean mConfigUseOldMobileType = mContext.getResources().getBoolean(
              //      com.android.internal.R.bool.config_useOldMobileIcons);
           // boolean showing = Settings.System.getIntForUser(resolver,
              //      Settings.System.USE_OLD_MOBILETYPE,
             //       mConfigUseOldMobileType ? 1 : 0, UserHandle.USER_CURRENT) != 0;
            //mOldMobileType.setChecked(showing);
        //}

        int batterystyle = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
        int batterypercent = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT);

        mBatteryStyle = (SystemSettingListPreference) findPreference(KEY_STATUS_BAR_BATTERY_STYLE);
        mBatteryStyle.setOnPreferenceChangeListener(this);

        mBatteryPercent = (SystemSettingListPreference) findPreference(KEY_STATUS_BAR_SHOW_BATTERY_PERCENT);
        mBatteryPercent.setOnPreferenceChangeListener(this);

        handleBatteryPercent(batterystyle, batterypercent);

        mBatteryTextCharging = (SwitchPreference) findPreference(KEY_STATUS_BAR_BATTERY_TEXT_CHARGING);
        mBatteryTextCharging.setEnabled(batterystyle != BATTERY_STYLE_TEXT &&
                (batterypercent == BATTERY_PERCENT_INSIDE || batterypercent == BATTERY_PERCENT_HIDDEN));

        mQuickPulldown =
                (SystemSettingListPreference) findPreference(QUICK_PULLDOWN);
        mQuickPulldown.setOnPreferenceChangeListener(this);
        updateQuickPulldownSummary(mQuickPulldown.getIntValue(0));

        // Adjust status bar preferences for RTL
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            mQuickPulldown.setEntries(R.array.status_bar_quick_qs_pulldown_entries_rtl);
            mQuickPulldown.setEntryValues(R.array.status_bar_quick_qs_pulldown_values_rtl);
        }
        
        mCombinedSignalIcons = findPreference("persist.sys.flags.combined_signal_icons");
        mCombinedSignalIcons.setOnPreferenceChangeListener(this);

        mPrivacyCam = findPreference(KEY_STATUS_BAR_PRIVACY_CAMERA);
        mPrivacyCam.setOnPreferenceChangeListener(this);
        mPrivacyLoc = findPreference(KEY_STATUS_BAR_PRIVACY_LOC);
        mPrivacyLoc.setOnPreferenceChangeListener(this);
        mPrivacyMedia = findPreference(KEY_STATUS_BAR_PRIVACY_MEDIA);
        mPrivacyMedia.setOnPreferenceChangeListener(this);

    }

    private void handleBatteryPercent(int batterystyle, int batterypercent) {
        if (batterystyle < BATTERY_STYLE_TEXT) {
            mBatteryPercent.setEntries(R.array.status_bar_battery_percent_entries);
            mBatteryPercent.setEntryValues(R.array.status_bar_battery_percent_values);;
        }
        else {
            mBatteryPercent.setEntries(R.array.status_bar_battery_percent_no_text_inside_entries);
            mBatteryPercent.setEntryValues(R.array.status_bar_battery_percent_no_text_inside_values);
            if (batterystyle == BATTERY_STYLE_IOS16) {
                if (batterypercent != BATTERY_PERCENT_INSIDE) {
                    batterypercent = BATTERY_PERCENT_INSIDE;
                    mBatteryPercent.setValueIndex(BATTERY_PERCENT_INSIDE);
                }
            } else if (batterypercent == BATTERY_PERCENT_INSIDE) {
                batterypercent = BATTERY_PERCENT_HIDDEN;
                mBatteryPercent.setValueIndex(BATTERY_PERCENT_HIDDEN);
            }
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                    batterypercent, UserHandle.USER_CURRENT);
        }

        mBatteryPercent.setEnabled(
                batterystyle != BATTERY_STYLE_TEXT &&
                batterystyle != BATTERY_STYLE_HIDDEN &&
                batterystyle != BATTERY_STYLE_IOS16);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContentResolver();
        if (preference == mBatteryStyle) {
            int batterystyle = Integer.parseInt((String) newValue);
            int batterypercent = Settings.System.getIntForUser(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT);
            handleBatteryPercent(batterystyle, batterypercent);
            mBatteryTextCharging.setEnabled(batterystyle != BATTERY_STYLE_TEXT &&
                    (batterypercent == BATTERY_PERCENT_INSIDE || batterypercent == BATTERY_PERCENT_HIDDEN));
            return true;
        } else if (preference == mBatteryPercent) {
            int batterypercent = Integer.parseInt((String) newValue);
            int batterystyle = Settings.System.getIntForUser(resolver,
                    Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
            mBatteryTextCharging.setEnabled(batterystyle != BATTERY_STYLE_TEXT &&
                    (batterypercent == BATTERY_PERCENT_INSIDE || batterypercent == BATTERY_PERCENT_HIDDEN));
            return true;
        } else if (preference == mQuickPulldown) {
            int pulldown = Integer.parseInt((String) newValue);
            updateQuickPulldownSummary(pulldown);
            return true;
        } else if (preference == mCombinedSignalIcons) {
            systemUtils.showSystemUIRestartDialog(getActivity());
            return true;
        } else if (preference == mPrivacyCam || preference == mPrivacyLoc || preference == mPrivacyMedia) {
            systemUtils.showSystemRestartDialog(getActivity());
            return true;
        }
        return false;
    }

    private void updateQuickPulldownSummary(int value) {
        String summary="";
        switch (value) {
            case PULLDOWN_DIR_NONE:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_off);
                break;

            case PULLDOWN_DIR_LEFT:
            case PULLDOWN_DIR_RIGHT:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_summary,
                    getResources().getString(
                        (value == PULLDOWN_DIR_LEFT) ^
                        (getResources().getConfiguration().getLayoutDirection()
                            == View.LAYOUT_DIRECTION_RTL)
                        ? R.string.status_bar_quick_qs_pulldown_summary_left
                        : R.string.status_bar_quick_qs_pulldown_summary_right));
                break;
        }
        mQuickPulldown.setSummary(summary);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_statusbar) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    //if (!TelephonyUtils.isVoiceCapable(context)) {
                       // keys.add(KEY_SHOW_ROAMING);
                      //  keys.add(KEY_SHOW_FOURG);
                      //  keys.add(KEY_SHOW_DATA_DISABLED);
                      //  keys.add(KEY_USE_OLD_MOBILETYPE);
                   // }

                    return keys;
                }
            };
}
