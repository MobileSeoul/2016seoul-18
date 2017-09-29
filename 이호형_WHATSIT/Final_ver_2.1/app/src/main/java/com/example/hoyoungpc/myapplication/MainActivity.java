package com.example.hoyoungpc.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.ispeech.SpeechSynthesis;
import org.ispeech.SpeechSynthesisEvent;
import org.ispeech.error.BusyException;
import org.ispeech.error.InvalidApiKeyException;
import org.ispeech.error.NoNetworkException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

// 161031 최종개발완료.


public class MainActivity extends AppCompatActivity {

    //다른 곳에서 mainAcitivyt종료를 위함
    public static Activity mainActivity;
    private String version = "161009";//현재 어플리케이션 버젼
    ProgressBar progressBar;
    private TextView loadingTxt;

    //내부DB
    WordDBHelper db_Helper;
    SQLiteDatabase db;
    ContentValues row;

    //koreaTTS
    SpeechSynthesis synthesis;
    Context _context;
    private static final String TAG = CameraActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String korean = "버전 확인완료";

        //다른 곳에서 main을 finish하기 위해 선언.
        mainActivity = MainActivity.this;

        //koreaTTS
        //20161018 hyson
        _context = this.getApplicationContext();
        prepareTTSEngine();
        synthesis.setStreamType(AudioManager.STREAM_MUSIC);

        //TTS읽어주기(한글버젼)
        try {
            // String ttsText = ((EditText) findViewById(R.id.text)).getText().toString();
            synthesis.addOptionalCommand("Voice", "krkoreanfemale");
            synthesis.speak(korean);

        } catch (BusyException e) {
            Log.e(TAG, "SDK is busy");
            e.printStackTrace();
            Toast.makeText(_context, "ERROR: SDK is busy", Toast.LENGTH_LONG).show();
        } catch (NoNetworkException e) {
            Log.e(TAG, "Network is not available\n" + e.getStackTrace());
            Toast.makeText(_context, "ERROR: Network is not available", Toast.LENGTH_LONG).show();
        }

        Log.d("translate", korean);

    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();


        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/font3.TTF");//폰트 설정하기
        loadingTxt = (TextView) findViewById(R.id.start_loading);
        loadingTxt.setText("인터넷 체크중...");
        //progressBar = (ProgressBar)findViewById(R.id.progressBar1);

