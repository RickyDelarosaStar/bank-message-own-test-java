package com.qoomon.banking.swift.submessage.mt942;

import com.google.common.base.Preconditions;
import com.qoomon.banking.swift.submessage.field.*;
import com.qoomon.banking.swift.submessage.field.subfield.DebitCreditMark;
import org.joda.money.CurrencyUnit;

import java.util.List;
import java.util.Optional;

import static com.qoomon.banking.swift.submessage.field.FieldUtils.*;

/**
 *
 */
public class MT942Page {

    public static final String MESSAGE_ID_942 = "942";

    /**
     * @see TransactionReferenceNumber#FIELD_TAG_20
     */
    private final TransactionReferenceNumber transactionReferenceNumber;

    /**
     * @see RelatedReference#FIELD_TAG_21
     */
    private final Optional<RelatedReference> relatedReference;

    /**
     * @see AccountIdentification#FIELD_TAG_25
     */
    private final AccountIdentification accountIdentification;

    /**
     * @see StatementNumber#FIELD_TAG_28C
     */
    private final StatementNumber statementNumber;

    /**
     * @see FloorLimitIndicator#FIELD_TAG_34F
     */
    private final FloorLimitIndicator floorLimitIndicatorDebit;

    /**
     * @see FloorLimitIndicator#FIELD_TAG_34F
     */
    private final FloorLimitIndicator floorLimitIndicatorCredit;

    /**
     * @see DateTimeIndicator#FIELD_TAG_13D
     */
    private final DateTimeIndicator dateTimeIndicator;

    /**
     * @see StatementLine#FIELD_TAG_61
     * @see InformationToAccountOwner#FIELD_TAG_86
     */
    private final List<TransactionGroup> transactionGroupList;

    /**
     * @see TransactionSummary#FIELD_TAG_90D
     */
    private final Optional<TransactionSummary> transactionSummaryDebit;

    /**
     * @see TransactionSummary#FIELD_TAG_90C
     */
    private final Optional<TransactionSummary> transactionSummaryCredit;

    /**
     * @see InformationToAccountOwner#FIELD_TAG_86
     */
    private final Optional<InformationToAccountOwner> informationToAccountOwner;


    public MT942Page(
            TransactionReferenceNumber transactionReferenceNumber,
            RelatedReference relatedReference,
            AccountIdentification accountIdentification,
            StatementNumber statementNumber,
            FloorLimitIndicator floorLimitIndicatorDebit,
            FloorLimitIndicator floorLimitIndicatorCredit,
            DateTimeIndicator dateTimeIndicator,
            List<TransactionGroup> transactionGroupList,
            TransactionSummary transactionSummaryDebit,
            TransactionSummary transactionSummaryCredit,
            InformationToAccountOwner informationToAccountOwner) {

        Preconditions.checkArgument(transactionReferenceNumber != null, "transactionReferenceNumber can't be null");
        Preconditions.checkArgument(accountIdentification != null, "accountIdentification can't be null");
        Preconditions.checkArgument(statementNumber != null, "statementNumber can't be null");
        Preconditions.checkArgument(floorLimitIndicatorDebit != null, "floorLimitIndicatorDebit can't be null");
        if (floorLimitIndicatorDebit != null && floorLimitIndicatorDebit.getDebitCreditMark().isPresent()) {
            DebitCreditMark debitCreditMark = floorLimitIndicatorDebit.getDebitCreditMark().get();
            Preconditions.checkArgument(debitCreditMark == DebitCreditMark.DEBIT, "floorLimitIndicatorDebit type can't " + debitCreditMark);
        }
        if (floorLimitIndicatorCredit != null && floorLimitIndicatorCredit.getDebitCreditMark().isPresent()) {
            DebitCreditMark debitCreditMark = floorLimitIndicatorCredit.getDebitCreditMark().get();
            Preconditions.checkArgument(debitCreditMark == DebitCreditMark.CREDIT, "floorLimitIndicatorCredit type can't " + debitCreditMark);
        }
        Preconditions.checkArgument(dateTimeIndicator != null, "dateTimeIndicator can't be null");
        Preconditions.checkArgument(transactionGroupList != null, "transactionGroupList can't be null");


        CurrencyUnit statementCurrency = floorLimitIndicatorDebit.getAmount().getCurrencyUnit();
        String statementFundsCode = statementCurrency.getCode().substring(2, 3);
        // ensure matching currency
        Optional.ofNullable(floorLimitIndicatorCredit).map(it -> it.getAmount().getCurrencyUnit())
                .ifPresent(currency ->
                        Preconditions.checkArgument(currency.equals(statementCurrency), "floorLimitCreditCurrency '" + currency + "' does not match statement currency'" + statementCurrency + "'"));
        transactionGroupList.stream().map(it -> it.getStatementLine().getFundsCode()).filter(Optional::isPresent).map(Optional::get)
                .forEach(fundsCode ->
                        Preconditions.checkArgument(fundsCode.equals(statementFundsCode), "statementLineFundsCode '" + fundsCode + "' does not match statement currency'" + statementCurrency + "'"));
        Optional.ofNullable(transactionSummaryDebit).map(it -> it.getAmount().getCurrencyUnit())
                .ifPresent(currency ->
                        Preconditions.checkArgument(currency.equals(statementCurrency), "transactionSummaryDebitCurrency '" + currency + "' does not match statement currency'" + statementCurrency + "'"));
        Optional.ofNullable(transactionSummaryCredit).map(it -> it.getAmount().getCurrencyUnit())
                .ifPresent(currency ->
                        Preconditions.checkArgument(currency.equals(statementCurrency), "transactionSummaryCreditCurrency '" + currency + "' does not match statement currency'" + statementCurrency + "'"));


        this.transactionReferenceNumber = transactionReferenceNumber;
        this.relatedReference = Optional.ofNullable(relatedReference);
        this.accountIdentification = accountIdentification;
        this.statementNumber = statementNumber;
        this.floorLimitIndicatorDebit = floorLimitIndicatorDebit;
        this.floorLimitIndicatorCredit = Optional.ofNullable(floorLimitIndicatorCredit).orElse(floorLimitIndicatorDebit);
        this.dateTimeIndicator = dateTimeIndicator;
        this.transactionGroupList = transactionGroupList;
        this.transactionSummaryDebit = Optional.ofNullable(transactionSummaryDebit);
        this.transactionSummaryCredit = Optional.ofNullable(transactionSummaryCredit);
        this.informationToAccountOwner = Optional.ofNullable(informationToAccountOwner);
    }

