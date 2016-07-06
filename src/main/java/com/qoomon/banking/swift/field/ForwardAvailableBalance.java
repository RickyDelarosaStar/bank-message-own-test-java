package com.qoomon.banking.swift.field;

import com.google.common.base.Preconditions;
import com.qoomon.banking.swift.field.notation.SwiftFieldNotation;
import com.qoomon.banking.swift.field.subfield.DebitCreditMark;
import org.joda.money.Money;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <b>Forward Available Balance</b>
 * <p>
 * <b>Field Tag</b> :65:
 * <p>
 * <b>Format</b> 1!a6!n3!a15d
 * <p>
 * <b>SubFields</b>
 * <pre>
 * 1: 1!a - Debit/Credit Mark - 'D' = Debit, 'C' Credit
 * 2: 6!n - Entry date - Format 'YYMMDD'
 * 3: 3!a - Currency - Three Digit Code
 * 3: 15d - Amount
 * </pre>
 */
public class ForwardAvailableBalance implements SwiftMTField {

    public static final String FIELD_TAG_65 = "65";

    public static final SwiftFieldNotation SWIFT_NOTATION = new SwiftFieldNotation("1!a6!n3!a15d");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private final DebitCreditMark debitCreditMark;
    private final LocalDate entryDate;
    private final Money amount;

    public ForwardAvailableBalance(DebitCreditMark debitCreditMark, LocalDate entryDate, Money amount) {
        this.debitCreditMark = Preconditions.checkNotNull(debitCreditMark);
        this.entryDate = Preconditions.checkNotNull(entryDate);
        this.amount = Preconditions.checkNotNull(amount);
    }

    public static ForwardAvailableBalance of(GeneralMTField field) throws ParseException {
        Preconditions.checkArgument(field.getTag().equals(FIELD_TAG_65), "unexpected field tag '" + field.getTag() + "'");

        List<String> subFields = SWIFT_NOTATION.parse(field.getContent());

        String amountCurrency = subFields.get(0);
        LocalDate entryDate = LocalDate.parse(subFields.get(1), DATE_FORMATTER);
        DebitCreditMark debitCreditMark = DebitCreditMark.of(subFields.get(2));
        String amountValue = subFields.get(3);

        Money amount = Money.parse(amountCurrency + amountValue.replaceFirst(",", "."));

        return new ForwardAvailableBalance(debitCreditMark, entryDate, amount);
    }

    public DebitCreditMark getDebitCreditMark() {
        return debitCreditMark;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public Money getAmount() {
        return amount;
    }

    @Override
    public String getTag() {
        return FIELD_TAG_65;
    }
}
