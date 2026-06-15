package com.walletlah.email;

interface BankEmailParser {

    boolean supports(EmailExpenseIngestRequest request);

    ParsedEmailExpense parse(EmailExpenseIngestRequest request);
}
