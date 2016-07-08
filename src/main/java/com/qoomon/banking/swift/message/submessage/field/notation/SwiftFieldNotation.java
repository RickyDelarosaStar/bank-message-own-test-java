package com.qoomon.banking.swift.message.submessage.field.notation;

import com.google.common.base.Preconditions;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

/**
 * <pre>
 * a = alphabetic, A through Z, upper case only
 * n = numeric digits, 0 through 9 only
 * c = alpha-numeric capital letters and numeric digits only ( a & n above )
 * d = decimals, including decimal comma ',' preceding the fractional part. The fractional part may be missing, but the decimal comman must always be present
 * e = space
 * s = sign ( + or _ )
 * h = hex ( 0 to 9, A to F)
 * x = SWIFT X character set : SPACE, A to Z, a to z, 0 to 9, and  + - / ? . : , ( ) '                   and CrLF
 * y = SWIFT Y character set : SPACE, A to Z, a to z, 0 to 9, and  + - / ? . : , ( ) ' = ! " % & * < > ;
 * z = SWIFT Z character set : SPACE, A to Z, a to z, 0 to 9, and  + - / ? . : , ( ) ' = ! " % & * < > ; and CrLf
 * A = alphabetic, A through Z, upper and lower case
 * B = alphanumeric upper case or lower case, and numeric digits
 *
 * length specification:
 * nn = maximum length ( minimum is 1 )
 * nn-nn = minimum and maximum length
 * nn! = fixed length
 * nn*nn = maximum number of lines time maximum line length - Will always be the last field
 *
 * separators
 * LSep = left separator ("/", "//", "BR" for CrLf, "ISIN ", etc.), field starts with the character specified
 * RSep = right separator ("/", "//", "BR" for CrLf, "ISIN ", etc.), field ends with the character specified
 *
 * examples,
 * 6!n = 6 numeric, fixed length
 * 6n = numeric up to 6 characters
 * 1!e = one blank space
 * 6*50x = up to 6 lines of up to 50 characters
 * </pre>
 */
public class SwiftFieldNotation {

    private static final String SEPARATOR_SET = "(?:/|//|BR|ISIN)";

    private static final Map<String, String> CHARSET_REGEX_MAP = new HashMap<String, String>() {{
        // see class description for charset details
        put("a", "[A-Z]");
        put("n", "[0-9]");
        put("c", "[0-9A-Z]");
        put("d", "[0-9,]");
        put("e", " ");
        put("s", "[+_]");
        put("h", "[0-9A-F]");
        put("x", "[ 0-9A-Za-z+-/?.:,()'\\n]");
        put("y", "[ 0-9A-Za-z+-/?.:,()'=!\"%&*<>;]");
        put("z", "[ 0-9A-Za-z+-/?.:,()'=!\"%&*<>;\\n]");
        put("A", "[A-Za-z]");
        put("B", "[0-9A-Za-z]");
    }};


    private final String notation;
    private final List<SubField> swiftSubFields;


    public SwiftFieldNotation(String notation) {

        this.swiftSubFields = parseSwiftNotation(notation);
        this.notation = notation;
    }


    /**
     * Parse sub fields
     *
     * @param fieldText Text to parse
     * @return List of subfield values. Missing optional fields are represented as NULL
     * @throws ParseException
     */
    public List<String> parse(String fieldText) throws ParseException {

        int parseIndex = 0;

        List<String> result = new LinkedList<>();

        for (SubField subfield : swiftSubFields) {
            String subfieldRegex = buildSubfieldRegex(subfield);
            Pattern subfieldPattern = Pattern.compile("^" + subfieldRegex);
            Matcher subfieldMatcher = subfieldPattern.matcher(fieldText).region(parseIndex, fieldText.length());
            if (!subfieldMatcher.find()) {
                throw new ParseException(subfield + " did not found matching characters."
                        + " near index " + parseIndex + " '" + fieldText.substring(parseIndex) + "'", parseIndex);
            }
            parseIndex = subfieldMatcher.end();

            String fieldValue = subfieldMatcher.group();

            // special handling for d charset due to only on comma constraint
            if (subfield.getCharSet().equals("d")) {
                Matcher decimalCharsetMatcher = Pattern.compile("[^,]+,[^,]*").matcher(fieldValue);
                if (!decimalCharsetMatcher.matches()) {
                    throw new ParseException(subfield + " did not found matching characters."
                            + " near index " + parseIndex + " '" + fieldText.substring(parseIndex) + "'", parseIndex);
                }
            }

            //remove prefix
            if (subfield.getPrefix().isPresent()) {
                fieldValue = fieldValue.replaceFirst(quote(subfield.getPrefix().get()), "");
            }

            // add field value
            if (!fieldValue.isEmpty()) {
                result.add(fieldValue);
            } else {
                result.add(null);
            }
        }

        if (parseIndex != fieldText.length()) {
            throw new ParseException("Unparsed characters remain."
                    + " near index " + parseIndex + " '" + fieldText.substring(parseIndex) + "'", parseIndex);
        }

        return result;
    }

