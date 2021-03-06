/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.util.regex;

import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern.CharPredicate;
import java.util.regex.Pattern.BmpCharPredicate;

class CharPredicates {

    static final CharPredicate ALPHABETIC() {
        return Character::isAlphabetic;
    }

    // \p{gc=Decimal_Number}
    static final CharPredicate DIGIT() {
        return Character::isDigit;
    }

    static final CharPredicate LETTER() {
        return Character::isLetter;
    }

    static final CharPredicate IDEOGRAPHIC() {
        return Character::isIdeographic;
    }

    static final CharPredicate LOWERCASE() {
        return Character::isLowerCase;
    }

    static final CharPredicate UPPERCASE() {
        return Character::isUpperCase;
    }

    static final CharPredicate TITLECASE() {
        return Character::isTitleCase;
    }

    // \p{Whitespace}
    static final CharPredicate WHITE_SPACE() {
        return ch ->
            ((((1 << Character.SPACE_SEPARATOR) |
               (1 << Character.LINE_SEPARATOR) |
               (1 << Character.PARAGRAPH_SEPARATOR)) >> Character.getType(ch)) & 1)
            != 0 || (ch >= 0x9 && ch <= 0xd) || (ch == 0x85);
    }

    // \p{gc=Control}
    static final CharPredicate CONTROL() {
        return ch -> Character.getType(ch) == Character.CONTROL;
    }

    // \p{gc=Punctuation}
    static final CharPredicate PUNCTUATION() {
        return ch ->
            ((((1 << Character.CONNECTOR_PUNCTUATION) |
               (1 << Character.DASH_PUNCTUATION) |
               (1 << Character.START_PUNCTUATION) |
               (1 << Character.END_PUNCTUATION) |
               (1 << Character.OTHER_PUNCTUATION) |
               (1 << Character.INITIAL_QUOTE_PUNCTUATION) |
               (1 << Character.FINAL_QUOTE_PUNCTUATION)) >> Character.getType(ch)) & 1)
            != 0;
    }

    // \p{gc=Decimal_Number}
    // \p{Hex_Digit}    -> PropList.txt: Hex_Digit
    static final CharPredicate HEX_DIGIT() {
        return DIGIT().union(ch -> (ch >= 0x0030 && ch <= 0x0039) ||
                (ch >= 0x0041 && ch <= 0x0046) ||
                (ch >= 0x0061 && ch <= 0x0066) ||
                (ch >= 0xFF10 && ch <= 0xFF19) ||
                (ch >= 0xFF21 && ch <= 0xFF26) ||
                (ch >= 0xFF41 && ch <= 0xFF46));
    }

    static final CharPredicate ASSIGNED() {
        return ch -> Character.getType(ch) != Character.UNASSIGNED;
    }

    // PropList.txt:Noncharacter_Code_Point
    static final CharPredicate NONCHARACTER_CODE_POINT() {
        return ch -> (ch & 0xfffe) == 0xfffe || (ch >= 0xfdd0 && ch <= 0xfdef);
    }

    // \p{alpha}
    // \p{digit}
    static final CharPredicate ALNUM() {
        return ALPHABETIC().union(DIGIT());
    }

    // \p{Whitespace} --
    // [\N{LF} \N{VT} \N{FF} \N{CR} \N{NEL}  -> 0xa, 0xb, 0xc, 0xd, 0x85
    //  \p{gc=Line_Separator}
    //  \p{gc=Paragraph_Separator}]
    static final CharPredicate BLANK() {
        return ch ->
            Character.getType(ch) == Character.SPACE_SEPARATOR ||
            ch == 0x9; // \N{HT}
    }

    // [^
    //  \p{space}
    //  \p{gc=Control}
    //  \p{gc=Surrogate}
    //  \p{gc=Unassigned}]
    static final CharPredicate GRAPH() {
        return ch ->
            ((((1 << Character.SPACE_SEPARATOR) |
               (1 << Character.LINE_SEPARATOR) |
               (1 << Character.PARAGRAPH_SEPARATOR) |
               (1 << Character.CONTROL) |
               (1 << Character.SURROGATE) |
               (1 << Character.UNASSIGNED)) >> Character.getType(ch)) & 1)
            == 0;
    }

