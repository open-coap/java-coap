/**
 * Copyright (C) 2011-2018 ARM Limited. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
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
package com.mbed.lwm2m.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

class NumberTypeAdapter extends TypeAdapter<Number> {

    @Override
    public void write(JsonWriter writer, Number value) throws IOException {
        if (value != null) {
            writer.value(value.toString() );
        } else {
            writer.nullValue();
        }
    }

    @Override
    public Number read(JsonReader reader) throws IOException {
        String string = reader.nextString();
        Number number;
        
        if (string.indexOf('.') == -1) {
            number = Integer.parseInt(string);
        } else {
            number = Double.parseDouble(string);
        }
        
        return number;
    }

}
