package com.getataxi.client;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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
import com.getataxi.client.utils.UserPreferencesManager;

import org.apache.http.HttpStatus;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class RegisterActivity extends ActionBarActivity{

    private static final int PASSWORD_MIN_LENGTH = 4;
    private static final int EMAIL_MIN_LENGTH = 4;
    private Button registerButton;
    private Button gotoLoginButton;
    private ProgressDialog dialog;

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

    public void attemptToRegister(){
        if(checkInputFields() == true){
            RegisterUserDM model = this.getRegistrationModel();
            TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
            model.phoneNumber = tMgr.getLine1Number();
            final RegisterUserDM registerModel = model;

            RestClientManager.register(model,  new Callback<LoginUserDM>() {
                @Override
                public void success(LoginUserDM loginUserDM, Response response) {
                    int status  = response.getStatus();
                    if (status == HttpStatus.SC_OK){
                        try {
                            Toast.makeText(context, R.string.successfully_registered_msg, Toast.LENGTH_LONG).show();
                            UserPreferencesManager.saveUserData(registerModel, context);
                            Intent login = new Intent(context, LoginActivity.class);
                            context.startActivity(login);
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    // TODO: Fix error message
                    Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void gotoLoginActivity(){
        Intent gotoLoginIntent = new Intent(this, LoginActivity.class);
        startActivity(gotoLoginIntent);
    }

    @Deprecated
    public class UserRegisterTask extends AsyncTask<RegisterUserDM, Void, Boolean>{

        @Override
        protected Boolean doInBackground(RegisterUserDM... params) {
//            RestClientManager manager = new RestClientManager(getApplicationContext());
//            try{
//                manager.register(params[0]);
//            }catch (Exception e){
//
//            }
//
            try{
             //   RestClientManager.register(params[0], context);
            } catch (Exception e){

            }

              return false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(getApplicationContext());
            dialog.setMessage(getString(R.string.creating_account_msg));
            dialog.show();
        }
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
        if( firstNameEditText.getText().toString().length() == 0 ) {
            firstNameEditText.setError("First name is required!");
            return false;
        }

        if( lastNameEditText.getText().toString().length() == 0 ) {
            lastNameEditText.setError("Last name is required!");
            return false;
        }
        if( emailEditText.getText().toString().length() == 0 ) {
            emailEditText.setError("First name is required!");
            return false;
        }
        if( firstNameEditText.getText().toString().length() == 0 ) {
            firstNameEditText.setError("First name is required!");
            return false;
        }
        if( firstNameEditText.getText().toString().length() == 0 ) {
            firstNameEditText.setError("First name is required!");
            return false;
        }
        if( !isEmailValid(emailEditText.getText().toString()) ) {
            firstNameEditText.setError("Invalid e-mail!");
            return false;
        }
        if( isPasswordValid(passwordEditText.getText().toString())) {
            firstNameEditText.setError(String.format("Password should be more than %d symbols!", PASSWORD_MIN_LENGTH));
            return false;
        }
        return true;
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".") && (email.length() >= EMAIL_MIN_LENGTH);
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= PASSWORD_MIN_LENGTH;
    }
}
