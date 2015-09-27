package com.getataxi.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.getataxi.client.comm.RestClientManager;
import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.utils.DeviceState;
import com.getataxi.client.utils.LocationService;
import com.getataxi.client.utils.UserPreferencesManager;

import org.apache.http.HttpStatus;
import org.parceler.transfuse.annotations.Resource;

import java.io.IOException;
import java.text.ParseException;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;


public class StartupActivity extends Activity {
    private Context context;
    private Button proceedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        context = this;
        this.proceedButton = (Button)findViewById(R.id.proceed_to_order_btn);
        boolean devicesReady = true;

        if(!DeviceState.isNetworkAvailable(context)){
            devicesReady = false;
            DeviceState.showSettingsAlert(
                    R.string.network_disconnected_title,
                    R.string.network_disconnected_message,
                    Settings.ACTION_WIRELESS_SETTINGS,
                    context
            );
        }

        if (!DeviceState.isPositioningAvailable(context)){
            devicesReady = false;
            DeviceState.showSettingsAlert(
                    R.string.positioning_unavailable_title,
                    R.string.positioning_unavailable_message,
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS,
                    context
            );
        }


        if(devicesReady) {
            proceedWithStartup();
        }else {
            proceedButton.setVisibility(View.VISIBLE);
            proceedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    proceedWithStartup();
                }
            });
        }
    }


    private void proceedWithStartup() {
        // Check for login credentials
        if(UserPreferencesManager.checkForLoginCredentials(context)){
            LoginUserDM loginUserDM = UserPreferencesManager.getLoginData(context);
            final String password = loginUserDM.password;
            // Check if still logged-in
            if(UserPreferencesManager.isLoggedIn(context) && !UserPreferencesManager.tokenHasExpired(loginUserDM.expires)){
                Intent orderMap = new Intent(context, OrderMap.class);
                orderMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(orderMap);
            }else{
                String grantType = "password";

                RestClientManager.login(loginUserDM, grantType, new Callback<LoginUserDM>() {
                    @Override
                    public void success(LoginUserDM loginSuccessUserDM, Response response) {
                        int status = response.getStatus();
                        if (status == HttpStatus.SC_OK) {
                            try {
                                String s = getResources().getString(R.string.token_renew);
                                Toast.makeText(context, String.format(s, loginSuccessUserDM.userName), Toast.LENGTH_LONG).show();
                                loginSuccessUserDM.password = password;
                                UserPreferencesManager.saveLoginData(loginSuccessUserDM, context);
                                Intent orderMap = new Intent(context, OrderMap.class);
                                orderMap.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(orderMap);

                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        showToastError(error);
                    }
                });

            }
        } else  {
            // No login credentials, check for stored registration
            Resources res = getResources();
            if (UserPreferencesManager.checkForRegistration(context)){
                // Stored credentials found, suggest login
                Toast.makeText(context, res.getString(R.string.please_login), Toast.LENGTH_LONG).show();
                Intent loginIntent = new Intent(context, LoginActivity.class);
                loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(loginIntent);
            } else {
                // No stored credentials at all, suggesting new registration
                Toast.makeText(context, res.getString(R.string.please_register), Toast.LENGTH_LONG).show();
                Intent registerIntent = new Intent(context, RegisterActivity.class);
                registerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(registerIntent);
            }
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


}
