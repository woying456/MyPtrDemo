package com.android.gmacs.utils;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import com.common.gmacs.utils.StringUtil;

public class HighLightTextUtil {

    /*static int threePointWidth = 0;
    final static String ELLIPSIS = "…";

    public static String getSuitableString(String text, String target, int index, int maxWidth, TextView tv) {
        if (measureText(tv, target) >= maxWidth) {
            return target;
        }

        if (measureText(tv, text) <= maxWidth) {
            return text;
        }

        if (target.equals(text)) {
            return target;
        }

        threePointWidth = measureText(tv, ELLIPSIS);

        String res = null;

        int start = index;
        int end = target.length() + start;

        if (end == text.length() && start > 0) {
            // AAAAAAAAAA哈哈
            int i = 0;
            for (i = start; i >= 0; i--) {
                res = ELLIPSIS + text.substring(i - 1, text.length());
                int w = measureText(tv, res);
                if (w > maxWidth) {//加了一个字符和… 超界了 判断是舍去…还是字符
                    //   去掉刚刚加的字符 保留…
                    res = ELLIPSIS + text.substring(i, text.length());
                    int ww = measureText(tv, res);
                    if (ww <= maxWidth) {
                        return res;
                    } else {
                        // 再判断去掉… 保留字符
                        res = text.substring(i - 1, text.length());
                        int www = measureText(tv, res);
                        if (www <= maxWidth) {
                            return res;
                        } else {
                            // 去掉两个字符 加个…
                            return ELLIPSIS + text.substring(i + 1, text.length());
                        }
                    }
                }
            }
        } else {
            // AAAAAA哈哈AAAA   判断 …哈哈… 的长度
            res = ELLIPSIS + target + ELLIPSIS;
            int tw = measureText(tv, res);

            if (tw > maxWidth) {
                // 判断 …哈哈 的长度
                res = ELLIPSIS + target;
                int tww = measureText(tv, res);
                if (tww > maxWidth) {
                    return target;
                } else {
                    return res;
                }
            }

            // 可以构成 …哈哈…
            int i;
            int j;
            for (i = start - 1, j = end; i >= 0 && j < text.length(); i--, j++) {
                res = ELLIPSIS + text.substring(i, j + 1) + ELLIPSIS;
                int w = measureText(tv, res);
                if (w < maxWidth) {
                } else if (w == maxWidth) {
                    return res;
                } else {
                    // w>maxWidth
                    // 去除规则
                    // 先去掉右边的一个字符 然后再去掉右边的…
                    // 再然后去掉左边的一个字符 最后去掉左边的…
                    // 还不行就去掉左右两边各一个字符
                    //去掉右边的一个字符
                    res = ELLIPSIS + text.substring(i, j) + ELLIPSIS;
                    int ww = measureText(tv, res);
                    if (ww <= maxWidth) {
                        return res;
                    } else {
                        // 去掉右边的…
                        res = ELLIPSIS + text.substring(i, j + 1);
                        int www = measureText(tv, res);
                        if (www <= maxWidth) {
                            return res;
                        } else {
                            // 去掉左边的一个字符
                            res = ELLIPSIS + text.substring(i + 1, j) + ELLIPSIS;
                            int wwww = measureText(tv, res);
                            if (wwww <= maxWidth) {
                                return res;
                            } else {
                                // 去掉左边的…
                                res = text.substring(i, j + 1) + ELLIPSIS;
                                int wwwww = measureText(tv, res);
                                if (wwwww <= maxWidth) {
                                    return res;
                                } else {
                                    return ELLIPSIS + text.substring(i + 1, j) + ELLIPSIS;
                                }
                            }
                        }
                    }
                }
            }
            if (i < 0 && j < text.length()) {
                //左边先结束了,右边还需要接着判断
                for (int k = j; k < text.length(); k++) {
                    res = text.substring(0, k + 1) + ELLIPSIS;
                    int w = measureText(tv, res);
                    ////////////
                    if (w > maxWidth) {
                        //加了一个字符和… 超界了 判断是舍去…还是字符
                        // 去掉刚刚加的字符 保留…
                        res = text.substring(0, k) + ELLIPSIS;
                        int ww = measureText(tv, res);
                        if (ww <= maxWidth) {
                            return res;
                        } else {
                            // 再判断去掉… 保留字符
                            res = text.substring(0, k + 1);
                            int www = measureText(tv, res);
                            if (www <= maxWidth) {
                                return res;
                            } else {
                                // 去掉两个字符 加个…
                                return text.substring(0, k - 1) + ELLIPSIS;
                            }
                        }
                    }
                }
            } else if (i > 0 && j >= text.length()) {
                // 右边先结束了，左边还需要接着判断
                for (int k = i; k >= 0; k--) {
                    res = ELLIPSIS + text.substring(k, text.length());
                    int w = measureText(tv, res);
                    if (w > maxWidth) {//加了一个字符和… 超界了 判断是舍去…还是字符
                        //   去掉刚刚加的字符 保留…
                        res = ELLIPSIS + text.substring(k + 1, text.length());
                        int ww = measureText(tv, res);
                        if (ww <= maxWidth) {
                            return res;
                        } else {
                            // 再判断去掉… 保留字符
                            res = text.substring(k, text.length());
                            int www = measureText(tv, res);
                            if (www <= maxWidth) {
                                return res;
                            } else {
                                // 去掉两个字符 加个…
                                return ELLIPSIS + text.substring(k + 1, text.length());
                            }
                        }
                    }
                }
            } else if (i == 0 && j == text.length()) {
                // 两边同时结束
                return target;
            }
        }
        return res;
    }*/