    // \p{graph}
    // \p{blank}
    // -- \p{cntrl}
    static final CharPredicate PRINT() {
        return GRAPH().union(BLANK()).and(CONTROL().negate());
    }

    //  200C..200D    PropList.txt:Join_Control
    static final CharPredicate JOIN_CONTROL() {
        return ch -> ch == 0x200C || ch == 0x200D;
    }

    //  \p{alpha}
    //  \p{gc=Mark}
    //  \p{digit}
    //  \p{gc=Connector_Punctuation}
    //  \p{Join_Control}    200C..200D
    static final CharPredicate WORD() {
        return ALPHABETIC().union(ch -> ((((1 << Character.NON_SPACING_MARK) |
                                  (1 << Character.ENCLOSING_MARK) |
                                  (1 << Character.COMBINING_SPACING_MARK) |
                                  (1 << Character.DECIMAL_DIGIT_NUMBER) |
                                  (1 << Character.CONNECTOR_PUNCTUATION))
                                 >> Character.getType(ch)) & 1) != 0,
                         JOIN_CONTROL());
    }

    /////////////////////////////////////////////////////////////////////////////

    private static CharPredicate getPosixPredicate(String name) {
        switch (name) {
            case "ALPHA": return ALPHABETIC();
            case "LOWER": return LOWERCASE();
            case "UPPER": return UPPERCASE();
            case "SPACE": return WHITE_SPACE();
            case "PUNCT": return PUNCTUATION();
            case "XDIGIT": return HEX_DIGIT();
            case "ALNUM": return ALNUM();
            case "CNTRL": return CONTROL();
            case "DIGIT": return DIGIT();
            case "BLANK": return BLANK();
            case "GRAPH": return GRAPH();
            case "PRINT": return PRINT();
            default: return null;
        }
    }

    private static CharPredicate getUnicodePredicate(String name) {
        switch (name) {
            case "ALPHABETIC": return ALPHABETIC();
            case "ASSIGNED": return ASSIGNED();
            case "CONTROL": return CONTROL();
            case "HEXDIGIT": return HEX_DIGIT();
            case "IDEOGRAPHIC": return IDEOGRAPHIC();
            case "JOINCONTROL": return JOIN_CONTROL();
            case "LETTER": return LETTER();
            case "LOWERCASE": return LOWERCASE();
            case "NONCHARACTERCODEPOINT": return NONCHARACTER_CODE_POINT();
            case "TITLECASE": return TITLECASE();
            case "PUNCTUATION": return PUNCTUATION();
            case "UPPERCASE": return UPPERCASE();
            case "WHITESPACE": return WHITE_SPACE();
            case "WORD": return WORD();
            case "WHITE_SPACE": return WHITE_SPACE();
            case "HEX_DIGIT": return HEX_DIGIT();
            case "NONCHARACTER_CODE_POINT": return NONCHARACTER_CODE_POINT();
            case "JOIN_CONTROL": return JOIN_CONTROL();
            default: return null;
        }
    }

    public static CharPredicate forUnicodeProperty(String propName) {
        propName = propName.toUpperCase(Locale.ROOT);
        CharPredicate p = getUnicodePredicate(propName);
        if (p != null)
            return p;
        return getPosixPredicate(propName);
    }

    public static CharPredicate forPOSIXName(String propName) {
        return getPosixPredicate(propName.toUpperCase(Locale.ENGLISH));
    }

    /////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a predicate matching all characters belong to a named
     * UnicodeScript.
     */
    static CharPredicate forUnicodeScript(String name) {
        final Character.UnicodeScript script;
        try {
            script = Character.UnicodeScript.forName(name);
            return ch -> script == Character.UnicodeScript.of(ch);
        } catch (IllegalArgumentException iae) {}
        return null;
    }

    /**
     * Returns a predicate matching all characters in a UnicodeBlock.
     */
    static CharPredicate forUnicodeBlock(String name) {
        final Character.UnicodeBlock block;
        try {
            block = Character.UnicodeBlock.forName(name);
            return ch -> block == Character.UnicodeBlock.of(ch);
        } catch (IllegalArgumentException iae) {}
         return null;
    }

    /////////////////////////////////////////////////////////////////////////////

