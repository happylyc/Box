package com.github.tvbox.osc.base;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;
import com.blankj.utilcode.util.ActivityUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LocaleHelper;
import com.kingja.loadsir.callback.Callback;
import com.kingja.loadsir.core.LoadService;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import me.jessyan.autosize.AutoSizeCompat;
import me.jessyan.autosize.internal.CustomAdapt;
import xyz.doikki.videoplayer.util.CutoutUtil;

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public abstract class BaseActivity extends AppCompatActivity implements CustomAdapt {
    protected Context mContext;
    private LoadService mLoadService;

    private static float screenRatio = -100.0f;

    // takagen99 : Fix for Locale change not persist on higher Android version
    @Override
    protected void attachBaseContext(Context base) {
        Context newBase = base;
        if (App.viewPump != null) {
            newBase = ViewPumpContextWrapper.wrap(base, App.viewPump);
        }

        if (Hawk.get(HawkConfig.HOME_LOCALE, 0) == 0) {
            super.attachBaseContext(LocaleHelper.onAttach(newBase, "zh"));
        } else {
            super.attachBaseContext(LocaleHelper.onAttach(newBase, ""));
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            if (screenRatio < 0) {
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int screenWidth = dm.widthPixels;
                int screenHeight = dm.heightPixels;
                screenRatio = (float) Math.max(screenWidth, screenHeight) / (float) Math.min(screenWidth, screenHeight);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

        // takagen99 : Set Theme Color
        if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 0) {
            setTheme(R.style.NetfxTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 1) {
            setTheme(R.style.DoraeTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 2) {
            setTheme(R.style.PepsiTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 3) {
            setTheme(R.style.NarutoTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 4) {
            setTheme(R.style.MinionTheme);
        } else if (Hawk.get(HawkConfig.THEME_SELECT, 0) == 5) {
            setTheme(R.style.YagamiTheme);
        } else {
            setTheme(R.style.SakuraTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(getLayoutResID());
        mContext = this;
        CutoutUtil.adaptCutoutAboveAndroidP(mContext, true);
        AppManager.getInstance().addActivity(this);
        init();
        setScreenOn();

        // ================= Password Lock Start =================
        // Normal init completes first, then overlay lock screen on top
        if (this instanceof com.github.tvbox.osc.ui.activity.HomeActivity) {
            boolean isReHome = false;
            if (getIntent() != null && getIntent().getExtras() != null) {
                isReHome = getIntent().getExtras().getBoolean("useCache", false);
            }
            long lastVerify = Hawk.get("password_last_verify", 0L);
            boolean isVerified = (System.currentTimeMillis() - lastVerify) < 0;

            if (!isVerified && !isReHome) {
                showLockOverlay();
            }
        }
        // ================= Password Lock End =================
    }

    private void showLockOverlay() {
        final String CORRECT_PASSWORD = "123456";
        final BaseActivity self = this;

        // Fullscreen black overlay
        final FrameLayout overlay = new FrameLayout(this);
        overlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(0xFF000000);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        container.setLayoutParams(cp);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(32);
        title.setPadding(0, 0, 0, 30);
        container.addView(title);

        TextView msg = new TextView(this);
        msg.setText("Enter password to continue");
        msg.setTextColor(0xFFAAAAAA);
        msg.setTextSize(16);
        msg.setPadding(0, 0, 0, 40);
        container.addView(msg);

        final EditText input = new EditText(this);
        input.setHint("Password");
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF888888);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setGravity(Gravity.CENTER);
        input.setBackgroundColor(0xFF333333);
        input.setPadding(40, 25, 40, 25);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(600, LinearLayout.LayoutParams.WRAP_CONTENT);
        ip.setMargins(100, 0, 100, 10);
        input.setLayoutParams(ip);
        container.addView(input);

        final TextView errorMsg = new TextView(this);
        errorMsg.setText("");
        errorMsg.setTextColor(0xFFFF4444);
        errorMsg.setTextSize(14);
        errorMsg.setPadding(0, 5, 0, 5);
        container.addView(errorMsg);

        Button confirmBtn = new Button(this);
        confirmBtn.setText("OK");
        confirmBtn.setTextColor(0xFFFFFFFF);
        confirmBtn.setBackgroundColor(0xFFe94560);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, 20, 0, 0);
        confirmBtn.setLayoutParams(bp);
        container.addView(confirmBtn);

        Button exitBtn = new Button(this);
        exitBtn.setText("Exit");
        exitBtn.setTextColor(0xFFCCCCCC);
        exitBtn.setBackgroundColor(0xFF444444);
        LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp2.setMargins(0, 15, 0, 0);
        exitBtn.setLayoutParams(bp2);
        container.addView(exitBtn);

        overlay.addView(container);

        // Add overlay to window root
        final ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(overlay);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pwd = input.getText().toString().trim();
                if (CORRECT_PASSWORD.equals(pwd)) {
                    Hawk.put("password_last_verify", System.currentTimeMillis());
                    rootView.removeView(overlay);
                } else {
                    errorMsg.setText("Wrong password, try again");
                    input.setText("");
                    input.requestFocus();
                }
            }
        });

        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI(true);
        changeWallpaper(false);
    }

    // takagen99 : Check for Gesture or 3-Buttons NavBar
    // 0 : 3-Button NavBar
    // 1 : 2-Button NavBar (Android P)
    // 2 : Gesture full screen
    public static int isEdgeToEdgeEnabled(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android");
        if (resourceId > 0) {
            return resources.getInteger(resourceId);
        }
        return 0;
    }

    public void hideSysBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            //    uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            //    uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    public void vidHideSysBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    public void hideSystemUI(boolean shownavbar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            uiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            uiVisibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            uiVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE;
            uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            if (!shownavbar) {
                uiVisibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                uiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            }
            getWindow().getDecorView().setSystemUiVisibility(uiVisibility);
            // set content behind navigation bar
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    public void showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            uiVisibility &= ~View.SYSTEM_UI_FLAG_LOW_PROFILE;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiVisibility &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            getWindow().getDecorView().setSystemUiVisibility(uiVisibility);
        }
    }

    @Override
    public Resources getResources() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            AutoSizeCompat.autoConvertDensityOfCustomAdapt(super.getResources(), this);
        }
        return super.getResources();
    }

    public boolean hasPermission(String permission) {
        boolean has = true;
        try {
            has = PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return has;
    }

    protected abstract int getLayoutResID();

    protected abstract void init();

    protected void setLoadSir(View view) {
        if (mLoadService == null) {
            mLoadService = LoadSir.getDefault().register(view, new Callback.OnReloadListener() {
                @Override
                public void onReload(View v) {
                }
            });
        }
    }

    protected void showLoading() {
        if (mLoadService != null) {
            mLoadService.showCallback(LoadingCallback.class);
        }
    }

    protected boolean isLoading() {
        if (mLoadService != null && mLoadService.getCurrentCallback() != null) {
            return mLoadService.getCurrentCallback().equals(LoadingCallback.class);
        }
        return false;
    }

    protected void showEmpty() {
        if (null != mLoadService) {
            mLoadService.showCallback(EmptyCallback.class);
        }
    }

    protected void showSuccess() {
        if (null != mLoadService) {
            mLoadService.showSuccess();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().finishActivity(this);
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz) {
        Intent intent = new Intent(mContext, clazz);
        startActivity(intent);
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz, Bundle bundle) {
        if (DetailActivity.class.isAssignableFrom(clazz) && Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0) == 2) {
            //1.重新打开singleTask的页面(关闭小窗) 2.关闭画中画，重进detail再开启画中画会闪退
            ActivityUtils.finishActivity(DetailActivity.class);
        }
        Intent intent = new Intent(mContext, clazz);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    protected String getAssetText(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assets = getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assets.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public float getSizeInDp() {
        return isBaseOnWidth() ? 1280 : 720;
    }

    @Override
    public boolean isBaseOnWidth() {
        return !(screenRatio >= 4.0f);
    }

    public boolean supportsPiPMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public boolean supportsTouch() {
        return getPackageManager().hasSystemFeature("android.hardware.touchscreen");
    }

    public void setScreenBrightness(float amt) {
        WindowManager.LayoutParams lparams = getWindow().getAttributes();
        lparams.screenBrightness = amt;
        getWindow().setAttributes(lparams);
    }

    public void setScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void setScreenOff() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // takagen99: Added Theme Color
    public int getThemeColor() {
        TypedArray a = mContext.obtainStyledAttributes(R.styleable.themeColor);
        int themeColor = a.getColor(R.styleable.themeColor_color_theme, 0);
        return themeColor;
    }

    protected static BitmapDrawable globalWp = null;

    public void changeWallpaper(boolean force) {
        if (!force && globalWp != null) {
            getWindow().setBackgroundDrawable(globalWp);
            return;
        }
        try {
            File wp = new File(getFilesDir().getAbsolutePath() + "/wp");
            if (wp.exists()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(wp.getAbsolutePath(), opts);
                // 从Options中获取图片的分辨率
                int imageHeight = opts.outHeight;
                int imageWidth = opts.outWidth;
                int picHeight = 720;
                int picWidth = 1080;
                int scaleX = imageWidth / picWidth;
                int scaleY = imageHeight / picHeight;
                int scale = Math.max(Math.max(scaleX, scaleY), 1);
                opts.inJustDecodeBounds = false;
                // 采样率
                opts.inSampleSize = scale;
                globalWp = new BitmapDrawable(BitmapFactory.decodeFile(wp.getAbsolutePath(), opts));
            } else {
                globalWp = null;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            globalWp = null;
        }
        if (globalWp != null) {
            getWindow().setBackgroundDrawable(globalWp);
        } else {
            getWindow().setBackgroundDrawableResource(R.drawable.app_bg);
        }
    }
}
