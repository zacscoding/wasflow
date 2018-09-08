/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wasflow.agent.trace.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

public class ShiroAntPathMatcher {

    private String pathSeparator = "/";

    public boolean match(String pattern, String path) {
        return this.doMatch(pattern, path, true);
    }

    private boolean doMatch(String pattern, String path, boolean fullMatch) {
        if (pattern == null) {
            return true;
        }
        if (path == null) {
            return false;
        }

        if (path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
            return false;
        } else {
            String[] pattDirs = tokenizeToStringArray(pattern, this.pathSeparator, true, true);
            String[] pathDirs = tokenizeToStringArray(path, this.pathSeparator, true, true);
            int pattIdxStart = 0;
            int pattIdxEnd = pattDirs.length - 1;
            int pathIdxStart = 0;

            int pathIdxEnd;
            String patDir;
            for (pathIdxEnd = pathDirs.length - 1; pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd; ++pathIdxStart) {
                patDir = pattDirs[pattIdxStart];
                if ("**".equals(patDir)) {
                    break;
                }

                if (!this.matchStrings(patDir, pathDirs[pathIdxStart])) {
                    return false;
                }

                ++pattIdxStart;
            }

            int patIdxTmp;
            if (pathIdxStart > pathIdxEnd) {
                if (pattIdxStart > pattIdxEnd) {
                    return pattern.endsWith(this.pathSeparator) ? path.endsWith(this.pathSeparator) : !path.endsWith(this.pathSeparator);
                } else if (!fullMatch) {
                    return true;
                } else if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(this.pathSeparator)) {
                    return true;
                } else {
                    for (patIdxTmp = pattIdxStart; patIdxTmp <= pattIdxEnd; ++patIdxTmp) {
                        if (!pattDirs[patIdxTmp].equals("**")) {
                            return false;
                        }
                    }

                    return true;
                }
            } else if (pattIdxStart > pattIdxEnd) {
                return false;
            } else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
                return true;
            } else {
                while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
                    patDir = pattDirs[pattIdxEnd];
                    if (patDir.equals("**")) {
                        break;
                    }

                    if (!this.matchStrings(patDir, pathDirs[pathIdxEnd])) {
                        return false;
                    }

                    --pattIdxEnd;
                    --pathIdxEnd;
                }

                if (pathIdxStart > pathIdxEnd) {
                    for (patIdxTmp = pattIdxStart; patIdxTmp <= pattIdxEnd; ++patIdxTmp) {
                        if (!pattDirs[patIdxTmp].equals("**")) {
                            return false;
                        }
                    }

                    return true;
                } else {
                    while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
                        patIdxTmp = -1;

                        int patLength;
                        for (patLength = pattIdxStart + 1; patLength <= pattIdxEnd; ++patLength) {
                            if (pattDirs[patLength].equals("**")) {
                                patIdxTmp = patLength;
                                break;
                            }
                        }

                        if (patIdxTmp == pattIdxStart + 1) {
                            ++pattIdxStart;
                        } else {
                            patLength = patIdxTmp - pattIdxStart - 1;
                            int strLength = pathIdxEnd - pathIdxStart + 1;
                            int foundIdx = -1;
                            int i = 0;

                            label140:
                            while (i <= strLength - patLength) {
                                for (int j = 0; j < patLength; ++j) {
                                    String subPat = pattDirs[pattIdxStart + j + 1];
                                    String subStr = pathDirs[pathIdxStart + i + j];
                                    if (!this.matchStrings(subPat, subStr)) {
                                        ++i;
                                        continue label140;
                                    }
                                }

                                foundIdx = pathIdxStart + i;
                                break;
                            }

                            if (foundIdx == -1) {
                                return false;
                            }

                            pattIdxStart = patIdxTmp;
                            pathIdxStart = foundIdx + patLength;
                        }
                    }

