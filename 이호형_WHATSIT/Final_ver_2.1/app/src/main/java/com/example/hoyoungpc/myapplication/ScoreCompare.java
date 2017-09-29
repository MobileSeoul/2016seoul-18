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

import java.util.Comparator;

//Map의 Value를 정렬하기 위한 비교함수
class ScoreCompare implements Comparator<LabelVO> {
    @Override
    public int compare(LabelVO vo1, LabelVO vo2) {
        if(vo1.getScore()!=null&&vo2.getScore()!=null)
        return vo2.getScore().compareTo(vo1.getScore());
        return 0;
    }

}