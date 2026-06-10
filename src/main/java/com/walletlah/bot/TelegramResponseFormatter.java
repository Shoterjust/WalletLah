package com.walletlah.bot;

import com.walletlah.analytics.CategoryBreakdownItem;
import com.walletlah.analytics.SpendingSummary;
import com.walletlah.common.MoneyUtils;
import com.walletlah.expense.Expense;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.expense.ExpenseSource;
import com.walletlah.expense.RecentExpenseView;
import com.walletlah.receipt.PendingExpense;
import com.walletlah.recurring.RecurringExpense;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TelegramResponseFormatter {

    public String start(String firstName) {
        String name = StringUtils.hasText(firstName) ? " " + firstName : "";
        return "Welcome to WalletLah" + name + ".\n\n"
                + "Log an expense in seconds:\n"
                + "/add 5.50 food chicken rice\n\n"
                + "Then check your month:\n"
                + "/status\n\n"
                + "Use /help anytime.";
    }

    public String help() {
        return "WalletLah commands:\n\n"
                + "/add 5.50 food chicken rice\n"
                + "/status\n"
                + "/recent\n"
                + "/edit 12 amount 7.20\n"
                + "/edit_latest category food\n"
                + "/budget 600\n"
                + "/recurring_add 14.99 subscriptions Spotify monthly\n"
                + "/recurring\n"
                + "/recurring_delete 3\n"
                + "Send a receipt photo to scan it\n"
                + "/email you@example.com\n"
                + "/delete_latest\n"
                + "/category food\n"
                + "/categories\n\n"
                + "Fast logging also works:\n"
                + "5.50 food chicken rice\n"
                + "food 5.50 chicken rice";
    }

    public String emailLinked(String emailAddress) {
        return "Email linked for auto-logging: " + emailAddress + "\n\n"
                + "Forwarded receipt and card transaction emails can now be matched to this WalletLah account.";
    }

    public String expenseAdded(Expense expense, SpendingSummary summary) {
        return "Added: " + MoneyUtils.format(expense.getAmount()) + " "
                + expense.getCategory().displayName() + " - " + expense.getDescription() + "\n\n"
                + compactStatus(summary);
    }

    public String expenseEdited(Expense expense, SpendingSummary summary) {
        return "Updated expense #" + expense.getId() + ": "
                + MoneyUtils.format(expense.getAmount()) + " "
                + expense.getCategory().displayName() + " - " + expense.getDescription() + "\n\n"
                + compactStatus(summary);
    }

    public String receiptScanned(PendingExpense pendingExpense) {
        return "Receipt scanned.\n\n"
                + pendingReceiptDetails(pendingExpense)
                + "\n\n"
                + pendingInstructions(pendingExpense);
    }

    public String receiptEdited(PendingExpense pendingExpense) {
        return "Updated pending receipt.\n\n"
                + pendingReceiptDetails(pendingExpense)
                + "\n\n"
                + pendingInstructions(pendingExpense);
    }

    public String pendingReceiptInstructions(PendingExpense pendingExpense) {
        return "You have a pending receipt scan.\n\n"
                + pendingReceiptDetails(pendingExpense)
                + "\n\n"
                + pendingInstructions(pendingExpense);
    }

    public String receiptSaved(Expense expense, SpendingSummary summary) {
        return "Saved receipt expense: " + MoneyUtils.format(expense.getAmount()) + " "
                + expense.getCategory().displayName() + " - " + expense.getDescription() + "\n\n"
                + compactStatus(summary);
    }

    public String receiptCancelled(PendingExpense pendingExpense) {
        return "Cancelled pending receipt from "
                + (StringUtils.hasText(pendingExpense.getMerchant()) ? pendingExpense.getMerchant() : "receipt scan")
                + ". Nothing was saved.";
    }

    public String status(SpendingSummary summary) {
        return compactStatus(summary);
    }

    public String recent(List<RecentExpenseView> expenses) {
        if (expenses.isEmpty()) {
            return "No expenses yet. Add one with:\n/add 5.50 food chicken rice";
        }

        StringBuilder builder = new StringBuilder("Recent expenses:\n");
        int index = 1;
        for (RecentExpenseView expense : expenses) {
            builder.append(index++)
                    .append(". #")
                    .append(expense.id())
                    .append(" ")
                    .append(expense.expenseDate())
                    .append(" - ")
                    .append(MoneyUtils.format(expense.amount()))
                    .append(" ")
                    .append(expense.category().displayName())
                    .append(sourceLabel(expense.source()))
                    .append(" - ")
                    .append(expense.description())
                    .append('\n');
        }
        builder.append("\nEdit with /edit 12 amount 7.20 or undo latest with /delete_latest");
        return builder.toString().trim();
    }

    public String deleted(Expense expense) {
        return "Deleted latest expense:\n"
                + MoneyUtils.format(expense.getAmount()) + " "
                + expense.getCategory().displayName() + " - "
                + expense.getDescription();
    }

    public String category(ExpenseCategory category, CategoryBreakdownItem item) {
        return category.displayName() + " this month:\n"
                + "Spent: " + MoneyUtils.format(item.total()) + "\n"
                + "Share: " + item.percentage().toPlainString() + "%";
    }

    public String categories(List<CategoryBreakdownItem> breakdown) {
        if (breakdown.isEmpty()) {
            return "Categories:\n" + ExpenseCategory.categoryListForHelp()
                    + "\n\nNo spending logged this month yet.";
        }

        StringBuilder builder = new StringBuilder("Category breakdown this month:\n");
        for (CategoryBreakdownItem item : breakdown) {
            builder.append("- ")
                    .append(item.category().displayName())
                    .append(": ")
                    .append(MoneyUtils.format(item.total()))
                    .append(" (")
                    .append(item.percentage().toPlainString())
                    .append("%)\n");
        }
        return builder.toString().trim();
    }

    public String recurringAdded(RecurringExpense recurringExpense) {
        return "Recurring expense added: #"
                + recurringExpense.getId()
                + " "
                + MoneyUtils.format(recurringExpense.getAmount())
                + " "
                + recurringExpense.getCategory().displayName()
                + " - "
                + recurringExpense.getDescription()
                + "\nFrequency: "
                + recurringExpense.getFrequency()
                + "\nNext run: "
                + recurringExpense.getNextRunDate();
    }

    public String recurringList(List<RecurringExpense> recurringExpenses) {
        if (recurringExpenses.isEmpty()) {
            return "No active recurring expenses.\n\nAdd one with:\n/recurring_add 14.99 subscriptions Spotify monthly";
        }

        StringBuilder builder = new StringBuilder("Active recurring expenses:\n");
        for (RecurringExpense recurringExpense : recurringExpenses) {
            builder.append("#")
                    .append(recurringExpense.getId())
                    .append(" ")
                    .append(MoneyUtils.format(recurringExpense.getAmount()))
                    .append(" ")
                    .append(recurringExpense.getCategory().displayName())
                    .append(" - ")
                    .append(recurringExpense.getDescription())
                    .append(" | ")
                    .append(recurringExpense.getFrequency())
                    .append(" | next ")
                    .append(recurringExpense.getNextRunDate())
                    .append('\n');
        }
        builder.append("\nDelete one with /recurring_delete 3");
        return builder.toString().trim();
    }

    public String recurringCancelled(RecurringExpense recurringExpense) {
        return "Recurring expense cancelled: #"
                + recurringExpense.getId()
                + " "
                + recurringExpense.getDescription();
    }

    private String compactStatus(SpendingSummary summary) {
        StringBuilder builder = new StringBuilder();
        builder.append(summary.monthLabel()).append(" spending: ")
                .append(MoneyUtils.format(summary.totalSpent()));

        if (summary.monthlyBudget() == null) {
            builder.append("\n\nNo budget set yet.\nSet one with /budget 600");
            return builder.toString();
        }

        builder.append(" / ").append(MoneyUtils.format(summary.monthlyBudget()))
                .append("\nRemaining: ").append(MoneyUtils.format(summary.remainingBudget()))
                .append("\nSafe daily spend: ").append(MoneyUtils.format(summary.safeDailySpend()))
                .append("\nDays left: ").append(summary.daysLeftInMonth());

        if (summary.remainingBudget().compareTo(BigDecimal.ZERO) < 0) {
            builder.append("\n\nYou are over budget for this month.");
        }
        return builder.toString();
    }

    private String pendingReceiptDetails(PendingExpense pendingExpense) {
        return "Merchant: " + valueOrMissing(pendingExpense.getMerchant()) + "\n"
                + "Amount: " + (pendingExpense.getAmount() == null ? "Not found" : MoneyUtils.format(pendingExpense.getAmount())) + "\n"
                + "Date: " + valueOrMissing(pendingExpense.getExpenseDate() == null ? null : pendingExpense.getExpenseDate().toString()) + "\n"
                + "Category: " + (pendingExpense.getCategory() == null ? "Not found" : pendingExpense.getCategory().displayName()) + "\n"
                + "Confidence: " + (pendingExpense.getConfidence() == null ? "Unknown" : pendingExpense.getConfidence().toPlainString() + "%");
    }

    private String pendingInstructions(PendingExpense pendingExpense) {
        String amountNote = pendingExpense.getAmount() == null
                ? "\n\nI could not confidently detect the amount. Reply with amount 7.20 before saving."
                : "";
        return "Reply YES to save, NO to cancel, or edit using:\n"
                + "amount 7.20\n"
                + "category food\n"
                + "date 2026-06-05\n"
                + "merchant Koufu"
                + amountNote;
    }

    private String valueOrMissing(String value) {
        return StringUtils.hasText(value) ? value : "Not found";
    }

    private String sourceLabel(ExpenseSource source) {
        if (source == ExpenseSource.RECEIPT_SCAN) {
            return " [receipt]";
        }
        if (source == ExpenseSource.RECURRING) {
            return " [recurring]";
        }
        return "";
    }
}
