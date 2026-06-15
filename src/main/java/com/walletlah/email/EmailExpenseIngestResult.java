package com.walletlah.email;

import com.walletlah.receipt.PendingExpense;

record EmailExpenseIngestResult(
        boolean duplicate,
        PendingExpense pendingExpense,
        String message
) {

    static EmailExpenseIngestResult pending(PendingExpense pendingExpense) {
        return new EmailExpenseIngestResult(false, pendingExpense, "Sent transaction to Telegram for confirmation.");
    }

    static EmailExpenseIngestResult alreadyLogged() {
        return new EmailExpenseIngestResult(true, null, "That email was already received.");
    }
}
