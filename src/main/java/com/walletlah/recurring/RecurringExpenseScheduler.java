package com.walletlah.recurring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringExpenseScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringExpenseScheduler.class);

    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseScheduler(RecurringExpenseService recurringExpenseService) {
        this.recurringExpenseService = recurringExpenseService;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Singapore")
    public void generateDueRecurringExpenses() {
        int generated = recurringExpenseService.generateDueExpenses();
        if (generated > 0) {
            log.info("Generated {} recurring expenses", generated);
        }
    }
}
