/*
 * Copyright (C) 2013 Android Open Kang Project
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

package com.android.internal.util.pac;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import com.android.internal.R;
import com.android.internal.statusbar.IStatusBarService;

import java.net.URISyntaxException;
import java.util.List;

import static com.android.internal.util.pac.AwesomeConstants.AwesomeConstant;
import static com.android.internal.util.pac.AwesomeConstants.fromString;

import com.android.internal.util.cm.ActionUtils;
import com.android.internal.util.cm.TorchConstants;

public class AwesomeAction {

    public final static String TAG = "AwesomeAction";
    private final static String SysUIPackage = "com.android.systemui";
    public static final String NULL_ACTION = AwesomeConstant.ACTION_NULL.value();

    private static final int STANDARD_FLAGS = KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_KEEP_TOUCH_MODE;
    private static final int CURSOR_FLAGS = KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;

    private static int mCurrentUserId = 0;

    private AwesomeAction() {
    }

    public static void setCurrentUser(int newUserId) {
        mCurrentUserId = newUserId;
    }

    public static boolean launchAction(final Context mContext, final String action) {
        if (TextUtils.isEmpty(action) || action.equals(NULL_ACTION)) {
            return false;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                AwesomeConstant AwesomeEnum = fromString(action);
                AudioManager am;
                switch (AwesomeEnum) {
                    case ACTION_ASSIST:
                        Intent intent = new Intent(Intent.ACTION_ASSIST);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (isIntentAvailable(mContext, intent))
                            mContext.startActivity(intent);
                        break;
                    case ACTION_HOME:
                        IWindowManager mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
                        try {
                            mWindowManagerService.sendHomeAction();
                        } catch (RemoteException e) {
                            Log.e(TAG, "HOME ACTION FAILED");
                        }
                        break;
                    case ACTION_BACK:
                        triggerVirtualKeypress(KeyEvent.KEYCODE_BACK, STANDARD_FLAGS);
                        break;
                    case ACTION_MENU:
                        triggerVirtualKeypress(KeyEvent.KEYCODE_MENU, STANDARD_FLAGS);
                        break;
                    case ACTION_SEARCH:
                        triggerVirtualKeypress(KeyEvent.KEYCODE_SEARCH, STANDARD_FLAGS);
                        break;
                    case ACTION_KILL:
                        KillApp onKillApp = new KillApp(mCurrentUserId, mContext);
                        mHandler.removeCallbacks(onKillApp);
                        mHandler.post(onKillApp);
                        break;
                    case ACTION_VIB:
                        am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        if (am != null) {
                            if (am.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
                                am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                                Vibrator vib = (Vibrator) mContext
                                        .getSystemService(Context.VIBRATOR_SERVICE);
                                if (vib != null) {
                                    vib.vibrate(50);
                                }
                            } else {
                                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                                ToneGenerator tg = new ToneGenerator(
                                        AudioManager.STREAM_NOTIFICATION,
                                        (int) (ToneGenerator.MAX_VOLUME * 0.85));
                                if (tg != null) {
                                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                                }
                            }
                        }
                        break;
                    case ACTION_SILENT:
                        am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        if (am != null) {
                            if (am.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            } else {
                                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                                ToneGenerator tg = new ToneGenerator(
                                        AudioManager.STREAM_NOTIFICATION,
                                        (int) (ToneGenerator.MAX_VOLUME * 0.85));
                                if (tg != null) {
                                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                                }
                            }
                        }
                        break;
                    case ACTION_SILENT_VIB:
                        am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        if (am != null) {
                            if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                                Vibrator vib = (Vibrator) mContext
                                        .getSystemService(Context.VIBRATOR_SERVICE);
                                if (vib != null) {
                                    vib.vibrate(50);
                                }
                            } else if (am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            } else {
                                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                                ToneGenerator tg = new ToneGenerator(
                                        AudioManager.STREAM_NOTIFICATION,
                                        (int) (ToneGenerator.MAX_VOLUME * 0.85));
                                if (tg != null) {
                                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                                }
                            }
                        }
                        break;
                    case ACTION_POWER:
                        triggerVirtualKeypress(KeyEvent.KEYCODE_POWER, STANDARD_FLAGS);
                        break;
                    case ACTION_IME:
                        mContext.sendBroadcast(new Intent(
                                "android.settings.SHOW_INPUT_METHOD_PICKER"));
                        break;
                    case ACTION_TORCH:
                        Intent intentTorch = new Intent(TorchConstants.ACTION_TOGGLE_STATE);
                        mContext.sendBroadcast(intentTorch);
                        break;
                    case ACTION_TODAY:
                        long startMillis = System.currentTimeMillis();
                        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                        builder.appendPath("time");
                        ContentUris.appendId(builder, startMillis);
                        Intent intentToday = new Intent(Intent.ACTION_VIEW)
                                .setData(builder.build());
                        intentToday.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intentToday);
                        break;
                    case ACTION_CLOCKOPTIONS:
                        Intent intentClock = new Intent(Intent.ACTION_QUICK_CLOCK);
                        intentClock.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intentClock);
                        break;
                    case ACTION_EVENT:
                        Intent intentEvent = new Intent(Intent.ACTION_INSERT)
                                .setData(Events.CONTENT_URI);
                        intentEvent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intentEvent);
                        break;
                    case ACTION_VOICEASSIST:
                        Intent intentVoice = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
                        intentVoice.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intentVoice);
                        break;
                    case ACTION_ALARM:
                        Intent intentAlarm = new Intent(AlarmClock.ACTION_SET_ALARM);
                        intentAlarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intentAlarm);
                        break;
                    case ACTION_LAST_APP:
                        ActionUtils.switchToLastApp(mContext, mCurrentUserId);
                        break;
                    case ACTION_NOTIFICATIONS:
                        try {
                            IStatusBarService.Stub.asInterface(
                                    ServiceManager.getService(mContext.STATUS_BAR_SERVICE))
                                    .expandNotificationsPanel();
                        } catch (RemoteException e) {
                            // A RemoteException is like a cold
                            // Let's hope we don't catch one!
                        }
                        break;
                    case ACTION_APP:
                        try {
                            Intent intentapp = Intent.parseUri(action, 0);
                            intentapp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(intentapp);
                        } catch (URISyntaxException e) {
                            Log.e(TAG, "URISyntaxException: [" + action + "]");
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, "ActivityNotFound: [" + action + "]");
                        }
                        break;
                    case ACTION_CAMERA:
                        Intent camera = new Intent("android.media.action.IMAGE_CAPTURE");
                        camera.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(camera);
                        break;
                    case ACTION_DPAD_LEFT:
                        triggerVirtualKeypress(KeyEvent.KEYCODE_DPAD_LEFT, CURSOR_FLAGS);
                        break;
                    case ACTION_DPAD_RIGHT:
                        triggerVirtualKeypress(KeyEvent.KEYCODE_DPAD_RIGHT, CURSOR_FLAGS);
                        break;
                }

            }
        }).start();

        return true;
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private static void triggerVirtualKeypress(int keycode, int flags) {
        KeyUp onInjectKey_Up = new KeyUp(keycode, flags);
        KeyDown onInjectKey_Down = new KeyDown(keycode, flags);
        mHandler.removeCallbacks(onInjectKey_Down);
        mHandler.removeCallbacks(onInjectKey_Up);
        mHandler.post(onInjectKey_Down);
        mHandler.postDelayed(onInjectKey_Up, 10);
    }

    public static class KillApp implements Runnable {
        private Context mContext;
        private int mCurrentUserId;

        public KillApp(int UserId, Context context) {
            this.mCurrentUserId = UserId;
            this.mContext = context;
        }

        public void run() {
            if (ActionUtils.killForegroundApp(mContext, mCurrentUserId)) {
                Toast.makeText(mContext, R.string.app_killed_message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class KeyDown implements Runnable {
        private int mInjectKeyCode;
        private int mFlags;

        public KeyDown(int keycode, int flags) {
            this.mInjectKeyCode = keycode;
            this.mFlags = flags;
        }

        public void run() {
            final KeyEvent ev = new KeyEvent(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_DOWN, mInjectKeyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0, mFlags, InputDevice.SOURCE_KEYBOARD);
            InputManager.getInstance().injectInputEvent(ev,
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    }

    public static class KeyUp implements Runnable {
        private int mInjectKeyCode;
        private int mFlags;

        public KeyUp(int keycode, int flags) {
            this.mInjectKeyCode = keycode;
            this.mFlags = flags;
        }

        public void run() {
            final KeyEvent ev = new KeyEvent(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP, mInjectKeyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0, mFlags, InputDevice.SOURCE_KEYBOARD);
            InputManager.getInstance().injectInputEvent(ev,
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    }

    private static Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            }
        }
    };
}