    private static String buildSubfieldRegex(SubField subfield) {
        String charSetRegex = CHARSET_REGEX_MAP.get(subfield.getCharSet());
        if (charSetRegex == null) {
            throw new IllegalArgumentException("Unknown charset: " + charSetRegex);
        }

        String subFieldRegex = "";
        if (!subfield.getLengthSign().isPresent()) {
            int maxCharacters = subfield.getLength0();
            subFieldRegex += charSetRegex + "{1," + maxCharacters + "}";
        } else {
            String lengthSign = subfield.getLengthSign().get();
            switch (lengthSign) {
                case "!": {
                    int fixedCharacters = subfield.getLength0();
                    subFieldRegex += charSetRegex + "{" + fixedCharacters + "}";
                    break;
                }
                case "-": {
                    int minCharacters = subfield.getLength0();
                    int maxCharacters = subfield.getLength1().get();
                    subFieldRegex += charSetRegex + "{" + minCharacters + "," + maxCharacters + "}";
                    break;
                }
                case "*": {
                    int maxLines = subfield.getLength0();
                    int maxLineCharacters = subfield.getLength1().get();
                    String lineCharactersRegexRange = "{1," + maxLineCharacters + "}";
                    String lineRegex = "[^\n]" + lineCharactersRegexRange;
                    subFieldRegex = "(?=" + lineRegex + "(\n" + lineRegex + ")" + "{0," + (maxLines - 1) + "}" + "$)" // lookahead for maxLines
                            + "(?:" + charSetRegex + "|\n)"  // add new line character to charset
                            + "{1," + (maxLines * maxLineCharacters + (maxLines - 1)) + "}$";  // calculate max length including newline signs
                    break;
                }
                default:
                    throw new RuntimeException("Unsupported length sign '" + lengthSign + "'");
            }
        }


        if (subfield.getPrefix().isPresent()) {
            subFieldRegex = quote(subfield.getPrefix().get()) + subFieldRegex;
        }

        if (subfield.isOptional()) {
            subFieldRegex = "(?:" + subFieldRegex + ")?";
        }

        return subFieldRegex;
    }

    public String groupRegex(String groupName, String regex) {
        return "(?<" + groupName + ">" + regex + ")";
    }

    public String groupRegex(String groupName, Pattern pattern) {
        return groupRegex(groupName, pattern.toString());
    }