    private static final String ELLIPSIS = "…";
//    private static final int PREFIX_CHARACTER_LENGTH = 8;
//    private static float ellipsisWidth;
//
//    public static SpannableString highlightText(String keyword, String text, TextView textView, int maxWidth, String textColor) {
//        SpannableString spannableString;
//        StringBuilder stringBuilder = new StringBuilder();
//        if (ellipsisWidth == 0) {
//            ellipsisWidth = measureText(textView, ELLIPSIS);
//        }
//        float keywordWidth = measureText(textView, keyword);
//
//        if (text.indexOf(keyword) == 0) {
//            if (measureText(textView, text) <= maxWidth) {
//                spannableString = toSpannableString(text, keyword, textColor);
//            } else {
//                char[] chars = text.toCharArray();
//                for (char c : chars) {
//                    if (measureText(textView, stringBuilder.toString() + c) <= maxWidth) {
//                        stringBuilder.append(c);
//                    } else {
//                        stringBuilder.append(ELLIPSIS);
//                        break;
//                    }
//                }
//                spannableString = toSpannableString(stringBuilder.toString(), keyword, textColor);
//            }
//        } else if (text.lastIndexOf(keyword) == text.length() - keyword.length() &&
//                text.indexOf(keyword) == text.length() - keyword.length()) {
//            if (measureText(textView, text) <= maxWidth) {
//                spannableString = toSpannableString(text, keyword, textColor);
//            } else {
//                char[] chars = text.toCharArray();
//                for (int i = chars.length - 1; i >= 0; i--) {
//                    if (measureText(textView, stringBuilder.toString() + chars[i]) <= maxWidth - ellipsisWidth) {
//                        stringBuilder.append(chars[i]);
//                    } else {
//                        stringBuilder.append(ELLIPSIS);
//                        break;
//                    }
//                }
//                spannableString = toSpannableString(stringBuilder.reverse().toString(), keyword, textColor);
//            }
//        } else {
//            if (measureText(textView, text) <= maxWidth) {
//                spannableString = toSpannableString(text, keyword, textColor);
//            } else {
//                int start = text.indexOf(keyword);
//                int end = start + keyword.length();
//                if (start >= PREFIX_CHARACTER_LENGTH) {
//                    char[] chars = text.toCharArray();
//                    float prefixPlusWidth = measureText(textView, text.substring(start - PREFIX_CHARACTER_LENGTH));
//                    int i;
//                    if (prefixPlusWidth < maxWidth) {
//                        for (i = start - 1 - PREFIX_CHARACTER_LENGTH; i >= 0; i--) {
//                            if (measureText(textView, stringBuilder.toString() + chars[i]) <= maxWidth - prefixPlusWidth - ellipsisWidth) {
//                                stringBuilder.append(chars[i]);
//                            } else {
//                                stringBuilder.append(ELLIPSIS);
//                                break;
//                            }
//                        }
//                        stringBuilder = stringBuilder.reverse().append(text.substring(start - PREFIX_CHARACTER_LENGTH));
//                        spannableString = toSpannableString(stringBuilder.toString(), keyword, textColor);
//                    } else {
//                        for (i = start - 1; i >= start - PREFIX_CHARACTER_LENGTH; i--) {
//                            if (measureText(textView, stringBuilder.toString() + chars[i]) <= maxWidth - keywordWidth - ellipsisWidth) {
//                                stringBuilder.append(chars[i]);
//                            } else {
//                                stringBuilder.append(ELLIPSIS);
//                                break;
//                            }
//                        }
//                        if (i < start - PREFIX_CHARACTER_LENGTH) {
//                            stringBuilder.append(ELLIPSIS);
//                        }
//
//                        stringBuilder = stringBuilder.reverse().append(keyword);
//
//                        for (int j = end; j < chars.length; j++) {
//                            if (measureText(textView, stringBuilder.toString() + chars[j]) <= maxWidth) {
//                                stringBuilder.append(chars[j]);
//                            } else {
//                                stringBuilder.append(ELLIPSIS);
//                                break;
//                            }
//                        }
//                        if (start > PREFIX_CHARACTER_LENGTH) {
//                            spannableString = toSpannableString(stringBuilder.toString(), keyword, textColor);
//                        } else {
//                            spannableString = toSpannableString(stringBuilder.toString(), keyword, textColor);
//                        }
//                    }
//                } else {
//                    char[] chars = text.toCharArray();
//                    int i;
//                    stringBuilder.append(text.substring(0, start));
//                    for (i = start; i < text.length(); i++) {
//                        if (measureText(textView, stringBuilder.toString() + chars[i]) < maxWidth - ellipsisWidth) {
//                            stringBuilder.append(chars[i]);
//                        } else {
//                            break;
//                        }
//                    }
//                    if (i < text.length()) {
//                        stringBuilder.append(ELLIPSIS);
//                    }
//                    spannableString = toSpannableString(stringBuilder.toString(), keyword, textColor);
//                }
//            }
//        }
//        return spannableString;
//    }
//
//    private static SpannableString toSpannableString(String text, String keyword, String color) {
//        SpannableString builder = new SpannableString(text);
//        int[] allIndex = StringUtil.getAllIndex(text, keyword);
//        for (int index : allIndex) {
//            builder.setSpan(new ForegroundColorSpan(Color.parseColor(color)), index, index + keyword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        }
//        return builder;
//    }
//
//    private static float measureText(TextView textView, String text) {
//        return textView.getPaint().measureText(text);
//    }