                    for (patIdxTmp = pattIdxStart; patIdxTmp <= pattIdxEnd; ++patIdxTmp) {
                        if (!pattDirs[patIdxTmp].equals("**")) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        }
    }

    private boolean matchStrings(String pattern, String str) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        boolean containsStar = false;
        char[] var11 = patArr;
        int patLength = patArr.length;

        int strLength;
        for (strLength = 0; strLength < patLength; ++strLength) {
            char aPatArr = var11[strLength];
            if (aPatArr == '*') {
                containsStar = true;
                break;
            }
        }

        char ch;
        int patIdxTmp;
        if (!containsStar) {
            if (patIdxEnd != strIdxEnd) {
                return false;
            } else {
                for (patIdxTmp = 0; patIdxTmp <= patIdxEnd; ++patIdxTmp) {
                    ch = patArr[patIdxTmp];
                    if (ch != '?' && ch != strArr[patIdxTmp]) {
                        return false;
                    }
                }

                return true;
            }
        } else if (patIdxEnd == 0) {
            return true;
        } else {
            while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
                if (ch != '?' && ch != strArr[strIdxStart]) {
                    return false;
                }

                ++patIdxStart;
                ++strIdxStart;
            }

            if (strIdxStart > strIdxEnd) {
                for (patIdxTmp = patIdxStart; patIdxTmp <= patIdxEnd; ++patIdxTmp) {
                    if (patArr[patIdxTmp] != '*') {
                        return false;
                    }
                }

                return true;
            } else {
                while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
                    if (ch != '?' && ch != strArr[strIdxEnd]) {
                        return false;
                    }

                    --patIdxEnd;
                    --strIdxEnd;
                }

                if (strIdxStart > strIdxEnd) {
                    for (patIdxTmp = patIdxStart; patIdxTmp <= patIdxEnd; ++patIdxTmp) {
                        if (patArr[patIdxTmp] != '*') {
                            return false;
                        }
                    }

                    return true;
                } else {
                    while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
                        patIdxTmp = -1;

                        for (patLength = patIdxStart + 1; patLength <= patIdxEnd; ++patLength) {
                            if (patArr[patLength] == '*') {
                                patIdxTmp = patLength;
                                break;
                            }
                        }

                        if (patIdxTmp == patIdxStart + 1) {
                            ++patIdxStart;
                        } else {
                            patLength = patIdxTmp - patIdxStart - 1;
                            strLength = strIdxEnd - strIdxStart + 1;
                            int foundIdx = -1;
                            int i = 0;

                            label132:
                            while (i <= strLength - patLength) {
                                for (int j = 0; j < patLength; ++j) {
                                    ch = patArr[patIdxStart + j + 1];
                                    if (ch != '?' && ch != strArr[strIdxStart + i + j]) {
                                        ++i;
                                        continue label132;
                                    }
                                }

                                foundIdx = strIdxStart + i;
                                break;
                            }

                            if (foundIdx == -1) {
                                return false;
                            }

                            patIdxStart = patIdxTmp;
                            strIdxStart = foundIdx + patLength;
                        }
                    }

                    for (patIdxTmp = patIdxStart; patIdxTmp <= patIdxEnd; ++patIdxTmp) {
                        if (patArr[patIdxTmp] != '*') {
                            return false;
                        }
                    }

                    return true;
                }
            }
        }
    }

    private String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
        if (str == null) {
            return null;
        } else {
            StringTokenizer st = new StringTokenizer(str, delimiters);
            ArrayList tokens = new ArrayList();

            while (true) {
                String token;
                do {
                    if (!st.hasMoreTokens()) {
                        return toStringArray(tokens);
                    }

                    token = st.nextToken();
                    if (trimTokens) {
                        token = token.trim();
                    }
                } while (ignoreEmptyTokens && token.length() <= 0);

                tokens.add(token);
            }
        }
    }

    private String[] toStringArray(Collection collection) {
        return collection == null ? null : (String[]) collection.toArray(new String[collection.size()]);
    }
}