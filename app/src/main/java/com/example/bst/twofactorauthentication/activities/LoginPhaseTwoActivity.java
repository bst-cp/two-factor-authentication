package com.example.bst.twofactorauthentication.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;

import com.example.bst.twofactorauthentication.R;
import com.example.bst.twofactorauthentication.sql.DatabaseHelper;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.File;

/**
 * Created by Taha on 15.10.2017.
 */

public class LoginPhaseTwoActivity extends AppCompatActivity implements View.OnClickListener{

    private final AppCompatActivity activity = LoginPhaseTwoActivity.this;

    private NestedScrollView nestedScrollView;

    private AppCompatButton appCompatButtonLogin;
    private AppCompatButton getAppCompatButtonTakePhoto;
    private AppCompatTextView textViewLoginDifferent;
    private DatabaseHelper databaseHelper;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_phase_two);
        getSupportActionBar().hide();

        initViews();
        initListeners();
        initObjects();
    }
    private void initViews() {

        nestedScrollView = (NestedScrollView) findViewById(R.id.nestedScrollView);

        appCompatButtonLogin = (AppCompatButton) findViewById(R.id.appCompatButtonLogin);
        getAppCompatButtonTakePhoto = (AppCompatButton) findViewById(R.id.appCompatButtonTakePhoto);
        textViewLoginDifferent = (AppCompatTextView) findViewById(R.id.textViewLoginDifferent);
    }

    /**
     * This method is to initialize listeners
     */
    private void initListeners() {
        appCompatButtonLogin.setOnClickListener(this);
        textViewLoginDifferent.setOnClickListener(this);
        getAppCompatButtonTakePhoto.setOnClickListener(this);
    }

    /**
     * This method is to initialize objects to be used
     */
    private void initObjects() {
        databaseHelper = new DatabaseHelper(activity);

    }

    /**
     * This implemented method is to listen the click on view
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            /*
            When login button is pressed it takes CONF value from previous intent which is same
            activity but in that activity, take picture button is pressed and confidence value is generated
            */
            case R.id.appCompatButtonLogin:
                String conf = getIntent().getStringExtra("CONF");
                if(Float.parseFloat(conf) > 80.0){
                    Intent accountsIntent = new Intent(activity, UsersListActivity.class);
                    accountsIntent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
                    startActivity(accountsIntent);
                }
                else{
                    Snackbar.make(nestedScrollView, "Confidence Level is low", Snackbar.LENGTH_LONG).show();
                }
                break;
            /*
            When Login Different button is pressed it take you first login screen.
            */
            case R.id.textViewLoginDifferent:
                // Navigate to LoginActivity
                Intent intentLogin = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intentLogin);
                break;
            case R.id.appCompatButtonTakePhoto:
                takePhoto();
                break;
        }
    }
    /*
    This function takes photo and save in value of path variable. Then it waits 10 second to be sure
    that photo is taken. After that it calls tokenGenerate function to generate token from taken picture.
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
    /*
    This function generate token from picture that is in path. It sent POST request to faceplusplus
    api. After response returns by json format, function takes faces array and take value of face_token and
    call tokenCompare function by that token.
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
                        tokenCompare(token);
                    } catch (Exception e1){
                        e1.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                    if (responseBody != null) {
                        Log.w("ceshi", "responseBody===" + new String(responseBody));
                    }
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
    /*
    This function compare newly taken picture's token and token in database. It takes username value
    from previous activity and get token value from database. After that it sent POST request to the
    faceplusplus api. Response is in json format and from that response function takes confidence level
    (similarity) sent to same activity and call again same activity. If user press login button onClick function will work.
    */
    private void tokenCompare(final String token){
        try {
            String usernameFromIntent = getIntent().getStringExtra("USERNAME");
            String url = "https://api-us.faceplusplus.com/facepp/v3/compare";
            RequestParams rParams = new RequestParams();
            rParams.put("api_key", "htWp39c1fK_TMgT9S2OasCpes8v9p4ja");
            rParams.put("api_secret", "hwO0zOAi-PKU2IOfgUmgTPu2jwp3S-qn");
            rParams.put("face_token1", token);
            rParams.put("face_token2", databaseHelper.getToken(usernameFromIntent));
            AsyncHttpClient asyncHttpclient = new AsyncHttpClient();
            asyncHttpclient.post(url, rParams, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                    try {
                        String successStr = new String(responseBody);
                        String confidence = new JSONObject(successStr).getString("confidence");
                        Intent accountsIntent = new Intent(activity, LoginPhaseTwoActivity.class);
                        accountsIntent.putExtra("CONF", confidence);
                        accountsIntent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
                        startActivity(accountsIntent);

                    } catch (Exception e1){
                        e1.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                    if (responseBody != null) {
                        Log.w("ceshi", "responseBody===" + new String(responseBody));
                    }
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
