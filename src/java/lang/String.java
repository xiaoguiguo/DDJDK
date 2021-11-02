package java.lang;

import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.annotation.Stable;

import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.annotation.Native;

public final class String implements Serializable, Comparable<String>, CharSequence {

    private static final long serialVersionUID = -8522897979510336244L;

    @Native static final byte LATIN1 = 0;
    @Native static final byte UTF16  = 1;

    /**
     * The value is used for character storage.
     * 被@Stable修饰的字段，是被vm信任的
     * char[] replaced by byte[] to used for String, 目的是为了compact string，从而节省jvm的使用内存空间
     */
    @Stable
    private final byte[] value;

    /**
     * The identifier of the encoding used to encode the bytes in value.
     * 支持LATIN1/UTF16, LATIN1(ISO-8859-1)
     * 字符串压缩 Compact String(since java 9)
     * 无论何时我们创建一个所有字符都能用一个字节的 LATIN-1 编码来描述的字符串，都将在内部使用字节数组的形式存储，且每个字符都只占用一个字节。
     * 另一方面，如果字符串中任一字符需要多于 8 比特位来表示时，该字符串的所有字符都统统使用两个字节的 UTF-16 编码来描述。
     * 虚拟机参数 CompactStrings 默认是开启的，可通过：+XX:-CompactStrings 关闭
     */
    private final byte coder;

    private int hash;

    static final boolean COMPACT_STRINGS;
    static {
        COMPACT_STRINGS = true;
    }

    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];

    public String() {
        this.value = "".value;
        this.coder = "".coder;
    }

    @HotSpotIntrinsicCandidate
    public String(String original) {
        this.value = original.value;
        this.coder = original.coder;
        this.hash = original.hash;
    }

    public String(char value[]) {
        this(value, 0, value.length, null);
    }

    public String(char value[], int offset, int count) {
        this(value, offset, count, rangeCheck(value, offset, count));
    }

    String(byte[] value, byte coder) {
        this.value = value;
        this.coder = coder;
    }

    String(char[] value, int off, int len, Void sig) {
        if (len == 0) {
            this.value = "".value;
            this.coder = "".coder;
            return;
        }
        if (COMPACT_STRINGS) {
            byte[] val = StringUTF16.compress(value, off, len);
            if (val != null) {
                this.value = val;
                this.coder = LATIN1;
                return;
            }
        }
        this.coder = UTF16;
        this.value = StringUTF16.toBytes(value, off, len);
    }

    /**
     * 直接使用双引号声明出来的String对象会直接存储在常量池中。
     * 如果不是用双引号声明的String对象，可以使用String提供的intern方法。
     * intern 方法会从字符串常量池中查询当前字符串是否存在，若不存在就会将当前字符串放入常量池中
     */
    public native String intern();

    public int length() {
        return value.length >> coder();
    }

    /**
     * 获取指定索引处的字符
     */
    public char charAt(int index) {
        if (isLatin1()) {
            return StringLatin1.charAt(value, index);
        } else {
            return StringUTF16.charAt(value, index);
        }
    }

    public CharSequence subSequence(int beginIndex, int endIndex) {
        return this.substring(beginIndex, endIndex);
    }

    private CharSequence substring(int beginIndex, int endIndex) {
        int length = length();
        checkBoundsBeginEnd(beginIndex, endIndex, length);
        int subLen = endIndex - beginIndex;
        if (beginIndex == 0 && endIndex == length) {
            return this;
        }
        return isLatin1() ? StringLatin1.newString(value, beginIndex, subLen)
                          : StringUTF16.newString(value, beginIndex, subLen);
    }

    public int compareTo(String o) {
        return 0;
    }

    byte coder() {
        return COMPACT_STRINGS ? coder : UTF16;
    }

    private boolean isLatin1() {
        return COMPACT_STRINGS && coder == LATIN1;
    }

    static void checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new StringIndexOutOfBoundsException("index " + index + ",length " + length);
        }
    }

    private static Void rangeCheck(char[] value, int offset, int count) {
        checkBoundsOffCount(offset, count, value.length);
        return null;
    }

    static void checkBoundsOffCount(int offset, int count, int length) {
        if (offset < 0 || count < 0 || offset > length - count) {
            throw new StringIndexOutOfBoundsException(
                    "offset " + offset + ", count " + count + ", length " + length);
        }
    }

    static void checkBoundsBeginEnd(int begin, int end, int length) {
        if (begin < 0 || begin > end || end > length) {
            throw new StringIndexOutOfBoundsException(
                    "begin " + begin + ", end " + end + ", length " + length);
        }
    }

    public boolean equalsIgnoreCase(String anotherString) {
        return (this == anotherString) ? true
                : (anotherString != null)
                && (anotherString.length() == length())
                && regionMatches(true, 0, anotherString, 0, length());
    }

    public boolean regionMatches(int toffset, String other, int ooffset, int len) {
        byte tv[] = value;
        byte ov[] = other.value;
        if ((ooffset < 0) || (toffset < 0) || (toffset > (long) length() - len) || (ooffset > (long) other.length() - len)) {
            return false;
        }
        if (coder() == other.coder()) {
            if (!isLatin1() && (len > 0)) {
                toffset = toffset << 1;
                ooffset = ooffset << 1;
                len = len << 1;
            }
            while (len-- > 0) {
                if (tv[toffset++] != ov[ooffset++]) {
                    return false;
                }
            }
        } else {
            if (coder() == LATIN1) {
                while (len-- > 0) {
                    if (StringLatin1.getChar(tv, toffset++) !=
                            StringUTF16.getChar(ov, ooffset++)) {
                        return false;
                    }
                }
            } else {
                while (len-- > 0) {
                    if (StringUTF16.getChar(tv, toffset++) !=
                            StringLatin1.getChar(ov, ooffset++)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) {
        if (!ignoreCase) {
            return regionMatches(toffset, other, ooffset, len);
        }
        if ((ooffset < 0) || (toffset < 0) || (toffset > (long) length() -len) || (ooffset > (long) other.length() - len)) {
            return false;
        }
        byte tv[] = value;
        byte ov[] = other.value;
        if (coder() == other.coder()) {
            return isLatin1()
                    ? StringLatin1.regionMatchesCI(tv, toffset, ov, ooffset, len)
                    : StringUTF16.regisnMatchesCI(tv, toffset, ov, ooffset, len);
        }
        return isLatin1()
                ? StringLatin1.regionMatchesCI_UTF16(tv, toffset, ov, ooffset, len)
                : StringUTF16.regfionMatchesCI_Latin1(tv, toffset, ov, ooffset, len);
    }


}
