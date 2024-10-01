/*
 * Copyright 2011 Jeremias Maerki, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: Util.java 1683 2011-09-27 14:16:23Z jeremias $ */

package ch.jm.osgi.provisioning;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Dictionary;
import java.util.Enumeration;

class Util {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Copy characters from a {@link Reader} to a {@link Writer}.
     * @param input the Reader to read from
     * @param output the Writer to write to
     * @return the number of characters copied
     * @throws IOException if an I/O error occurs
     */
    public static long copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Copy bytes from an {@link InputStream} to an {@link OutputStream}.
     * @param input the InputStream to read from
     * @param output the OutputStream to write to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    public static long copy(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Closes an {@link InputStream} ignoring any {@link IOException} in the process.
     * @param in the input stream
     */
    public static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {
                //ignore
            }
        }
    }

    /**
     * Closes an {@link OutputStream} ignoring any {@link IOException} in the process.
     * @param out the output stream
     */
    public static void closeQuietly(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ioe) {
                //ignore
            }
        }
    }

    /**
     * Turns the given {@link Dictionary} into a read-only one.
     * @param dict the dictionary
     * @return the read-only version of the dictionary
     */
    public static Dictionary unmodifiableDictionary(Dictionary dict) {
        return new UnmodifiableDictionary(dict);
    }

    private static final class UnmodifiableDictionary extends Dictionary {

        private Dictionary dict;

        public UnmodifiableDictionary(Dictionary dict) {
            this.dict = dict;
        }

        public Object get(Object key) {
            return this.dict.get(key);
        }

        public boolean isEmpty() {
            return this.dict.isEmpty();
        }

        public Enumeration keys() {
            return this.dict.keys();
        }

        public Enumeration elements() {
            return this.dict.elements();
        }

        public Object put(Object key, Object value) {
            readOnly();
            return null;
        }

        public Object remove(Object key) {
            readOnly();
            return null;
        }

        private void readOnly() {
            throw new UnsupportedOperationException("This Dictionary is read-only!");
        }

        public int size() {
            return this.dict.size();
        }

    }

}