    public static SpannableStringBuilder highlightText(String[] keywords, CharSequence hitWord, TextView tv, float maxWidth, int color) {
        if (keywords == null || keywords.length == 0 || hitWord == null || tv == null || maxWidth <= 0) {
            if (hitWord == null) {
                return new SpannableStringBuilder();
            } else {
                return new SpannableStringBuilder(hitWord);
            }
        }
        String hitString = hitWord.toString();
        String firstKeyword = keywords[0];
        int[] startIndex = StringUtil.getAllIndex(hitString, firstKeyword);
        TextPaint paint = tv.getPaint();
        int start = 0;
        int end;
        float oneWordWidth = paint.measureText("中");
        int midWidthCount = (int) (maxWidth / 2 / oneWordWidth);
        if (startIndex[0] + firstKeyword.length() <= hitWord.length() - 3) {
            end = startIndex[0] + firstKeyword.length() + 3;
        } else {
            end = startIndex[0] + firstKeyword.length();
        }
        while (paint.measureText(hitString, start, end) > maxWidth - oneWordWidth) {
            if (end - startIndex[0] > midWidthCount) {
                end = startIndex[0] + midWidthCount;
            } else {
                ++start;
            }
        }
        if (start++ != 0) {
            hitWord = ELLIPSIS + hitString.substring(start);
            for (int i = 0; i < startIndex.length; ++i) {
                startIndex[i] = startIndex[i] - start + 1;
            }
        }
        hitWord = toSpannableString(startIndex, hitWord, firstKeyword, color);
        for (int i = 1; i < keywords.length; i++) {
            int[] allIndex = StringUtil.getAllIndex(hitWord.toString(), keywords[i]);
            toSpannableString(allIndex, hitWord, keywords[i], color);
        }
        return (SpannableStringBuilder) hitWord;
    }

    //相邻的span归并
    private static SpannableStringBuilder toSpannableString(int[] allIndex, CharSequence text, String keyword, int color) {
        SpannableStringBuilder spannable;
        if (text instanceof SpannableStringBuilder) {
            spannable = (SpannableStringBuilder) text;
        } else {
            spannable = new SpannableStringBuilder(text);
        }
        if (allIndex != null) {
            int lastIndex = 0;
            for (int i = 0; i < allIndex.length; i++) {
                int currentIndex = allIndex[i];
                int preIndex = -1;
                if (i - 1 >= 0) {
                    preIndex = allIndex[i - 1];
                }
                if (preIndex == -1 || preIndex + keyword.length() != currentIndex) {
                    if (currentIndex < 0) {
                        lastIndex = 0;
                    } else {
                        lastIndex = currentIndex;
                    }
                }

                if (i + 1 < allIndex.length && allIndex[i + 1] == currentIndex + keyword.length()) {
                    continue;
                }
                ForegroundColorSpan fColorSpan = new ForegroundColorSpan(color);
                spannable.setSpan(fColorSpan, lastIndex, currentIndex + keyword.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return spannable;
    }

}
