/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.hoyoungpc.myapplication;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.ImageContext;

import org.ispeech.SpeechSynthesis;
import org.ispeech.SpeechSynthesisEvent;
import org.ispeech.error.BusyException;
import org.ispeech.error.InvalidApiKeyException;
import org.ispeech.error.NoNetworkException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

//카메라, 갤러리에서 선택 가능한 버젼.
//20161016 오전 갤러리 기능 제거, TTS기능 입히기
//20161016 오후 매뉴얼 본적 한번도 없을 때만 매뉴얼로 넘어가게 하는 기능 추가.
//20161024 디자인 입힘.
//20161025 카메라로 여러번 찍을 수록 정확도 점점 포아짐(예를 들어 첫번째 코카콜라 90%, 두번째 10%, 세번째 1% 인 경우 90% 최고치로 저장), 음성 출력 중에 사진 또 찍으면 종료하는게 나으려나??
//20161028_2 자동으로 카메라 계속 찍도록 수정.
//20161030 Aging개념 도입으로 오래된 태그는 자동으로 지워지도록.
public class CameraActivity extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyDtIuusIOM4Bv6SCWIC5wkiE8_ZwlmVrsg";
    public static final String FILE_NAME = "temp.jpg";
    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    public static final int NUM_OF_RESPONSE = 10;//한번의 request마다 API에서 받아오는 response 결과 수
    public static final int MAX_AGE = 5;//MAX_AGE만큼 나이가 든 label은 삭제함. label이 재등장하면 초기화
    public static final int MAX_SHOW_RESULT = 6;//분석 결과를 몇개까지 알려줄 지 정함. 너무 많으면 한글 변환시 오류발생
    //자동 카메라 촬영
    ImageView iv_preview;
    private TextView mImageDetails;
    private ImageView mMainImage;
    private Timer timer;
    SurfaceView sv_viewFinder;
    SurfaceHolder sh_viewFinder;
    Camera camera;
    boolean inProgress = false;


    //englishTTS
    public TextToSpeech tts;
    //koreaTTS
    SpeechSynthesis synthesis;
    Context _context;

    //정확도 높이기 : 결과 분석해서 매번 제일 높은 순위 알려주기
    ArrayList<LabelVO> labelList = new ArrayList<LabelVO>();//HashMap + aging개념을 도입한 리스트
    LabelVO vo;
    int labelListSize = 0;
    boolean isVoCreated = false;

    StringBuilder logoFound = new StringBuilder();//로고는 따로 저장했다가 한글 말고 영어로 TTS해줌.
    StringBuffer analyEngResult;//결과 저장
    StringBuffer analyKorResult;//한국어 번역 결과 저장
    String korean2;
    //뒤로 가기 핸들러
    private BackPressCloseHandler backPressCloseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        //자동 촬영레이아웃 설정
        mImageDetails = (TextView) findViewById(R.id.image_details);
        mMainImage = (ImageView) findViewById(R.id.main_image);
        sv_viewFinder = (SurfaceView) findViewById(R.id.sv_viewFinder);
        sh_viewFinder = sv_viewFinder.getHolder();
        sh_viewFinder.addCallback(surfaceListener);
        sh_viewFinder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        iv_preview = (ImageView) findViewById(R.id.iv_preview);

        //TTS Init한 후에 speek함수 호출 가능.
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    //tts.setLanguage(Locale.KOREAN);
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });


        // 1초 뒤 자동촬영 시작해서 5초마다 계속 찍음. autosFocus추가
        timer = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                Log.d("list", "사진 찍기 scheduler동작!");
                startTakePicture();

                //labelList의 aging을 증가시킴.
                for (int i = 0; i < labelList.size(); i++) {
                    labelList.get(i).setAge(labelList.get(i).getAge() + 1);
                    if (labelList.get(i).getAge() > MAX_AGE) {//나이가 최대치가 넘으면 제거함.
                        Log.d("list", "오래된 항목이라 지워집니다 : " + labelList.get(i).getKey());
                        labelList.remove(i);
                    }
                }


            }
        };
        timer.schedule(tt, 3000, 5000);

        //koreaTTS
        //20161018 hyson
        _context = this.getApplicationContext();
        prepareTTSEngine();
        synthesis.setStreamType(AudioManager.STREAM_MUSIC);

        // 뒤로가기 핸들러
        backPressCloseHandler = new BackPressCloseHandler(this);

        Log.d("list", "1VO created again!");
        vo = new LabelVO();

    }

    // 뒤로가기 핸들러
    @Override
    public void onBackPressed() {
        Log.d("list", "초기화 완료. 다음 물체를 찍어주세요");
        //기존리스트 지우기(다시 3번 찍어야함)
        //resultList.clear();
        labelList.clear();
        mImageDetails.setText("초기화됨");

        backPressCloseHandler.onBackPressed();
    }

    //koreaTTS
    //20161018 hyson
    private void prepareTTSEngine() {
        try {
            synthesis = SpeechSynthesis.getInstance(this);
            synthesis.setSpeechSynthesisEvent(new SpeechSynthesisEvent() {

                public void onPlaySuccessful() {
                    Log.i(TAG, "onPlaySuccessful");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && logoFound.length() > 0) {//로고 탐색이 되었으면 한국어 읽은 후, 영어로 읽어줌.(로고는 자체 고유명사이므로 영어로)
                        ttsGreater21(logoFound.toString());//TTS읽어주기(영어버젼)
                    } else {
                        ttsUnder20(logoFound.toString());//TTS읽어주기(영어버젼)
                    }
                }

                public void onPlayStopped() {
                    Log.i(TAG, "onPlayStopped");
                }

                public void onPlayFailed(Exception e) {
                    Log.e(TAG, "onPlayFailed" + e.toString());

                    Log.d("list", e.toString());
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

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        Log.d("list", "callcloudvision called");
        // Switch text to loading
        //mImageDetails.setText(R.string.loading_message);

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(new
                            VisionRequestInitializer(CLOUD_VISION_API_KEY));
                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        // add the features we want
                        List<Feature> featureList = new ArrayList<>();
                        Feature labelDetection = new Feature();
                        labelDetection.setType("LABEL_DETECTION");
                        labelDetection.setMaxResults(NUM_OF_RESPONSE);
                        featureList.add(labelDetection);

                        Feature landmarkDetection = new Feature();
                        landmarkDetection.setType("LANDMARK_DETECTION");
                        landmarkDetection.setMaxResults(NUM_OF_RESPONSE);
                        featureList.add(landmarkDetection);

                        Feature logoDetection = new Feature();
                        logoDetection.setType("LOGO_DETECTION");
                        logoDetection.setMaxResults(NUM_OF_RESPONSE);
                        featureList.add(logoDetection);

                            /*
                            Feature textDetection = new Feature();
                            textDetection.setType("TEXT_DETECTION");
                            textDetection.setMaxResults(NUM_OF_RESPONSE);
                            featureList.add(textDetection);
    */
                        //이미지를 base64로 전환 후 google에 전송해야함.
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();
                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);
                        annotateImageRequest.setFeatures(featureList);

                        //한국어 세팅
                        ImageContext imageContext = new ImageContext();
                        String[] languages = {"ko"};
                        imageContext.setLanguageHints(Arrays.asList(languages));
                        annotateImageRequest.setImageContext(imageContext);


                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {
                //결과 출력
                mImageDetails.setText(result);
            }
        }.execute();
    }


    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        StringBuilder english = new StringBuilder();
        logoFound = new StringBuilder();
        //message.append("한글읽기 테스트");

        //message.append("Logos:\n");
        List<EntityAnnotation> logos = response.getResponses().get(0)
                .getLogoAnnotations();
        if (logos != null) {//로고를 찾은 경우
            for (EntityAnnotation logo : logos) {
                //message.append(String.format(Locale.getDefault(), "%.3f: %s",logo.getScore(), logo.getDescription()));
                english.append(String.format(Locale.getDefault(), "%s", logo.getDescription()));
                english.append(".\n");
                logoFound.append(String.format(Locale.getDefault(), "%s", logo.getDescription()));

                if (vo != null) {
                    vo.setKey(logo.getDescription());
                    vo.setScore(100.0);
                    vo.setAge(0);
                    //로고는 찾으면 무조건 리스트에 추가함.(한번 찾은 로고는 몹시 정확하므로)
                    labelList.add(vo);
                }
            }
        }

        //message.append("Landmarks:\n");
        List<EntityAnnotation> landmarks = response.getResponses().get(0)
                .getLandmarkAnnotations();
        if (landmarks != null) {
            for (EntityAnnotation landmark : landmarks) {
                //message.append(String.format(Locale.getDefault(), "%.3f: %s",landmark.getScore(), landmark.getDescription()));
                english.append(String.format(Locale.getDefault(), "%s", landmark.getDescription()));
                english.append(".\n");
                //resultList에 넣는데 중복이면 확률 높은 것으로 덮어씀.

                if (vo != null) {
                    vo.setKey(landmark.getDescription());
                    vo.setScore(100.0);
                    vo.setAge(0);
                    //랜드마크도 찾으면 무조건 리스트에 추가함.(한번 찾은 랜드마크는 몹시 정확하므로)
                    labelList.add(vo);
                }
            }
        }

        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            Log.d("list", "=======구글 결과 Label========");
            for (int i = 0; i < labels.size(); i++) {
                Log.d("list", labels.get(i).getDescription() + "," + labels.get(i).getScore());
            }
            Log.d("list", "=======구글 결과 Label========");
            for (EntityAnnotation label : labels) {
                if (label == null) {//null이면 아래 로직 안 탐. null exception나므로
                    continue;
                }
                //message.append(String.format(Locale.getDefault(), "%.3f: %s",label.getScore(), label.getDescription()));
                english.append(String.format(Locale.getDefault(), "%s", label.getDescription()));
                english.append(".\n");

                //message.append("Labels:\n");


                if (vo != null) {//vo가 생성되었으면..
                    // 쓰레드 새로 만들어서 실행(언제 실행될지 정확히 모름)
                    Handler mHandler = new Handler(Looper.getMainLooper());
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isVoCreated) {//아래 쓰레드가 완료되면 isVoCreate=false가 됨.
                                Log.d("list", "2VO created again!");
                                vo = new LabelVO();
                                isVoCreated = true;
                            }
                        }
                    }, 0);

                    if (isVoCreated) {//위 쓰레드에서 VO가 생성된 경우에 실행.
                        vo.setKey(label.getDescription());
                        Log.d("list", "==새로운 항목1: " + vo.getKey() + "," + vo.getScore() + "," + vo.getAge());

                        vo.setScore(Double.parseDouble(label.getScore().toString()));
                        Log.d("list", "==새로운 항목2: " + vo.getKey() + "," + vo.getScore() + "," + vo.getAge());

                        vo.setAge(0);
                        Log.d("list", "==새로운 항목3: " + vo.getKey() + "," + vo.getScore() + "," + vo.getAge());

                        boolean isDuplicated = false;
                        for (int i = 0; i < labelList.size(); i++) {
                            Log.d("list", "labelList[" + i + "] 비교 :" + labelList.get(i).getKey() + " VS " + vo.getKey());

                            if (labelList.get(i).getKey().equals(vo.getKey())) {//중복항목이 있는 경우,
                                Log.d("list", "중복항목은 score와 age를 업데이트합니다. : " + labelList.get(i).getKey() + " = " + vo.getKey());
                                isDuplicated = true;//중복항목은 update만 하고 insert안 함.
                                labelList.get(i).setAge(0);//중복항목은 다시 젊음을 찾음
                                if (labelList.get(i).getScore() < vo.getScore()) {//점수가 높으면 업데이트한다.
                                    labelList.get(i).setScore(Double.parseDouble(vo.getScore().toString()));
                                    Log.d("list", "vo updated!");
                                }
                                break;//중복 항목은 항상 하나로 유지되므로 쓸데없는 연산방지로 for문 break해도 됨.
                            }
                        }

                        if (labelList.size() == 0) {//리스트에 비교대상이 없으면 바로 넣음.
                            Log.d("list", "첫 항목이므로 바로 추가함");
                            labelList.add(vo);
                        } else if (!isDuplicated) {//비교대상이 있는 경우 -> 1중복이면 위에 for문에서 update만 하고 2. 중복없으면 새로 넣음
                            Log.d("list", "중복항목 없으므로 새로 추가함");
                            labelList.add(vo);
                        }


                        labelListSize = labelList.size();
                        for (int i = 0; i < labelList.size(); i++) {
                            Log.d("list", "현재리스트" + labelList.get(i).getKey() + "," + labelList.get(i).getScore() + "," + labelList.get(i).getAge());
                        }

                        //실행이 끝났으면 위 쓰레드에 알려줌
                        isVoCreated = false;
                    }
                }
            }
        }


        //message.append("TEXT:\n");
            /*
            List<EntityAnnotation> texts = response.getResponses().get(0)
                    .getTextAnnotations();
            if (texts != null) {
                for (EntityAnnotation text : texts) {
                    english.append(String.format(Locale.getDefault(), "%s",text.getDescription()));
                    english.append(".\n");
                }
            } else {
                //message.append("nothing\n");
            }
            */
        //TTS읽어주기(한글버젼)
        try

        {
            //resultList를 정렬해서 sortedResultList에 담기
            Log.d("list", "=====정렬하기 전=====size : " + labelList.size());
            for (int i = 0; i < labelList.size(); i++) {
                Log.d("list", labelList.get(i).getKey() + "," + labelList.get(i).getScore());
            }
            Collections.sort(labelList, new ScoreCompare());
            Log.d("list", "=======정렬 후========");
            for (int i = 0; i < labelList.size(); i++) {
                Log.d("list", labelList.get(i).getKey() + "," + labelList.get(i).getScore());
            }

            //너무 많으면 한글 변환 오류남. 충분히 적어질 때까지 뒤에서부터 지움
            while (labelList.size() > MAX_SHOW_RESULT) {
                Log.d("list", MAX_SHOW_RESULT + "개를 초과하여 삭제합니다 : " + labelList.get(labelList.size() - 1).getKey());
                labelList.remove(labelList.size() - 1);
            }
            Log.d("list", "=======삭제 후========");
            for (int i = 0; i < labelList.size(); i++) {
                Log.d("list", labelList.get(i).getKey() + "," + labelList.get(i).getScore());
            }

            //labelList의 모든 label값을 갖고와서 출력할 StringBuffer analyResult에 저장한다.
            analyEngResult = new StringBuffer();
            for (LabelVO iter : labelList) {
                iter.setAge(iter.getAge() + 1);
                analyEngResult.append(iter.getKey());
                analyEngResult.append(",");
            }

            //정렬된 목록을 한글로 번역함
            GoogleTranslate translator = new GoogleTranslate(CLOUD_VISION_API_KEY);
            korean2 = translator.translte(analyEngResult.toString(), "en", "ko");
            Log.d("list", "TTS list : " + korean2);

            synthesis.addOptionalCommand("Voice", "krkoreanfemale");
            synthesis.speak(korean2);

        } catch (
                BusyException e
                )

        {
            Log.e(TAG, "SDK is busy");
            e.printStackTrace();
            Toast.makeText(_context, "ERROR: SDK is busy", Toast.LENGTH_LONG).show();
        } catch (
                NoNetworkException e
                )

        {
            Log.e(TAG, "Network is not available\n" + e.getStackTrace());
            Toast.makeText(_context, "ERROR: Network is not available", Toast.LENGTH_LONG).show();
        }

        Log.d("list", english.toString());
        return korean2;
    }

    @Override
    public void onStop() {
        if (tts != null) {
            tts.stop();
        }

        //홈버튼 누르면 종료함.
        finish();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        synthesis.stop();    //여기 주석을 풀면, 카메라로 사진 찍을 때마다, 음성이 stop되고 처음부터 재생된다. 뭐가 더 나을까??
        //주석 하면 이전 음성이 끝나야 다음 음성이 재생된다.
        super.onPause();
    }

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        //map.put(TextToSpeech.Engine.KEY_PARAM_VOLUME,"1.0");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId = this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    ////////카메라 자동촬영 함수///////////
    public SurfaceHolder.Callback surfaceListener = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i("list", "sufraceListener 카메라 미리보기 활성");
            Camera.Parameters parameters = camera.getParameters();
            //*EDIT*//params.setFocusMode("continuous-picture");
            //It is better to use defined constraints as opposed to String, thanks to AbdelHady
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d("Version", "Marshmellow");
            } else {
                parameters.setRotation(90);
            }
            parameters.setPreviewSize(width, height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            for (Camera.Size size : parameters.getSupportedPictureSizes()) {
                // 640 480
                // 960 720
                // 1024 768
                // 1280 720
                // 1600 1200
                // 2560 1920
                // 3264 2448
                // 2048 1536
                // 3264 1836
                // 2048 1152
                // 3264 2176
                if (1600 <= size.width & size.width <= 1920) {
                    parameters.setPreviewSize(size.width, size.height);
                    parameters.setPictureSize(size.width, size.height);
                    break;
                }
            }
            camera.setDisplayOrientation(90);
            camera.setParameters(parameters);
            camera.startPreview();
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i("list", "sufraceListener 카메라 오픈");

            int int_cameraID = 0;
                /* 카메라가 여러개 일 경우 그 수를 가져옴  */
            int numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);

                // 전면카메라
                //                if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT)
                //                    int_cameraID = i;
                // 후면카메라
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                    int_cameraID = i;
            }

            camera = Camera.open(int_cameraID);
            //set camera to continually auto-focus


            try {
                camera.setPreviewDisplay(sh_viewFinder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i("list", "sufraceListener 카메라 해제");
            timer.cancel();
            if (camera != null)//이미 releasee됬는지 확인
                camera.release();
            camera = null;
        }
    };


    public void startTakePicture() {
        if (camera != null && inProgress == false) {
            camera.takePicture(null, null, takePicture);
            inProgress = true;
        }
    }


    public Camera.PictureCallback takePicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d("list", "=== takePicture ===");

            if (data != null) {
                Log.v("list", "takePicture JPEG 사진 찍음");

                //찍은 사진을 비트맵으로 갖고 오기
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                //사진 90도 돌리기
                Matrix m = new Matrix();
                m.postRotate(90);
                //내 폰의 최대 Texture 출력 사이즈 갖고 오기
                int[] maxTextureSize = new int[1];
                GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
                //내 폰의 최대 Texure 출력 사이즈만큼으로 강제 resizing하기 90도 돌릴것이므로 width,height반대로 설정
                if (bitmap.getHeight() > maxTextureSize[0]) {
                    int resizedWidth = 400;
                    int resizedHeight = 240;
                    bitmap = bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, resizedWidth, resizedHeight, m, false);
                }
                //iv_preview.setImageBitmap(bitmap);

                try {
                    //클라우드 비젼 호출 전에 vo를 새로 만들어야 함.

                    iv_preview.setImageBitmap(bitmap);
                    callCloudVision(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (camera != null)
                    camera.startPreview();
                inProgress = false;
            } else {
                Log.e("list", "takePicture data null");
            }
        }
    };

}
