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

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

//뒤로 가기 누르면 종료하는 기능
public class BackPressCloseHandler {
    private long backKeyPressedTime = 0;
    private Toast toast;

    private Activity activity;

    public BackPressCloseHandler(Activity context) {
        this.activity = context;
    }

    public void onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            showGuide();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            toast.cancel();

            Intent t = new Intent(activity, MainActivity.class);
            activity.startActivity(t);

            activity.moveTaskToBack(true);
            activity.finish();
            android.os.Process.killProcess(android.os.Process.myPid());//현재 process 종료
        }
    }

    public void showGuide() {
        toast = Toast.makeText(activity, "초기화 완료. 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT);
        toast.show();
        /*
        //뒤로 가기 터치시 현재 액티비티 종료 및 temp액티비티 실행. temp액티비티는 cameraACtivity실행시키고 죽음
        Intent t = new Intent(activity,TempActivity.class);
        activity.startActivity(t);
        activity.finish();*/


    }

}