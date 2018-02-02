package top.trumeet.mipushframework.wizard;

import android.app.AppOpsManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.widget.Toast;

import com.android.setupwizardlib.view.NavigationBar;

import top.trumeet.common.Constants;
import top.trumeet.common.push.PushController;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.R;
import top.trumeet.mipushframework.utils.ShellUtils;
import top.trumeet.mipushframework.wizard.fake.FakeBuildActivity;

/**
 * Created by Trumeet on 2017/8/25.
 * @author Trumeet
 */

public class CheckRunInBackgroundActivity extends PushControllerWizardActivity implements NavigationBar.NavigationBarListener {
    private boolean allow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !canFix()) {
            nextPage();
            finish();
            return;
        }
        connect();
    }

    @Override
    public void onResume () {
        super.onResume();
        if (!canFix()) {
            nextPage();
            finish();
        }
        PushController controller = getController();
        if (controller != null && controller.isConnected() && !isConnecting()) {
            mText.setText(Html.fromHtml(getString(R.string.wizard_descr_run_in_background, Build.VERSION.SDK_INT >= 26 ?
                    "" : (Utils.isAppOpsInstalled() ? getString(R.string.run_in_background_rikka_appops) :
                    getString(R.string.run_in_background_appops_root)))));

            int result = controller.checkOp(AppOpsManager.OP_RUN_IN_BACKGROUND);
            allow = (result == AppOpsManager.MODE_ALLOWED);

            if (allow) {
                nextPage();
                finish();
            }
        }
    }

    @Override
    public void onConnected(@NonNull PushController controller,
                            Bundle savedInstanceState) {
        super.onConnected(controller, savedInstanceState);
        layout.getNavigationBar()
                .setNavigationBarListener(this);
        mText.setText(Html.fromHtml(getString(R.string.wizard_descr_run_in_background, (Utils.isAppOpsInstalled() ? getString(R.string.run_in_background_rikka_appops) :
                getString(R.string.run_in_background_appops_root)))));
        layout.setHeaderText(R.string.wizard_title_run_in_background);
        setContentView(layout);

        int result = controller.checkOp(AppOpsManager.OP_RUN_IN_BACKGROUND);
        allow = (result == AppOpsManager.MODE_ALLOWED);

        if (allow) {
            nextPage();
            finish();
        }
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        if (!allow && canFix()) {
            lunchAppOps();
        } else {
            nextPage();
        }
    }

    private void nextPage () {
        startActivity(new Intent(this,
                FakeBuildActivity.class));
    }

    private boolean canFix () {
        return Utils.isAppOpsInstalled() ||
                ShellUtils.isSuAvailable();
    }

    private void lunchAppOps () {
        if (Utils.isAppOpsInstalled()) {
            Intent intent = new Intent("rikka.appops.intent.action.PACKAGE_DETAIL")
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .setClassName("rikka.appops", "rikka.appops.DetailActivity")
                    .putExtra("rikka.appops.intent.extra.USER_HANDLE", UserHandle.CURRENT.hashCode())
                    .putExtra("rikka.appops.intent.extra.PACKAGE_NAME", Constants.SERVICE_APP_NAME)
                    .setData(Uri.parse("package:" + Constants.SERVICE_APP_NAME))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, Utils.getString(R.string.rikka_appops_help_toast,
                    this), Toast.LENGTH_LONG).show();
        } else {
            if (ShellUtils.exec("appops set --user " + Utils.myUid() +
                    " " + Constants.SERVICE_APP_NAME  + " " + AppOpsManager.OP_RUN_IN_BACKGROUND +
                    " " + AppOpsManager.MODE_ALLOWED))
                nextPage();
            else
                Toast.makeText(this, R.string.fail
                        , Toast.LENGTH_SHORT).show();
        }
    }
}
