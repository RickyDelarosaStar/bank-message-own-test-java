package com.qoomon.banking.swift.message.submessage.mt940;

import com.google.common.collect.ImmutableSet;
import com.qoomon.banking.swift.message.submessage.field.*;
import com.qoomon.banking.swift.message.submessage.field.TransactionGroup;
import com.qoomon.banking.swift.message.submessage.exception.SwiftMTParserException;
import com.qoomon.banking.swift.message.submessage.field.exception.SwiftMTFieldParseException;

import java.io.Reader;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Parser for {@link SwiftMT940}
 */
public class SwiftMT940Parser {

    private final SwiftMTFieldParser swiftMTParser = new SwiftMTFieldParser();


    public List<SwiftMT940> parse(Reader mt940TextReader) throws SwiftMTFieldParseException {

        List<SwiftMT940> result = new LinkedList<>();

        List<GeneralField> fieldList = swiftMTParser.parse(mt940TextReader);

        TransactionReferenceNumber transactionReferenceNumber = null;
        RelatedReference relatedReference = null;
        AccountIdentification accountIdentification = null;
        StatementNumber statementNumber = null;
        OpeningBalance openingBalance = null;
        List<TransactionGroup> transactionList = new LinkedList<>();
        ClosingBalance closingBalance = null;
        ClosingAvailableBalance closingAvailableBalance = null;
        List<ForwardAvailableBalance> forwardAvailableBalanceList = new LinkedList<>();
        InformationToAccountOwner informationToAccountOwner = null;

        int currentFieldNumber = 0;

        Set<String> currentValidFieldSet = ImmutableSet.of(TransactionReferenceNumber.FIELD_TAG_20);

        GeneralField previousField = null;
        for (GeneralField currentField : fieldList) {
            try {
            Set<String> nextValidFieldSet;

            currentFieldNumber++;

            switch (currentField.getTag()) {
                case TransactionReferenceNumber.FIELD_TAG_20: {
                    transactionReferenceNumber = TransactionReferenceNumber.of(currentField);
                    nextValidFieldSet = ImmutableSet.of(
                            RelatedReference.FIELD_TAG_21,
                            AccountIdentification.FIELD_TAG_25);
                    break;
                }
                case RelatedReference.FIELD_TAG_21: {
                    relatedReference = RelatedReference.of(currentField);
                    nextValidFieldSet = ImmutableSet.of(
                            AccountIdentification.FIELD_TAG_25);
                    break;
                }
                case AccountIdentification.FIELD_TAG_25: {
                    accountIdentification = AccountIdentification.of(currentField);
                    nextValidFieldSet = ImmutableSet.of(
                            StatementNumber.FIELD_TAG_28C);
                    break;
                }
                case StatementNumber.FIELD_TAG_28C: {
                    statementNumber = StatementNumber.of(currentField);
                    nextValidFieldSet = ImmutableSet.of(
                            OpeningBalance.FIELD_TAG_60F,
                            OpeningBalance.FIELD_TAG_60M);
                    break;
                }
                case OpeningBalance.FIELD_TAG_60F:
                case OpeningBalance.FIELD_TAG_60M: {
                    openingBalance = OpeningBalance.of(currentField);
                    nextValidFieldSet = ImmutableSet.of(
                            StatementLine.FIELD_TAG_61,
                            ClosingBalance.FIELD_TAG_62F,
                            ClosingBalance.FIELD_TAG_62M);
                    break;
                }
                case StatementLine.FIELD_TAG_61: {
                    StatementLine statementLine = StatementLine.of(currentField);
                    transactionList.add(new TransactionGroup(statementLine, null));
                    nextValidFieldSet = ImmutableSet.of(
                            InformationToAccountOwner.FIELD_TAG_86,
                            ClosingBalance.FIELD_TAG_62F,
                            ClosingBalance.FIELD_TAG_62M);
                    break;
                }
                case ClosingBalance.FIELD_TAG_62F:
                case ClosingBalance.FIELD_TAG_62M: {
                    closingBalance = ClosingBalance.of(currentField);
                    nextValidFieldSet = ImmutableSet.of(
                            ClosingAvailableBalance.FIELD_TAG_64,
                            ForwardAvailableBalance.FIELD_TAG_65,
                            InformationToAccountOwner.FIELD_TAG_86,
                            SwiftMTFieldParser.SEPARATOR_FIELD_TAG);
                    break;
                }
                case ClosingAvailableBalance.FIELD_TAG_64: {
                    closingAvailableBalance = ClosingAvailableBalance.of(currentField);
                    nextValidFieldSet = ImmutableSet.of(
                            ForwardAvailableBalance.FIELD_TAG_65,
                            InformationToAccountOwner.FIELD_TAG_86,
                            SwiftMTFieldParser.SEPARATOR_FIELD_TAG);
                    break;
                }
                case ForwardAvailableBalance.FIELD_TAG_65: {
                    ForwardAvailableBalance forwardAvailableBalance = ForwardAvailableBalance.of(currentField);
                    forwardAvailableBalanceList.add(forwardAvailableBalance);
                    nextValidFieldSet = ImmutableSet.of(
                            InformationToAccountOwner.FIELD_TAG_86,
                            SwiftMTFieldParser.SEPARATOR_FIELD_TAG);
                    break;
                }
                case InformationToAccountOwner.FIELD_TAG_86: {
                    if (previousField != null && previousField.getTag().equals(StatementLine.FIELD_TAG_61)) {
                        // amend transaction with transactionInformationToAccountOwner
                        int lastTransactionIndex = transactionList.size() - 1;
                        TransactionGroup lastTransaction = transactionList.get(lastTransactionIndex);
                        InformationToAccountOwner transactionInformationToAccountOwner = InformationToAccountOwner.of(currentField);

                        TransactionGroup updatedTransaction = new TransactionGroup(lastTransaction.getStatementLine(), transactionInformationToAccountOwner);
                        transactionList.set(lastTransactionIndex, updatedTransaction);

                        nextValidFieldSet = ImmutableSet.of(
                                StatementLine.FIELD_TAG_61,
                                ClosingBalance.FIELD_TAG_62F,
                                ClosingBalance.FIELD_TAG_62M);
                    } else {
                        informationToAccountOwner = InformationToAccountOwner.of(currentField);
                        nextValidFieldSet = ImmutableSet.of(SwiftMTFieldParser.SEPARATOR_FIELD_TAG);
                    }
                    break;
                }
                case SwiftMTFieldParser.SEPARATOR_FIELD_TAG: {
                    // see below at finish message
                    nextValidFieldSet = ImmutableSet.of(
                            TransactionReferenceNumber.FIELD_TAG_20);
                    break;
                }
                default:
                    throw new SwiftMTParserException("Parse error: unexpected field", currentFieldNumber, currentField.getTag());
            }

            if (!currentValidFieldSet.contains(currentField.getTag())) {
                if (previousField == null) {
                    throw new SwiftMTParserException("Field " + currentField.getTag() + " is not allowed as first field", currentFieldNumber, currentField.getTag());
                } else {
                    throw new SwiftMTParserException("Field " + currentField.getTag() + " is not allowed after field " + previousField.getTag(), currentFieldNumber, currentField.getTag());
                }
            }

            // handle finishing message
            if (fieldList.size() == currentFieldNumber // last field
                    || currentField.getTag().equals(SwiftMTFieldParser.SEPARATOR_FIELD_TAG)) {

                result.add(new SwiftMT940(
                        transactionReferenceNumber,
                        relatedReference,
                        accountIdentification,
                        statementNumber,
                        openingBalance,
                        transactionList,
                        closingBalance,
                        closingAvailableBalance,
                        forwardAvailableBalanceList,
                        informationToAccountOwner
                ));

                transactionReferenceNumber = null;
                relatedReference = null;
                accountIdentification = null;
                statementNumber = null;
                openingBalance = null;
                transactionList = new LinkedList<>();
                closingBalance = null;
                closingAvailableBalance = null;
                forwardAvailableBalanceList = new LinkedList<>();
                informationToAccountOwner = null;
            }

            // prepare for next iteration
            previousField = currentField;
            currentValidFieldSet = nextValidFieldSet;

            } catch (ParseException parseException) {
                throw new SwiftMTParserException("Subfield parse error", currentFieldNumber, currentField.getTag(), parseException);
            }
        }

        return result;

    }
}
