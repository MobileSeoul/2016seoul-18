package com.example.hoyoungpc.myapplication;

/**
 * Created by O_sung on 2016. 10. 9..
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.ispeech.SpeechSynthesis;
import org.ispeech.SpeechSynthesisEvent;
import org.ispeech.error.BusyException;
import org.ispeech.error.InvalidApiKeyException;
import org.ispeech.error.NoNetworkException;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class SubActivity extends Activity {


    private static final String TAG = CameraActivity.class.getSimpleName();
    // koreanList에서 읽을 문장 index 지정
    int speechIndex = 0;
    //korean에 말할 문장 저장(추가하려면 여기만 추가하면 됨)
    String[] koreanList = {
            "안녕하세요. 음료를 카메라로 비추면 음성으로 알려주는 인공지능 시각도우미 앱, 왓,이즈잇?입니다.",
            "음료를 천천히 돌리면서 카메라로 비춰주세요. 시간이 길수록 분석이 점점 더 정확해집니다.",
            "원하는 답을 찾았으면 뒤로가기를 한번 터치하세요. 그러면 사물 분석을 다시 시작합니다.",
            "앱을 종료하려면 뒤로가기를 두번 터치하세요. 간단하죠?",
            "처음 사용시에만 이렇게 안내메시지가 나오며, 다음 사용부터는 이 안내가 나오지 않습니다.",
            "자~~ 지금부터 카메라를 시작하려면 화면을 터치하세요."
    };
    //koreaTTS
    SpeechSynthesis synthesis;
    Context _context;

    //배경을 변경할 layout 선언
    LinearLayout layout;

    ViewFlipper flipper;

    //뒤로 가기 핸들러
    private BackPressCloseHandler backPressCloseHandler;

    //스케쥴러 
    private TimerTask mTask;
    private Timer mTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/font3.TTF");//폰트 설정하기
        setContentView(R.layout.activity_sub);

        Button btn_start = (Button) findViewById(R.id.btn_start);

        //xml의 sub_background를 참조한다.
        final LinearLayout layout = (LinearLayout) findViewById(R.id.sub_background);


        //koreaTTS
        //20161018 hyson
        _context = this.getApplicationContext();
        prepareTTSEngine();
        synthesis.setStreamType(AudioManager.STREAM_MUSIC);

        //TTS읽어주기(한글버젼)
        try {
            // String ttsText = ((EditText) findViewById(R.id.text)).getText().toString();
            synthesis.addOptionalCommand("Voice", "krkoreanfemale");
            synthesis.speak(koreanList[speechIndex]);

        } catch (BusyException e) {
            Log.e(TAG, "SDK is busy");
            e.printStackTrace();
            Toast.makeText(_context, "ERROR: SDK is busy", Toast.LENGTH_LONG).show();
        } catch (NoNetworkException e) {
            Log.e(TAG, "Network is not available\n" + e.getStackTrace());
            Toast.makeText(_context, "ERROR: Network is not available", Toast.LENGTH_LONG).show();
        }
        Log.d("translate", speechIndex + ":" + koreanList[speechIndex]);
/*
        flipper = (ViewFlipper) findViewById(R.id.flipper);
        final int slideTime = 1000;
        flipper.setFlipInterval(slideTime);
        flipper.startFlipping();
*/

        btn_start.setOnClickListener(new View.OnClickListener() {

                                         @Override
                                         public void onClick(View v) {
                                             Log.d("translate", speechIndex + ">=" + koreanList.length);
                                             if (speechIndex >= koreanList.length-1) {//모든 문장을 읽은 경우
                                                 Intent intent = new Intent(SubActivity.this, CameraActivity.class);
                                                 startActivity(intent);
                                                 if(mTimer!=null){
                                                     mTimer.cancel();
                                                     mTimer.purge();
                                                     mTimer=null;
                                                 }

                                                 finish();
                                             }
                                         }
                                     }
        );


        // 뒤로가기 핸들러
        backPressCloseHandler = new BackPressCloseHandler(this);


        //그림 선언. 그림 추가시 imageList에만 추가해주면 됨.
        int sub1 = R.drawable.manual1;
        int sub2 = R.drawable.manual2;
        int sub3 = R.drawable.manual3;
        final ArrayList<Integer> imageList = new ArrayList<Integer>();
        imageList.add(sub1);
        imageList.add(sub2);
        imageList.add(sub3);
        Log.d("osung",imageList.size()+"");

        final Handler handler = new Handler() {
            int imageIndex = 0;

            public void handleMessage(Message msg) {
                // TimerTask에서 이벤트 발생 -> 핸들러에서 배경 이미지 변경
                layout.setBackgroundResource(imageList.get(imageIndex));
                imageIndex++;
                if (imageIndex >= imageList.size()) {
                    imageIndex = 0;
                }
            }
        };

        //1초 후부터, 1초마다 실행되는 timerTask 생성
        mTask = new TimerTask() {
            @Override
            public void run() {
                //핸들러로 이벤트 전달
                Message msg = handler.obtainMessage();
                handler.sendMessage(msg);
            }
        };
        mTimer = new Timer();
        mTimer.schedule(mTask, 1000, 1000);


    }

    // 뒤로가기 핸들러
    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    private void prepareTTSEngine() {
        try {
            synthesis = SpeechSynthesis.getInstance(this);
            synthesis.setSpeechSynthesisEvent(new SpeechSynthesisEvent() {

                public void onPlaySuccessful() {
                    Log.i(TAG, "onPlaySuccessful");
                    if (speechIndex < koreanList.length - 1) {//읽을 문장이 더 있는 경우
                        speechIndex++;
                        try {
                            Log.d("translate", speechIndex + "번째 문장 읽기:" + koreanList[speechIndex]);
                            synthesis.speak(koreanList[speechIndex]);//현재 index에 해당하는 문장 읽기
                        } catch (BusyException e) {
                            Log.e(TAG, "SDK is busy");
                            e.printStackTrace();
                            Toast.makeText(_context, "ERROR: SDK is busy", Toast.LENGTH_LONG).show();
                        } catch (NoNetworkException e) {
                            Log.e(TAG, "Network is not available\n" + e.getStackTrace());
                            Toast.makeText(_context, "ERROR: Network is not available", Toast.LENGTH_LONG).show();
                        }

                    }


                }

                public void onPlayStopped() {
                    Log.i(TAG, "onPlayStopped");
                }

                public void onPlayFailed(Exception e) {
                    Log.e(TAG, "onPlayFailed");
                    AlertDialog.Builder builder = new AlertDialog.Builder(SubActivity.this);
                    builder.setMessage("Error[TTSActivity]: " + e.toString())
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    AlertDialog alert = builder.create();
                    //alert.show();
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

