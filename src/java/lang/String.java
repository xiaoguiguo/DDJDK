package java.lang;

import java.io.Serializable;

public final class String implements Serializable, Comparable<String>, CharSequence {
    @Override
    public int length() {
        return 0;
    }

    @Override
    public char charAt(int index) {
        return 0;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return null;
    }

    @Override
    public int compareTo(String o) {
        return 0;
    }
}
