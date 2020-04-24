package com.inledco.exoterra.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.inledco.exoterra.R;
import com.inledco.exoterra.aliot.AliotServer;
import com.inledco.exoterra.base.BaseActivity;
import com.inledco.exoterra.foundback.FoundbackActivity;
import com.inledco.exoterra.home.HomeActivity;
import com.inledco.exoterra.main.MainActivity;
import com.inledco.exoterra.manager.UserManager;
import com.inledco.exoterra.manager.UserPref;
import com.inledco.exoterra.register.RegisterActivity;
import com.inledco.exoterra.util.RegexUtil;
import com.inledco.exoterra.view.AdvancedTextInputEditText;
import com.inledco.exoterra.view.MessageDialog;
import com.inledco.exoterra.view.PasswordEditText;
import com.liruya.loaddialog.LoadDialog;

public class LoginActivity extends BaseActivity {

    private TextInputLayout login_til_email;
    private AdvancedTextInputEditText login_et_email;
    private TextInputLayout login_til_password;
    private PasswordEditText login_et_password;
    private Button login_btn_signin;
    private Button login_btn_forget;
    private Button login_btn_signup;
    private Button login_btn_skip;
    private LoadDialog mLoadDialog;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initData();
        initEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoading();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == 1) {
            if (data != null) {
                String email = data.getStringExtra("email");
                String password = data.getStringExtra("password");
                boolean login = data.getBooleanExtra("login", false);
                login_et_email.setText(email);
                login_et_password.setText(password);
                if (login) {
                    login_btn_signin.performClick();
                }
            }
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_login;
    }

    @Override
    protected void initView() {
        login_til_email = findViewById(R.id.login_til_email);
        login_et_email = findViewById(R.id.login_et_email);
        login_til_password = findViewById(R.id.login_til_password);
        login_et_password = findViewById(R.id.login_et_password);
        login_btn_signin = findViewById(R.id.login_btn_signin);
        login_btn_forget = findViewById(R.id.login_btn_forget);
        login_btn_signup = findViewById(R.id.login_btn_signup);
        login_btn_skip = findViewById(R.id.login_btn_skip);

        login_et_email.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_email_white_24dp, 0, 0, 0);
        login_et_password.setIcon(R.drawable.ic_lock_white_24dp, R.drawable.design_ic_visibility, R.drawable.design_ic_visibility_off);
        login_et_email.bindTextInputLayout(login_til_email);
        login_et_password.bindTextInputLayout(login_til_password);

    }

    @Override
    protected void initData() {
        final String email = UserPref.readEmail(this);
        final String password = UserPref.readPassword(this);
        login_et_email.setText(email);
        login_et_password.setText(password);
        if (TextUtils.isEmpty(email)) {
            login_et_email.requestFocus();
        } else {
            login_et_password.requestFocus();
        }
        mLoadDialog = new LoadDialog(this);
    }

    @Override
    protected void initEvent() {
        login_btn_signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = getEmailText();
                final String password = getPasswordText();
                if (!RegexUtil.isEmail(email)) {
                    login_til_email.setError(getString(R.string.error_email));
                    login_et_email.requestFocus();
                    return;
                }
                if (TextUtils.isEmpty(password) || password.length() < 6) {
                    login_til_password.setError(getString(R.string.error_password));
                    login_et_password.requestFocus();
                    return;
                }
                login(email, password);
            }
        });
        login_btn_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoRegisterActivity();
            }
        });
        login_btn_forget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoFoundbackActivity();
            }
        });
        login_btn_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                gotoMainActivity();
            }
        });
    }

    private String getEmailText() {
        return login_et_email.getText().toString();
    }

    private String getPasswordText() {
        return login_et_password.getText().toString();
    }

    private void showMessageDialog(@StringRes final int res, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new MessageDialog(LoginActivity.this).setTitle(res)
                                                     .setMessage(message)
                                                     .setButton(R.string.ok, null)
                                                     .show();
                dismissLoading();
            }
        });
    }

    private void showLoading() {
//        mLoadDialog.show();
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    private void dismissLoading() {
//        mLoadDialog.dismiss();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void login(final String email, final String password) {
        AsyncTask<String, Void, String> authTask = new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                return UserManager.getInstance().login(LoginActivity.this, email, password);
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showLoading();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                if (result == null) {
                    String userid = UserManager.getInstance().getUserid();
                    String token = UserManager.getInstance().getToken();
                    AliotServer.getInstance().init(userid, token);
                    dismissLoading();
                    gotoHomeActivity();
//                    gotoMainActivity();
                } else {
                    new MessageDialog(LoginActivity.this).setTitle(R.string.signin_failed)
                                                         .setMessage(result)
                                                         .setButton(R.string.ok, null)
                                                         .show();
                    dismissLoading();
                }
            }
        };
        authTask.execute();
    }

    private void gotoHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    private void gotoMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void gotoRegisterActivity() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivityForResult(intent, 1);
    }

    private void gotoFoundbackActivity() {
        Intent intent = new Intent(this, FoundbackActivity.class);
        intent.putExtra("email", getEmailText());
        startActivityForResult(intent, 1);
    }
}
