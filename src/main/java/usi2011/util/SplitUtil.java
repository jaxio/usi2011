package usi2011.util;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.jboss.netty.handler.codec.http.HttpMessageDecoder;

/**
 * Extracted from {@link HttpMessageDecoder}
 */
public final class SplitUtil {
    private SplitUtil() {

    }

    public static String[] splitKeyValue(final String sb, final char separator) {
        final int length = sb.length();
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;

        nameStart = 0;
        for (nameEnd = nameStart; nameEnd < length; nameEnd++) {
            char ch = sb.charAt(nameEnd);
            if (ch == separator) {
                break;
            }
        }

        for (colonEnd = nameEnd; colonEnd < length; colonEnd++) {
            if (sb.charAt(colonEnd) == separator) {
                colonEnd++;
                break;
            }
        }

        valueStart = colonEnd;
        if (valueStart == length) {
            return new String[] { sb.substring(nameStart, nameEnd), "" };
        }

        return new String[] { sb.substring(nameStart, nameEnd), sb.substring(valueStart, length) };
    }

    public static String[] split(final String sb, final char separator) {
        final List<String> ret = newArrayList();
        final int length = sb.length();
        int lastStart = 0;
        for (int i = 0; i < length; i++) {
            if (sb.charAt(i) == separator) {
                if (i != 0) {
                    ret.add(sb.substring(lastStart, i));
                }
                lastStart = i + 1;
            }
        }
        if (lastStart != length) {
            ret.add(sb.substring(lastStart, length));
        }
        return ret.toArray(new String[0]);
    }
    
    public static String[] split(final String sb, final char separator, int expectedResultSize) {
        String[] result = new String[expectedResultSize];
        int index = 0;

        final int length = sb.length();
        int lastStart = 0;
        for (int i = 0; i < length; i++) {
            if (sb.charAt(i) == separator) {
                if (i != 0) {
                    result[index++] = sb.substring(lastStart, i);
                }
                lastStart = i + 1;
            }
        }
        if (lastStart != length) {
            result[index++] = sb.substring(lastStart, length);
        }
        return result;
    }
    
}
