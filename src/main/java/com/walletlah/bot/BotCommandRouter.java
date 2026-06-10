package com.walletlah.bot;

import com.walletlah.analytics.AnalyticsService;
import com.walletlah.budget.BudgetService;
import com.walletlah.common.MoneyUtils;
import com.walletlah.common.UserFacingException;
import com.walletlah.dashboard.auth.DashboardLinkCodeService;
import com.walletlah.expense.AddExpenseParser;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.expense.ExpenseService;
import com.walletlah.receipt.PendingExpenseService;
import com.walletlah.recurring.AddRecurringExpenseParser;
import com.walletlah.recurring.RecurringExpenseService;
import com.walletlah.user.UserService;
import java.math.BigDecimal;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BotCommandRouter {

    private static final Logger log = LoggerFactory.getLogger(BotCommandRouter.class);

    private final UserService userService;
    private final ExpenseService expenseService;
    private final BudgetService budgetService;
    private final AnalyticsService analyticsService;
    private final AddExpenseParser addExpenseParser;
    private final PendingExpenseService pendingExpenseService;
    private final AddRecurringExpenseParser addRecurringExpenseParser;
    private final RecurringExpenseService recurringExpenseService;
    private final DashboardLinkCodeService dashboardLinkCodeService;
    private final TelegramResponseFormatter formatter;

    public BotCommandRouter(
            UserService userService,
            ExpenseService expenseService,
            BudgetService budgetService,
            AnalyticsService analyticsService,
            AddExpenseParser addExpenseParser,
            PendingExpenseService pendingExpenseService,
            AddRecurringExpenseParser addRecurringExpenseParser,
            RecurringExpenseService recurringExpenseService,
            DashboardLinkCodeService dashboardLinkCodeService,
            TelegramResponseFormatter formatter
    ) {
        this.userService = userService;
        this.expenseService = expenseService;
        this.budgetService = budgetService;
        this.analyticsService = analyticsService;
        this.addExpenseParser = addExpenseParser;
        this.pendingExpenseService = pendingExpenseService;
        this.addRecurringExpenseParser = addRecurringExpenseParser;
        this.recurringExpenseService = recurringExpenseService;
        this.dashboardLinkCodeService = dashboardLinkCodeService;
        this.formatter = formatter;
    }

    public String handle(TelegramUserContext context, String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (!StringUtils.hasText(text)) {
            return formatter.help();
        }

        try {
            if (shouldHandleAsPendingReceiptReply(context.telegramUserId(), text)) {
                return handlePendingReceiptReply(context, text);
            }
            if (isCommand(text, "/start")) {
                var user = userService.registerOrUpdate(context);
                return formatter.start(user.getFirstName());
            }
            if (isCommand(text, "/help")) {
                userService.registerOrUpdate(context);
                return formatter.help();
            }
            if (isCommand(text, "/add")) {
                return addExpense(context, commandBody(text));
            }
            if (isCommand(text, "/status") || isCommand(text, "/summary")) {
                var user = userService.registerOrUpdate(context);
                return formatter.status(analyticsService.monthlySummary(user));
            }
            if (isCommand(text, "/recent")) {
                var user = userService.registerOrUpdate(context);
                return formatter.recent(expenseService.recent(user, parseRecentLimit(commandBody(text))));
            }
            if (isCommand(text, "/edit")) {
                return editExpense(context, commandBody(text));
            }
            if (isCommand(text, "/edit_latest")) {
                return editLatestExpense(context, commandBody(text));
            }
            if (isCommand(text, "/budget")) {
                var user = userService.registerOrUpdate(context);
                if (!StringUtils.hasText(commandBody(text))) {
                    return formatter.budgetStatus(analyticsService.monthlySummary(user));
                }
                BigDecimal amount = parseBudget(commandBody(text));
                var budget = budgetService.setCurrentMonthBudget(user, amount);
                return "Budget set for this month: " + MoneyUtils.format(budget.getAmount()) + "\n\n"
                        + formatter.status(analyticsService.monthlySummary(user));
            }
            if (isCommand(text, "/dashboard_link")) {
                var user = userService.registerOrUpdate(context);
                return formatter.dashboardLinkCode(dashboardLinkCodeService.issueCode(user));
            }
            if (isCommand(text, "/email")) {
                var user = userService.linkEmail(context, commandBody(text));
                return formatter.emailLinked(user.getEmailAddress());
            }
            if (isCommand(text, "/recurring_add")) {
                var user = userService.registerOrUpdate(context);
                var request = addRecurringExpenseParser.parse(commandBody(text));
                return formatter.recurringAdded(recurringExpenseService.add(user, request));
            }
            if (isCommand(text, "/recurring")) {
                var user = userService.registerOrUpdate(context);
                return formatter.recurringList(recurringExpenseService.listActive(user));
            }
            if (isCommand(text, "/recurring_delete") || isCommand(text, "/recurring_cancel")) {
                var user = userService.registerOrUpdate(context);
                Long id = parseId(commandBody(text), "Use /recurring_delete 3");
                return formatter.recurringCancelled(recurringExpenseService.cancel(user, id));
            }
            if (isCommand(text, "/delete_latest")) {
                var user = userService.registerOrUpdate(context);
                return formatter.deleted(expenseService.deleteLatest(user));
            }
            if (isCommand(text, "/category")) {
                var user = userService.registerOrUpdate(context);
                ExpenseCategory category = ExpenseCategory.from(commandBody(text))
                        .orElseThrow(() -> new UserFacingException("Use /category food or /categories to see valid categories."));
                return formatter.category(category, analyticsService.categorySummary(user, category));
            }
            if (isCommand(text, "/categories") || isCommand(text, "/breakdown")) {
                var user = userService.registerOrUpdate(context);
                return formatter.categories(analyticsService.categoryBreakdown(user));
            }
            if (text.startsWith("/")) {
                return "I do not know that command yet.\n\n" + formatter.help();
            }
            return addExpense(context, text);
        } catch (UserFacingException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            log.error("Unexpected bot command failure for text '{}'", text, e);
            return "Something went wrong while handling that. Please try again.";
        }
    }

    private String addExpense(TelegramUserContext context, String body) {
        var user = userService.registerOrUpdate(context);
        var request = addExpenseParser.parse(body);
        var expense = expenseService.add(user, request);
        return formatter.expenseAdded(expense, analyticsService.monthlySummary(user));
    }

    private String editExpense(TelegramUserContext context, String body) {
        String[] parts = body.split("\\s+", 3);
        if (parts.length < 3) {
            throw new UserFacingException("Use /edit 12 amount 7.20 or /edit 12 category food");
        }
        Long expenseId = parseId(parts[0], "Use /edit 12 amount 7.20");
        var user = userService.registerOrUpdate(context);
        var expense = expenseService.edit(user, expenseId, parts[1], parts[2]);
        return formatter.expenseEdited(expense, analyticsService.monthlySummary(user));
    }

    private String editLatestExpense(TelegramUserContext context, String body) {
        String[] parts = body.split("\\s+", 2);
        if (parts.length < 2) {
            throw new UserFacingException("Use /edit_latest amount 7.20 or /edit_latest category food");
        }
        var user = userService.registerOrUpdate(context);
        var expense = expenseService.editLatest(user, parts[0], parts[1]);
        return formatter.expenseEdited(expense, analyticsService.monthlySummary(user));
    }

    private boolean shouldHandleAsPendingReceiptReply(Long telegramUserId, String text) {
        if (text.startsWith("/")) {
            return false;
        }
        return pendingExpenseService.looksLikePendingReply(text)
                || pendingExpenseService.activePending(telegramUserId).isPresent();
    }

    private String handlePendingReceiptReply(TelegramUserContext context, String text) {
        var user = userService.registerOrUpdate(context);
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("yes") || normalized.equals("y")) {
            var expense = pendingExpenseService.confirm(user);
            return formatter.receiptSaved(expense, analyticsService.monthlySummary(user));
        }
        if (normalized.equals("no") || normalized.equals("n")) {
            var pending = pendingExpenseService.cancel(context.telegramUserId());
            return formatter.receiptCancelled(pending);
        }
        if (pendingExpenseService.looksLikePendingReply(text)) {
            var pending = pendingExpenseService.edit(context.telegramUserId(), text);
            return formatter.receiptEdited(pending);
        }
        return pendingExpenseService.activePending(context.telegramUserId())
                .map(formatter::pendingReceiptInstructions)
                .orElse("No pending receipt scan. Send a receipt photo first, or log manually with /add 5.50 food chicken rice");
    }

    private BigDecimal parseBudget(String body) {
        if (!StringUtils.hasText(body)) {
            throw new UserFacingException("Use /budget 600 to set your monthly budget.");
        }
        try {
            BigDecimal amount = new BigDecimal(body.replace("S$", "").replace("$", "").trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new UserFacingException("Budget must be more than zero.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new UserFacingException("I could not read that budget. Try /budget 600");
        }
    }

    private int parseRecentLimit(String body) {
        if (!StringUtils.hasText(body)) {
            return 5;
        }
        try {
            int limit = Integer.parseInt(body.trim());
            if (limit <= 0) {
                throw new UserFacingException("Use /recent or /recent 10");
            }
            return Math.min(limit, 20);
        } catch (NumberFormatException e) {
            throw new UserFacingException("Use /recent or /recent 10");
        }
    }

    private Long parseId(String rawValue, String errorMessage) {
        if (!StringUtils.hasText(rawValue)) {
            throw new UserFacingException(errorMessage);
        }
        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new UserFacingException(errorMessage);
        }
    }

    private boolean isCommand(String text, String command) {
        String firstToken = text.split("\\s+", 2)[0];
        int mentionIndex = firstToken.indexOf('@');
        if (mentionIndex >= 0) {
            firstToken = firstToken.substring(0, mentionIndex);
        }
        return firstToken.equalsIgnoreCase(command);
    }

    private String commandBody(String text) {
        String[] parts = text.split("\\s+", 2);
        return parts.length == 2 ? parts[1].trim() : "";
    }
}
