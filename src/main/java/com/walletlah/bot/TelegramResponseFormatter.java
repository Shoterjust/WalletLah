package com.walletlah.bot;

import com.walletlah.analytics.CategoryBreakdownItem;
import com.walletlah.analytics.SpendingSummary;
import com.walletlah.common.MoneyUtils;
import com.walletlah.expense.Expense;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.expense.RecentExpenseView;
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
                + "/budget 600\n"
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
                    .append(". ")
                    .append(expense.expenseDate())
                    .append(" - ")
                    .append(MoneyUtils.format(expense.amount()))
                    .append(" ")
                    .append(expense.category().displayName())
                    .append(" - ")
                    .append(expense.description())
                    .append('\n');
        }
        builder.append("\nUndo your latest entry with /delete_latest");
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
}
