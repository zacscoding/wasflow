/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wasflow.util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class FileUtil {

    public static InputStream close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (Throwable t) {
                // ignore
            }
        }

        return null;
    }

    public static OutputStream close(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (Throwable t) {
                // ignore
            }
        }

        return null;
    }

    public static Reader close(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (Throwable t) {
                // ignore
            }
        }

        return null;
    }

    public static Writer close(Writer writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (Throwable t) {
                // ignore
            }
        }

        return null;
    }


    public static byte[] readAll(InputStream fin) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        int n = fin.read(buff);
        while (n >= 0) {
            out.write(buff, 0, n);
            n = fin.read(buff);
        }
        return out.toByteArray();
    }
}