    public List<SubField> parseSwiftNotation(String swiftNotation) {
        List<SubField> result = new LinkedList<>();

        // Group 1: Field Prefix
        // Group 2: Field length0
        // Group 3: Field length sign ! - *
        // Group 4: Field length1
        // Group 5: Field charset
        Pattern fieldValueNotationPattern = Pattern.compile("(" + SEPARATOR_SET + ")?([0-9]{1,2})([!-*])?([0-9]{1,2})?([acdehnsxyzAB])");
        Pattern fieldNotationPattern = Pattern.compile(quote("[") + fieldValueNotationPattern + quote("]") + "|" + fieldValueNotationPattern);
        Matcher fieldNotationMatcher = fieldNotationPattern.matcher(swiftNotation);
        int parseIndex = 0;
        while (fieldNotationMatcher.find(parseIndex)) {
            if (fieldNotationMatcher.start() != parseIndex) {
                throw new RuntimeException("Parse error: Unexpected sign(s) near index " + parseIndex + " '" + swiftNotation + "'");
            }
            parseIndex = fieldNotationMatcher.end();

            String subfieldNotation = fieldNotationMatcher.group();
            // trim optional indicator
            String trimmedSubfieldNotation = subfieldNotation.replaceFirst("^" + quote("[") + "(.*)" + quote("]") + "$", "$1");
            Matcher fieldPropertiesMatcher = fieldValueNotationPattern.matcher(trimmedSubfieldNotation);
            if (!fieldPropertiesMatcher.matches()) {
                throw new RuntimeException("Parse error: Unexpected sign(s) near index " + parseIndex + " '" + swiftNotation + "'");
            }

            boolean fieldOptional = subfieldNotation.startsWith("[");
            String fieldPrefix = fieldPropertiesMatcher.group(1);
            Integer fieldLength0 = Integer.parseInt(fieldPropertiesMatcher.group(2));
            Integer fieldLength1 = fieldPropertiesMatcher.group(4) == null ? null : Integer.parseInt(fieldPropertiesMatcher.group(4));
            String fieldLengthSign = fieldPropertiesMatcher.group(3);
            String fieldCharset = fieldPropertiesMatcher.group(5);

            SubField subField = new SubField(
                    fieldOptional,
                    fieldPrefix,
                    fieldCharset,
                    fieldLength0,
                    fieldLength1,
                    fieldLengthSign);

            // add field
            result.add(subField);
        }
        if (parseIndex != swiftNotation.length()) {
            throw new RuntimeException("Parse error: Unexpected sign(s) near index " + parseIndex + " '" + swiftNotation + "'");
        }
        return result;
    }

    public String getNotation() {
        return notation;
    }

    public class SubField {
        private final Boolean optional;
        private final Optional<String> prefix;
        private final String charSet;
        private final Integer length0;
        private final Optional<Integer> length1;
        private final Optional<String> lengthSign;

        public SubField(Boolean optional, String prefix, String charSet, Integer length0, Integer length1, String lengthSign) {
            this.optional = Preconditions.checkNotNull(optional);
            this.prefix = Optional.ofNullable(prefix);
            this.charSet = Preconditions.checkNotNull(charSet);
            this.length0 = Preconditions.checkNotNull(length0);
            this.length1 = Optional.ofNullable(length1);
            this.lengthSign = Optional.ofNullable(lengthSign);

            if (!this.lengthSign.isPresent()) {
                Preconditions.checkArgument(!this.length1.isPresent(), "Missing field length sign between field lengths : '" + this.toString() + "'");
            } else switch (this.lengthSign.get()) {
                case "!":
                    Preconditions.checkArgument(!this.length1.isPresent(), "Unexpected field length after fixed length sign '!' : '" + this.toString() + "'");
                    break;
                case "-":
                    Preconditions.checkArgument(this.length1.isPresent(), "Missing field length after range length sign '-' : '" + this.toString() + "'");
                    break;
                case "*":
                    Preconditions.checkArgument(this.length1.isPresent(), "Missing field length after multiline length sign '*' : '" + this.toString() + "'");
                    break;
                default:
                    Preconditions.checkArgument(false, "Unknown length sign : '" + this.toString() + "'");
            }
        }

        public Boolean isOptional() {
            return optional;
        }

        public Integer getLength0() {
            return length0;
        }

        public Optional<Integer> getLength1() {
            return length1;
        }

        public Optional<String> getLengthSign() {
            return lengthSign;
        }

        public String getCharSet() {
            return charSet;
        }

        public Optional<String> getPrefix() {
            return prefix;
        }

        @Override
        public String toString() {
            String fieldNotation = "";

            fieldNotation += length0;
            if (lengthSign.isPresent()) {
                fieldNotation += lengthSign;
                if (lengthSign.equals("-") || lengthSign.equals("*")) {
                    fieldNotation += length1;
                }
            }
            fieldNotation += charSet;
            if (optional) {
                fieldNotation = "[" + prefix + fieldNotation + "]";
            }
            return fieldNotation;
        }
    }


}
