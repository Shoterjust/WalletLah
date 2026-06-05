package com.walletlah.email;

import com.walletlah.expense.Expense;

record EmailExpenseIngestResult(
        boolean duplicate,
        Expense expense,
        String message
) {

    static EmailExpenseIngestResult logged(Expense expense) {
        return new EmailExpenseIngestResult(false, expense, "Logged expense from email.");
    }

    static EmailExpenseIngestResult alreadyLogged() {
        return new EmailExpenseIngestResult(true, null, "That email was already logged.");
    }
}