        // ------------Http 제어권 획득(HTTPpost사용시 필수) -------------------------
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        if (checkNetwokState()) {
            //loadingTxt.setText("최신버젼 체크중...");
            // -------------------Http 전송 ----------------------------
            HttpClient HTTPClient = new DefaultHttpClient();
            HttpConnectionParams.setSoTimeout(HTTPClient.getParams(),
                    15000);
            HttpConnectionParams.setConnectionTimeout(
                    HTTPClient.getParams(), 15000);
            String check_url = "http://sos37591.dothome.co.kr/seoulapp/10versionCheck.php";//최신 버젼 확인.
            HttpPost httppost = new HttpPost(check_url);
            HttpResponse response = null;
            try {
                List nameValuePairs = new ArrayList(1);
                nameValuePairs.add(new BasicNameValuePair("clientex04",
                        "zxcvzxcv"));
                httppost.setEntity(new UrlEncodedFormEntity(
                        nameValuePairs));
                response = HTTPClient.execute(httppost);

                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(response.getEntity()
                                .getContent(), "euc-kr"));

                while (true) {
                    String line = br.readLine();

                    if (line == null)
                        break;

                    sb.append(line);
                }

                br.close();
                String temp = sb.toString();
                //loadingTxt.setText("Version Check ...");
                if (!temp.equals(version)) {//버젼이 최신이 아닌 경우.


                    Log.d("debugingg!!", temp);
                    Log.d("debugingg!!", version);

                    // 알럿 다이얼로그 빌더를 생성한다.
                    new AlertDialog.Builder(MainActivity.this)
                            // 타이틀을 설정한다.
                            .setTitle("Notice")
                            // 메시지를 설정한다.
                            .setMessage("최신 버젼이 있습니다. 자동으로 최신 버젼을 다운로드합니다.")
                            // positive 버튼을 설정한다.
                            .setPositiveButton("확인",
                                    new DialogInterface.OnClickListener() {
                                        // positive 버튼에 대한 클릭 이벤트 처리를 구현한다.
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {

                                            Log.e("ERROR", "확인 클릭");

                                            // 다이얼로그를 화면에서 사라지게 한다.
                                            dialog.dismiss();
                                            finish();
                                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://sos37591.dothome.co.kr/seoulapp/release/app-debug.apk"));//최신 버젼 apk경로
                                            startActivity(intent);
                                        }
                                    })
                            // 빌더를 통해 만든 알럿 다이얼로그를 화면에 보여준다.
                            .show();
                } else {
                    //내부 DB생성
                    db_Helper = new WordDBHelper(MainActivity.this);
                    db = db_Helper.getReadableDatabase();
                    // 안드로이드의 Cursor 클래스는 ResultSet과 유사하다.
                    final Cursor cursor;
                    // SQL 명령으로 읽기
                    cursor = db.rawQuery(
                            "SELECT first FROM user_table", null);
                    String first = "";
                    while (cursor.moveToNext()) {
                        String getFirst = cursor.getString(0);
                        first = getFirst;
                    }

                    //DB에 first가 비어있으면 처음 접속한 것이므로 first를 NOT으로 업데이트함.
                    if (first.length() == 0) {
                        Toast.makeText(MainActivity.this, "처음 실행이므로 매뉴얼을 실행합니다.",
                                Toast.LENGTH_SHORT).show();


                        Handler mHandler = new Handler();
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                //마시멜로 이상 카메라 퍼미션 획득
                                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//마시멜로는
                                    Log.d("Version", "Marshmellow");

                                    PermissionListener permissionlistener = new PermissionListener() {
                                        @Override
                                        public void onPermissionGranted() {
                                            Toast.makeText(MainActivity.this, "권한 허용됨", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(MainActivity.this, SubActivity.class);
                                            startActivity(intent);
                                            db_Helper.close();
                                            db = db_Helper.getWritableDatabase();
                                            row = new ContentValues();
                                            row.put("first", "NOT");
                                            db.insert("user_table", null, row);
                                            cursor.close();////////런타임 오류 날수있음
                                            db_Helper.close();
                                        }

                                        @Override
                                        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                                            Toast.makeText(MainActivity.this, "권한을 설정해주세요", Toast.LENGTH_SHORT).show();
                                            synthesis.cancel();
                                            finish();
                                        }
                                    };
                                    new TedPermission(getBaseContext())
                                            .setPermissionListener(permissionlistener)
                                            .setDeniedMessage("카메라 사용을 위한 권한을 설정해주세요\n\n[설정] > [애플리케이션] > [권한]")
                                            .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                                            .check();
                                } else {//마시멜로가 아닌 경우
                                    Intent intent = new Intent(MainActivity.this, SubActivity.class);
                                    startActivity(intent);
                                    db_Helper.close();
                                    db = db_Helper.getWritableDatabase();
                                    row = new ContentValues();
                                    row.put("first", "NOT");
                                    db.insert("user_table", null, row);
                                    cursor.close();////////런타임 오류 날수있음
                                    db_Helper.close();
                                }

                            }
                        }, 3000);
                    } else {//처음 앱을 실행한 게 아니면 로고도 3초(인터넷 체크시간)만 보여주고 매뉴얼Activity를 skip함.
                        cursor.close();
                        db_Helper.close();
                        Handler mHandler = new Handler();
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    Log.d("Version", "Marshmellow");

                                    //마시멜로 이상 카메라 퍼미션 획득
                                    PermissionListener permissionlistener = new PermissionListener() {
                                        @Override
                                        public void onPermissionGranted() {
                                            Toast.makeText(MainActivity.this, "권한 허용됨", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                                            startActivity(intent);
                                        }

                                        @Override
                                        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                                            Toast.makeText(MainActivity.this, "권한을 설정해주세요", Toast.LENGTH_SHORT).show();
                                            synthesis.cancel();
                                            finish();
                                        }
                                    };
                                    new TedPermission(getBaseContext())
                                            .setPermissionListener(permissionlistener)
                                            .setDeniedMessage("카메라 사용을 위한 권한을 설정해주세요\n\n[설정] > [애플리케이션] > [권한]")
                                            .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                                            .check();

                                } else {
                                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                                    startActivity(intent);
                                }

                            }
                        }, 3000);

                    }

                }
            } catch (Exception e) {

                //Log.d("easds",e.toString());

                // 알럿 다이얼로그 빌더를 생성한다.
                new AlertDialog.Builder(MainActivity.this)
                        // 타이틀을 설정한다.
                        .setTitle("인터넷 연결 오류")
                        // 메시지를 설정한다.
                        .setMessage("인터넷 연결 상태가 불안합니다.")
                        // positive 버튼을 설정한다.
                        .setPositiveButton("확인",
                                new DialogInterface.OnClickListener() {
                                    // positive 버튼에 대한 클릭 이벤트 처리를 구현한다.
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which) {

                                        Log.e("error", "확인 클릭");

                                        // 다이얼로그를 화면에서 사라지게 한다.
                                        dialog.dismiss();
                                        finish();

                                    }
                                })
                        // 빌더를 통해 만든 알럿 다이얼로그를 화면에 보여준다.
                        .show();
            }
        }
    }

    public boolean checkNetwokState() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = manager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = manager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo lte_4g = manager
                .getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
        boolean blte_4g = false;
        if (lte_4g != null)
            blte_4g = lte_4g.isConnected();
        if (mobile.isConnected() || wifi.isConnected() || blte_4g)
            return true;
        else {
            AlertDialog.Builder dlg = new AlertDialog.Builder(
                    MainActivity.this);
            dlg.setTitle("네트워크 오류");
            dlg.setMessage("네트워크 상태를 확인해 주십시요.");
            dlg.setNegativeButton("종료", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish(); // 끝내버렷
                }
            });
            dlg.setCancelable(false);
            dlg.show();
            return false;
        }
    }


    //koreaTTS
    //20161018 hyson
    private void prepareTTSEngine() {
        try {
            synthesis = SpeechSynthesis.getInstance(this);
            synthesis.setSpeechSynthesisEvent(new SpeechSynthesisEvent() {

                public void onPlaySuccessful() {
                    Log.i(TAG, "onPlaySuccessful");
                    //temp2 = 1;
                }

                public void onPlayStopped() {
                    Log.i(TAG, "onPlayStopped");
                }

                public void onPlayFailed(Exception e) {
                    Log.e(TAG, "onPlayFailed");
                    Log.d("list", e.toString());
                }

                public void onPlayStart() {
                    Log.i(TAG, "onPlayStart");
                }

                @Override
                public void onPlayCanceled() {
                    Log.i(TAG, "onPlayCanceled");
                }


            });


        } catch (InvalidApiKeyException e) {
            Log.e(TAG, "Invalid API key\n" + e.getStackTrace());
            Toast.makeText(_context, "ERROR: Invalid API key", Toast.LENGTH_LONG).show();
        }

    }

}