    // unicode categories, aliases, properties, java methods ...

    static CharPredicate forProperty(String name) {
        // Unicode character property aliases, defined in
        // http://www.unicode.org/Public/UNIDATA/PropertyValueAliases.txt
        switch (name) {
            case "Cn": return category(1<<Character.UNASSIGNED);
            case "Lu": return category(1<<Character.UPPERCASE_LETTER);
            case "Ll": return category(1<<Character.LOWERCASE_LETTER);
            case "Lt": return category(1<<Character.TITLECASE_LETTER);
            case "Lm": return category(1<<Character.MODIFIER_LETTER);
            case "Lo": return category(1<<Character.OTHER_LETTER);
            case "Mn": return category(1<<Character.NON_SPACING_MARK);
            case "Me": return category(1<<Character.ENCLOSING_MARK);
            case "Mc": return category(1<<Character.COMBINING_SPACING_MARK);
            case "Nd": return category(1<<Character.DECIMAL_DIGIT_NUMBER);
            case "Nl": return category(1<<Character.LETTER_NUMBER);
            case "No": return category(1<<Character.OTHER_NUMBER);
            case "Zs": return category(1<<Character.SPACE_SEPARATOR);
            case "Zl": return category(1<<Character.LINE_SEPARATOR);
            case "Zp": return category(1<<Character.PARAGRAPH_SEPARATOR);
            case "Cc": return category(1<<Character.CONTROL);
            case "Cf": return category(1<<Character.FORMAT);
            case "Co": return category(1<<Character.PRIVATE_USE);
            case "Cs": return category(1<<Character.SURROGATE);
            case "Pd": return category(1<<Character.DASH_PUNCTUATION);
            case "Ps": return category(1<<Character.START_PUNCTUATION);
            case "Pe": return category(1<<Character.END_PUNCTUATION);
            case "Pc": return category(1<<Character.CONNECTOR_PUNCTUATION);
            case "Po": return category(1<<Character.OTHER_PUNCTUATION);
            case "Sm": return category(1<<Character.MATH_SYMBOL);
            case "Sc": return category(1<<Character.CURRENCY_SYMBOL);
            case "Sk": return category(1<<Character.MODIFIER_SYMBOL);
            case "So": return category(1<<Character.OTHER_SYMBOL);
            case "Pi": return category(1<<Character.INITIAL_QUOTE_PUNCTUATION);
            case "Pf": return category(1<<Character.FINAL_QUOTE_PUNCTUATION);
            case "L": return category(((1<<Character.UPPERCASE_LETTER) |
                              (1<<Character.LOWERCASE_LETTER) |
                              (1<<Character.TITLECASE_LETTER) |
                              (1<<Character.MODIFIER_LETTER)  |
                              (1<<Character.OTHER_LETTER)));
            case "M": return category(((1<<Character.NON_SPACING_MARK) |
                              (1<<Character.ENCLOSING_MARK)   |
                              (1<<Character.COMBINING_SPACING_MARK)));
            case "N": return category(((1<<Character.DECIMAL_DIGIT_NUMBER) |
                              (1<<Character.LETTER_NUMBER)        |
                              (1<<Character.OTHER_NUMBER)));
            case "Z": return category(((1<<Character.SPACE_SEPARATOR) |
                              (1<<Character.LINE_SEPARATOR)  |
                              (1<<Character.PARAGRAPH_SEPARATOR)));
            case "C": return category(((1<<Character.CONTROL)     |
                              (1<<Character.FORMAT)      |
                              (1<<Character.PRIVATE_USE) |
                              (1<<Character.SURROGATE)   |
                              (1<<Character.UNASSIGNED))); // Other
            case "P": return category(((1<<Character.DASH_PUNCTUATION)      |
                              (1<<Character.START_PUNCTUATION)     |
                              (1<<Character.END_PUNCTUATION)       |
                              (1<<Character.CONNECTOR_PUNCTUATION) |
                              (1<<Character.OTHER_PUNCTUATION)     |
                              (1<<Character.INITIAL_QUOTE_PUNCTUATION) |
                              (1<<Character.FINAL_QUOTE_PUNCTUATION)));
            case "S": return category(((1<<Character.MATH_SYMBOL)     |
                              (1<<Character.CURRENCY_SYMBOL) |
                              (1<<Character.MODIFIER_SYMBOL) |
                              (1<<Character.OTHER_SYMBOL)));
            case "LC": return category(((1<<Character.UPPERCASE_LETTER) |
                               (1<<Character.LOWERCASE_LETTER) |
                               (1<<Character.TITLECASE_LETTER)));
            case "LD": return category(((1<<Character.UPPERCASE_LETTER) |
                               (1<<Character.LOWERCASE_LETTER) |
                               (1<<Character.TITLECASE_LETTER) |
                               (1<<Character.MODIFIER_LETTER)  |
                               (1<<Character.OTHER_LETTER)     |
                               (1<<Character.DECIMAL_DIGIT_NUMBER)));
            case "L1": return range(0x00, 0xFF); // Latin-1
            case "all": return Pattern.ALL();
            // Posix regular expression character classes, defined in
            // http://www.unix.org/onlinepubs/009695399/basedefs/xbd_chap09.html
            case "ASCII": return range(0x00, 0x7F);   // ASCII
            case "Alnum": return ctype(ASCII.ALNUM);  // Alphanumeric characters
            case "Alpha": return ctype(ASCII.ALPHA);  // Alphabetic characters
            case "Blank": return ctype(ASCII.BLANK);  // Space and tab characters
            case "Cntrl": return ctype(ASCII.CNTRL);  // Control characters
            case "Digit": return range('0', '9');     // Numeric characters
            case "Graph": return ctype(ASCII.GRAPH);  // printable and visible
            case "Lower": return range('a', 'z');     // Lower-case alphabetic
            case "Print": return range(0x20, 0x7E);   // Printable characters
            case "Punct": return ctype(ASCII.PUNCT);  // Punctuation characters
            case "Space": return ctype(ASCII.SPACE);  // Space characters
            case "Upper": return range('A', 'Z');     // Upper-case alphabetic
            case "XDigit": return ctype(ASCII.XDIGIT); // hexadecimal digits

            // Java character properties, defined by methods in Character.java
            case "javaLowerCase": return Character::isLowerCase;
            case "javaUpperCase": return  Character::isUpperCase;
            case "javaAlphabetic": return Character::isAlphabetic;
            case "javaIdeographic": return Character::isIdeographic;
            case "javaTitleCase": return Character::isTitleCase;
            case "javaDigit": return Character::isDigit;
            case "javaDefined": return Character::isDefined;
            case "javaLetter": return Character::isLetter;
            case "javaLetterOrDigit": return Character::isLetterOrDigit;
            case "javaJavaIdentifierStart": return Character::isJavaIdentifierStart;
            case "javaJavaIdentifierPart": return Character::isJavaIdentifierPart;
            case "javaUnicodeIdentifierStart": return Character::isUnicodeIdentifierStart;
            case "javaUnicodeIdentifierPart": return Character::isUnicodeIdentifierPart;
            case "javaIdentifierIgnorable": return Character::isIdentifierIgnorable;
            case "javaSpaceChar": return Character::isSpaceChar;
            case "javaWhitespace": return Character::isWhitespace;
            case "javaISOControl": return Character::isISOControl;
            case "javaMirrored": return Character::isMirrored;
            default: return null;
        }
    }

    private static CharPredicate category(final int typeMask) {
        return ch -> (typeMask & (1 << Character.getType(ch))) != 0;
    }

    private static CharPredicate range(final int lower, final int upper) {
        return (BmpCharPredicate)ch -> lower <= ch && ch <= upper;
    }

    private static CharPredicate ctype(final int ctype) {
        return (BmpCharPredicate)ch -> ch < 128 && ASCII.isType(ch, ctype);
    }

    /////////////////////////////////////////////////////////////////////////////

    /**
     * Posix ASCII variants, not in the lookup map
     */
    static final BmpCharPredicate ASCII_DIGIT() {
        return ch -> ch < 128 && ASCII.isDigit(ch);
    }
    static final BmpCharPredicate ASCII_WORD() {
        return ch -> ch < 128 && ASCII.isWord(ch);
    }
    static final BmpCharPredicate ASCII_SPACE() {
        return ch -> ch < 128 && ASCII.isSpace(ch);
    }

}