    public TransactionReferenceNumber getTransactionReferenceNumber() {
        return transactionReferenceNumber;
    }

    public Optional<RelatedReference> getRelatedReference() {
        return relatedReference;
    }

    public AccountIdentification getAccountIdentification() {
        return accountIdentification;
    }

    public StatementNumber getStatementNumber() {
        return statementNumber;
    }

    public FloorLimitIndicator getFloorLimitIndicatorDebit() {
        return floorLimitIndicatorDebit;
    }

    public FloorLimitIndicator getFloorLimitIndicatorCredit() {
        return floorLimitIndicatorCredit;
    }

    public DateTimeIndicator getDateTimeIndicator() {
        return dateTimeIndicator;
    }

    public List<TransactionGroup> getTransactionGroupList() {
        return transactionGroupList;
    }

    public Optional<InformationToAccountOwner> getInformationToAccountOwner() {
        return informationToAccountOwner;
    }

    public Optional<TransactionSummary> getTransactionSummaryDebit() {
        return transactionSummaryDebit;
    }

    public Optional<TransactionSummary> getTransactionSummaryCredit() {
        return transactionSummaryCredit;
    }

    public String getContent() {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(swiftTextOf(transactionReferenceNumber)).append("\n");
        if (relatedReference.isPresent()) {
            contentBuilder.append(swiftTextOf(relatedReference.get())).append("\n");
        }
        contentBuilder.append(swiftTextOf(accountIdentification)).append("\n");
        contentBuilder.append(swiftTextOf(statementNumber)).append("\n");
        contentBuilder.append(swiftTextOf(floorLimitIndicatorDebit)).append("\n");
        if (!floorLimitIndicatorDebit.getAmount().equals(floorLimitIndicatorCredit.getAmount())) {
            contentBuilder.append(swiftTextOf(floorLimitIndicatorCredit)).append("\n");
        }
        contentBuilder.append(swiftTextOf(dateTimeIndicator)).append("\n");
        for (TransactionGroup transactionGroup : transactionGroupList) {
            contentBuilder.append(swiftTextOf(transactionGroup.getStatementLine())).append("\n");
            if (transactionGroup.getInformationToAccountOwner().isPresent()) {
                contentBuilder.append(swiftTextOf(transactionGroup.getInformationToAccountOwner().get())).append("\n");
            }
        }
        if (transactionSummaryDebit.isPresent()) {
            contentBuilder.append(swiftTextOf(transactionSummaryDebit.get())).append("\n");
        }
        if (transactionSummaryCredit.isPresent()) {
            contentBuilder.append(swiftTextOf(transactionSummaryCredit.get())).append("\n");
        }
        if (informationToAccountOwner.isPresent()) {
            contentBuilder.append(swiftTextOf(informationToAccountOwner.get())).append("\n");
        }
        contentBuilder.append("-");
        return contentBuilder.toString();
    }
}
