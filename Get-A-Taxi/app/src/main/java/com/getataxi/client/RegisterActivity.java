package com.getataxi.client;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.getataxi.client.comm.RestClientManager;
import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.comm.models.RegisterUserDM;
import com.getataxi.client.utils.Constants;
import com.getataxi.client.utils.UserPreferencesManager;

import org.apache.http.HttpStatus;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;


public class RegisterActivity extends ActionBarActivity{


    private Button registerButton;
    private Button gotoLoginButton;
    private View mProgressView;
    private View mRegisterFormView;

    private EditText firstNameEditText;
    private EditText middleNameEditText;
    private EditText lastNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        context = this;

        mProgressView = findViewById(R.id.register_progress);
        mRegisterFormView = findViewById(R.id.register_form);

        this.firstNameEditText = (EditText) findViewById(R.id.et_firstName);
        this.middleNameEditText = (EditText) findViewById(R.id.et_middleName);
        this.lastNameEditText = (EditText) findViewById(R.id.et_lastName);

        this.emailEditText = (EditText) findViewById(R.id.et_register_email);
        this.passwordEditText = (EditText) findViewById(R.id.et_register_password);
        this.confirmPasswordEditText = (EditText) findViewById(R.id.et_confirm_pass);

        this.registerButton = (Button)findViewById(R.id.btn_register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptToRegister();
            }
        });

        this.gotoLoginButton = (Button) findViewById(R.id.btn_goto_login);
        gotoLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoLoginActivity();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegisterFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void attemptToRegister(){
        if(checkInputFields() == true){
            RegisterUserDM model = this.getRegistrationModel();
            TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
            model.phoneNumber = tMgr.getLine1Number();
            final RegisterUserDM registerModel = model;
            showProgress(true);

            RestClientManager.register(model,  new Callback<String>() {
                @Override
                public void success(String s, Response response) {
                    int status  = response.getStatus();
                    if (status == HttpStatus.SC_OK){
                        try {
                            showProgress(false);
                            Toast.makeText(context, R.string.successfully_registered_msg, Toast.LENGTH_LONG).show();
                            UserPreferencesManager.saveUserData(registerModel, context);
                            gotoLoginActivity();
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    showProgress(false);
                    showToastError(error);
                }
            });
        }
    }

    private void showToastError(RetrofitError error) {
        if(error.getResponse() != null) {
            if (error.getResponse().getBody() != null) {
                String json =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
                if(!json.isEmpty()){
                    Toast.makeText(context, json, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void gotoLoginActivity(){
        Intent gotoLoginIntent = new Intent(this, LoginActivity.class);
        startActivity(gotoLoginIntent);
    }

    public RegisterUserDM getRegistrationModel(){
        RegisterUserDM registerUserDM = new RegisterUserDM();

        registerUserDM.userName = this.emailEditText.getText().toString();
        registerUserDM.firstName = this.firstNameEditText.getText().toString();
        registerUserDM.middleName = this.middleNameEditText.getText().toString();
        registerUserDM.lastName = this.lastNameEditText.getText().toString();

        registerUserDM.email = this.emailEditText.getText().toString();
        registerUserDM.password = this.passwordEditText.getText().toString();
        registerUserDM.confirmPassword = this.confirmPasswordEditText.getText().toString();

        return registerUserDM;
    }

    private boolean checkInputFields(){
        Resources res = getResources();
        if( firstNameEditText.getText().toString().length() == 0 ) {
            firstNameEditText.setError(res.getString(R.string.first_name_required));
            return false;
        }

        if( lastNameEditText.getText().toString().length() == 0 ) {
            lastNameEditText.setError(res.getString(R.string.last_name_required));
            return false;
        }
        if( emailEditText.getText().toString().length() == 0 ) {
            emailEditText.setError(res.getString(R.string.email_required));
            return false;
        }

        if( !isEmailValid(emailEditText.getText().toString()) ) {
            emailEditText.setError(res.getString(R.string.invalid_email));
            return false;
        }
        if( !isPasswordValid(passwordEditText.getText().toString())) {
            passwordEditText.setError(String.format(res.getString(R.string.password_short), Constants.PASSWORD_MIN_LENGTH));
            return false;
        }
        return true;
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".") && (email.length() >= Constants.EMAIL_MIN_LENGTH);
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= Constants.PASSWORD_MIN_LENGTH;
    }
}
