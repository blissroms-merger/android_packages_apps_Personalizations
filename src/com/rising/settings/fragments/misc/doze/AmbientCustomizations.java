/**
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rising.settings.fragments.misc.doze;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.SwitchPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import android.provider.SearchIndexableResource;

import com.rising.settings.preferences.SystemSettingSeekBarPreference;
import com.rising.settings.preferences.SystemSettingListPreference;
import com.rising.settings.preferences.SystemSettingEditTextPreference;
import com.rising.settings.preferences.colorpicker.SystemSettingColorPickerPreference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AmbientCustomizations extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String AMBIENT_TEXT_STRING = "ambient_text_string";
    private static final String AMBIENT_TEXT_ALIGNMENT = "ambient_text_alignment";
    private static final String AMBIENT_TEXT_TYPE_COLOR = "ambient_text_type_color";
    private static final String AMBIENT_TEXT_COLOR = "ambient_text_color";
    private static final String FILE_AMBIENT_SELECT = "file_ambient_select";

    private static final int REQUEST_PICK_IMAGE = 0;

    private SystemSettingEditTextPreference mAmbientText;
    private SystemSettingListPreference mAmbientTextAlign;
    private SystemSettingListPreference mAmbientTextTypeColor;
    private SystemSettingColorPickerPreference mAmbientTextColor;

    private Preference mAmbientImage;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.ambient_customization);
        final PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        Resources resources = getResources();

        // set ambient text alignment
        mAmbientTextAlign = (SystemSettingListPreference) findPreference(AMBIENT_TEXT_ALIGNMENT);
        int align = Settings.System.getInt(resolver,
                Settings.System.AMBIENT_TEXT_ALIGNMENT, 3);
        mAmbientTextAlign.setValue(String.valueOf(align));
        mAmbientTextAlign.setSummary(mAmbientTextAlign.getEntry());
        mAmbientTextAlign.setOnPreferenceChangeListener(this);

        // ambient text color type
        mAmbientTextTypeColor = (SystemSettingListPreference) findPreference(AMBIENT_TEXT_TYPE_COLOR);
        mAmbientTextTypeColor.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.AMBIENT_TEXT_TYPE_COLOR, 0)));
        mAmbientTextTypeColor.setSummary(mAmbientTextTypeColor.getEntry());
        mAmbientTextTypeColor.setOnPreferenceChangeListener(this);

        mAmbientTextColor = (SystemSettingColorPickerPreference) findPreference(AMBIENT_TEXT_COLOR);
        mAmbientTextColor.setOnPreferenceChangeListener(this);
        int ambientTextColor = Settings.System.getInt(getContentResolver(),
                Settings.System.AMBIENT_TEXT_COLOR, 0xFF3980FF);
        String ambientTextColorHex = String.format("#%08x", (0xFF3980FF & ambientTextColor));
        if (ambientTextColorHex.equals("#ff3980ff")) {
            mAmbientTextColor.setSummary(R.string.default_string);
        } else {
            mAmbientTextColor.setSummary(ambientTextColorHex);
        }
        mAmbientTextColor.setNewPreviewColor(ambientTextColor);

        mAmbientImage = findPreference(FILE_AMBIENT_SELECT);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mAmbientText) {
            String value = (String) newValue;
            Settings.System.putString(resolver,
                    Settings.System.AMBIENT_TEXT_STRING, value);
            return true;
        } else if (preference == mAmbientTextAlign) {
            int align = Integer.valueOf((String) newValue);
            int index = mAmbientTextAlign.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.AMBIENT_TEXT_ALIGNMENT, align);
            mAmbientTextAlign.setSummary(mAmbientTextAlign.getEntries()[index]);
            return true;
         } else if (preference == mAmbientTextTypeColor) {
             int value = Integer.valueOf((String) newValue);
             int vIndex = mAmbientTextTypeColor.findIndexOfValue((String) newValue);
             mAmbientTextTypeColor.setSummary(mAmbientTextTypeColor.getEntries()[vIndex]);
             Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.AMBIENT_TEXT_TYPE_COLOR, value);
            if (value == 2) {
                mAmbientTextColor.setEnabled(true);
            } else {
                mAmbientTextColor.setEnabled(false);
            }
            return true;
        } else if (preference == mAmbientTextColor) {
            String hex = SystemSettingColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#ff3980ff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = SystemSettingColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.AMBIENT_TEXT_COLOR, intHex);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mAmbientImage) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }

            final Uri imageUri = result.getData();
            if (imageUri != null) {
                Settings.System.putStringForUser(getContentResolver(), Settings.System.AMBIENT_CUSTOM_IMAGE, imageUri.toString(), UserHandle.USER_CURRENT);
            }
        }
    }


    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.ambient_customization;
                    result.add(sir);
                    return result;
                }

          @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
    };
}
