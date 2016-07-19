package com.qoomon.banking.swift.message.submessage.field;

import com.google.common.base.Preconditions;
import com.qoomon.banking.swift.notation.FieldNotationParseException;
import com.qoomon.banking.swift.notation.SwiftNotation;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <b>Date Time Indicator</b>
 * <p>
 * <b>Field Tag</b> :13D:
 * <p>
 * <b>Format</b> 6!n4!n1x4!n
 * <p>
 * <b>SubFields</b>
 * <pre>
 * 1: 6!n - Debit/Credit Mark - 'D' = Debit, 'C' Credit
 * 2: 4!n - Date - Format 'YYMMDD'
 * 3: 1x  - Offset sign - '+' or '-'
 * 4: 4!n - Offset - Format 'hhmm'
 * </pre>
 * <p>
 * <b>Example</b>
 * <pre>
 * 1605191047+0100
 * </pre>
 */
public class DateTimeIndicator implements SwiftField {

    public static final String FIELD_TAG_13D = "13D";

    public static final SwiftNotation SWIFT_NOTATION = new SwiftNotation("6!n4!n1x4!n");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmZ");

    private final OffsetDateTime value;


    public DateTimeIndicator(OffsetDateTime value) {

        Preconditions.checkArgument(value != null, "value can't be null");

        this.value = value;
    }

    public static DateTimeIndicator of(GeneralField field) throws FieldNotationParseException {
        Preconditions.checkArgument(field.getTag().equals(FIELD_TAG_13D), "unexpected field tag '%s'", field.getTag());

        List<String> subFields = SWIFT_NOTATION.parse(field.getContent());

        OffsetDateTime value = OffsetDateTime.parse(subFields.stream().collect(Collectors.joining()), DATE_TIME_FORMATTER);

        return new DateTimeIndicator(value);
    }

    public OffsetDateTime getValue() {
        return value;
    }

    @Override
    public String getTag() {
        return FIELD_TAG_13D;
    }


}
