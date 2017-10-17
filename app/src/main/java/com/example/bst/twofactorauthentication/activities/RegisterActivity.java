package com.example.bst.twofactorauthentication.activities;

import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.content.Intent;
import android.provider.MediaStore;
import android.util.Log;
import android.os.Handler;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import com.example.bst.twofactorauthentication.R;
import com.example.bst.twofactorauthentication.helpers.InputValidation;
import com.example.bst.twofactorauthentication.model.User;
import com.example.bst.twofactorauthentication.sql.DatabaseHelper;
import com.example.bst.twofactorauthentication.sha256.sha256;

import org.json.JSONObject;

import java.io.File;


public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private final AppCompatActivity activity = RegisterActivity.this;

    private NestedScrollView nestedScrollView;

    private TextInputLayout textInputLayoutUsername;
    private TextInputLayout textInputLayoutPassword;
    private TextInputLayout textInputLayoutConfirmPassword;

    private TextInputEditText textInputEditTextUsername;
    private TextInputEditText textInputEditTextPassword;
    private TextInputEditText textInputEditTextConfirmPassword;

    private AppCompatTextView appCompatTextViewLoginLink;
    private AppCompatButton getAppCompatButtonTakePhoto;
    private InputValidation inputValidation;
    private DatabaseHelper databaseHelper;
    private User user;


    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getSupportActionBar().hide();

        initViews();
        initListeners();
        initObjects();
    }

    /**
     * initViews is used to initialize views
     */
    private void initViews() {
        nestedScrollView = (NestedScrollView) findViewById(R.id.nestedScrollView);

        textInputLayoutUsername = (TextInputLayout) findViewById(R.id.textInputLayoutUsername);
        textInputLayoutPassword = (TextInputLayout) findViewById(R.id.textInputLayoutPassword);
        textInputLayoutConfirmPassword = (TextInputLayout) findViewById(R.id.textInputLayoutConfirmPassword);

        textInputEditTextUsername = (TextInputEditText) findViewById(R.id.textInputEditTextUsername);
        textInputEditTextPassword = (TextInputEditText) findViewById(R.id.textInputEditTextPassword);
        textInputEditTextConfirmPassword = (TextInputEditText) findViewById(R.id.textInputEditTextConfirmPassword);

        getAppCompatButtonTakePhoto = (AppCompatButton) findViewById(R.id.appCompatButtonTakePhoto);

        appCompatTextViewLoginLink = (AppCompatTextView) findViewById(R.id.appCompatTextViewLoginLink);

    }

    /**
     * initListeners is used to initialize listeners
     */
    private void initListeners() {
        appCompatTextViewLoginLink.setOnClickListener(this);
        getAppCompatButtonTakePhoto.setOnClickListener(this);
    }

    /**
     * initObjects is used to initialize objects
     */
    private void initObjects() {
        inputValidation = new InputValidation(activity);
        databaseHelper = new DatabaseHelper(activity);
        user = new User();

    }


    /**
     * onClick is used to listen click on view
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.appCompatTextViewLoginLink:
                finish();
                break;
            case R.id.appCompatButtonTakePhoto:
                if(inputCheck())
                    takePhoto();
                break;
        }
    }

    /**
     * validate the input text fields
     */
    private boolean inputCheck() {
        if (!inputValidation.isInputEditTextFilled(textInputEditTextUsername, textInputLayoutUsername, getString(R.string.error_message_username))) {
            return false;
        }
        if (!inputValidation.isInputEditTextFilled(textInputEditTextPassword, textInputLayoutPassword, getString(R.string.error_message_password))) {
            return false;
        }
        if (!inputValidation.isInputEditTextMatches(textInputEditTextPassword, textInputEditTextConfirmPassword,
                textInputLayoutConfirmPassword, getString(R.string.error_password_match))) {
            return false;
        }
        return true;
    }
    /**
     *takePhoto is used to take photo and save in value of path variable Then it waits 10 second to be sure
     *that photo is taken.
     */
    private void takePhoto(){
        final String path = "/storage/emulated/0/Pictures/test.jpg";
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"test.jpg");
        Uri tempUri = Uri.fromFile(imageFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                tokenGenerate(path);
            }
        }, 10000);
    }

    /**
     * This method is to empty all input edit text
     */
    private void emptyInputEditText() {
        textInputEditTextUsername.setText(null);
        textInputEditTextPassword.setText(null);
        textInputEditTextConfirmPassword.setText(null);
    }
    /**
     * tokenGenerate is used to generate token from picture that is in path. It sent POST request to faceplusplus
     * api. Then information is saved to database
     */
    private void tokenGenerate(final String path) {
        try {
            String url = "https://api-us.faceplusplus.com/facepp/v3/detect";
            RequestParams rParams = new RequestParams();
            rParams.put("api_key", "htWp39c1fK_TMgT9S2OasCpes8v9p4ja");
            rParams.put("api_secret", "hwO0zOAi-PKU2IOfgUmgTPu2jwp3S-qn");
            rParams.put("image_file", new File(path));
            AsyncHttpClient asyncHttpclient = new AsyncHttpClient();
            asyncHttpclient.post(url, rParams, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                    try {
                        String successStr = new String(responseBody);
                        JSONObject jObject = new JSONObject(successStr).getJSONArray("faces").getJSONObject(0);
                        String token = jObject.getString("face_token");

                        if (!databaseHelper.checkUser(textInputEditTextUsername.getText().toString().trim())) {
                            sha256 sha = new sha256();
                            user.setUsername(textInputEditTextUsername.getText().toString().trim());
                            user.setPassword(sha.shaConverter(textInputEditTextPassword.getText().toString().trim()));
                            user.setToken(token);
                            databaseHelper.addUser(user);
                            Snackbar.make(nestedScrollView, getString(R.string.success_message), Snackbar.LENGTH_LONG).show();

                        }else {
                            // Snack Bar to show error message that record already exists
                            Snackbar.make(nestedScrollView, getString(R.string.error_username_exists), Snackbar.LENGTH_LONG).show();
                        }

                    } catch (Exception e1){
                        e1.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                    if (responseBody != null) {
                        Log.w("ceshi", "responseBody===" + new String(responseBody));
                        Snackbar.make(nestedScrollView, getString(R.string.error_token), Snackbar.LENGTH_LONG).show();

                    }
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
