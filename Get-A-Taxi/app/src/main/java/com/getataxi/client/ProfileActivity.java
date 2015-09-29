package com.getataxi.client;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getataxi.client.comm.RestClientManager;
import com.getataxi.client.comm.models.PhotoDM;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;

import java.io.ByteArrayOutputStream;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

public class ProfileActivity extends ActionBarActivity {

    private Context context;
    private static final int CAMERA_REQUEST = 1888;
    private ImageView photoImageView;
    private PhotoDM photoModel = null;
    private View mProgressView;
    Button photoChangeButton;
    Button photoSaveButton;
    Button photoRemoveButton;
    TextView photoPlaceholderText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        context = this;

        mProgressView = findViewById(R.id.profile_progress);
        this.photoImageView = (ImageView)this.findViewById(R.id.photoImageView);
        photoChangeButton = (Button)this.findViewById(R.id.changePhotoButton);
        photoChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        photoSaveButton = (Button)this.findViewById(R.id.setPhotoButton);
        photoSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (photoModel != null) {
                    storePhoto(photoModel);
                }
            }
        });

        photoRemoveButton = (Button)this.findViewById(R.id.removePhotoButton);
        photoRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (photoModel != null) {
                    removePhoto();
                }
            }
        });

        photoPlaceholderText = (TextView) this.findViewById(R.id.photoPlaceholderText);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(photoModel == null){
            getUserPhoto();
        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            photoImageView.setImageBitmap(photo);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            photoModel = new PhotoDM();
            photoModel.extension = "png";
            photoModel.content = Base64.encodeToString(byteArray, Base64.DEFAULT);

            photoSaveButton.setEnabled(true);
            setButton(PhotoButtonType.SavePhoto);
        }
    }

    private void storePhoto(PhotoDM photo) {
        if(photo == null) return;
        showProgress(true);
        RestClientManager.addPhoto(photo, context, new Callback<Integer>() {
            @Override
            public void success(Integer integer, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    Toast.makeText(context, context.getResources().getText(R.string.photo_stored_success), Toast.LENGTH_LONG).show();
                    setButton(PhotoButtonType.RemovePhoto);
                }
                showProgress(false);
            }

            @Override
            public void failure(RetrofitError error) {
                showToastError(error);
                showProgress(false);
            }
        });
    }

    private void getUserPhoto(){
        showProgress(true);
        RestClientManager.getUserPhoto(context, new Callback<PhotoDM>() {
            @Override
            public void success(PhotoDM photoDM, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    photoModel = photoDM;
                    displayPhoto(photoModel);
                    setButton(PhotoButtonType.RemovePhoto);
                }

                showProgress(false);
            }

            @Override
            public void failure(RetrofitError error) {
                setButton(PhotoButtonType.NoButton);
                Response response = error.getResponse();
                if(response != null){
                    int status = response.getStatus();
                    if (status == HttpStatus.SC_NOT_FOUND) {
                        Toast.makeText(context, context.getResources().getText(R.string.no_photo), Toast.LENGTH_LONG).show();
                        photoPlaceholderText.setText(context.getResources().getText(R.string.no_photo));
                    }
                }
                showToastError(error);
                showProgress(false);
            }
        });
    }

    private void removePhoto(){
        if(photoModel == null) return;
        showProgress(true);
        RestClientManager.removePhoto(photoModel.photoId, context, new Callback<Integer>() {
            @Override
            public void success(Integer integer, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_NOT_FOUND || status == HttpStatus.SC_BAD_REQUEST) {
                    Toast.makeText(context, context.getResources().getText(R.string.no_photo), Toast.LENGTH_LONG).show();
                    photoPlaceholderText.setText(context.getResources().getText(R.string.no_photo));
                }
                photoModel = null;
                photoImageView.setImageDrawable(null);
                setButton(PhotoButtonType.NoButton);
            }

            @Override
            public void failure(RetrofitError error) {
                showToastError(error);
                showProgress(false);
            }
        });
    }

    private void displayPhoto(PhotoDM photoDM){
        photoPlaceholderText.setText("");
        //Bitmap photo = BitmapFactory.decodeByteArray(photoDM.content, 0, photoDM.content.length);
        byte[] byteArray = photoDM.getImage();
        Bitmap photo = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        photoImageView.setImageBitmap(photo);
    }

    public enum PhotoButtonType {
       NoButton, SavePhoto, RemovePhoto
    }

    public void setButton(PhotoButtonType button){
        switch (button){
            case SavePhoto: photoSaveButton.setEnabled(true);
                photoSaveButton.setVisibility(View.VISIBLE);
                photoRemoveButton.setEnabled(false);
                photoRemoveButton.setVisibility(View.INVISIBLE);
                break;
            case RemovePhoto:
                photoSaveButton.setEnabled(false);
                photoSaveButton.setVisibility(View.INVISIBLE);
                photoRemoveButton.setEnabled(true);
                photoRemoveButton.setVisibility(View.VISIBLE);
                break;
            case NoButton:
            default:
                photoSaveButton.setEnabled(false);
                photoSaveButton.setVisibility(View.INVISIBLE);
                photoRemoveButton.setEnabled(false);
                photoRemoveButton.setVisibility(View.INVISIBLE);
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_goto_order) {
            Intent gotoOrderIntent = new Intent(this, OrderMap.class);
            startActivity(gotoOrderIntent);
        }

        if(id == R.id.action_user_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows the ordering progress UI
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

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
        }
    }

    private boolean showToastError(RetrofitError error) {
        Response response = error.getResponse();
        if (response != null && response.getBody() != null) {
            String json =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
            if(!json.isEmpty()){
                JsonObject jobj = new Gson().fromJson(json, JsonObject.class);
                String message = jobj.get("Message").getAsString();
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                // There was a message from the server
                return true;
            } else {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
        }

        return false;
    }
}
