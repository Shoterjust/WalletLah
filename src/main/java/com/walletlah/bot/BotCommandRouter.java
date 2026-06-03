package com.walletlah.bot;

import com.walletlah.analytics.AnalyticsService;
import com.walletlah.budget.BudgetService;
import com.walletlah.common.MoneyUtils;
import com.walletlah.common.UserFacingException;
import com.walletlah.expense.AddExpenseParser;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.expense.ExpenseService;
import com.walletlah.user.UserService;
import java.math.BigDecimal;
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
    private final TelegramResponseFormatter formatter;

    public BotCommandRouter(
            UserService userService,
            ExpenseService expenseService,
            BudgetService budgetService,
            AnalyticsService analyticsService,
            AddExpenseParser addExpenseParser,
            TelegramResponseFormatter formatter
    ) {
        this.userService = userService;
        this.expenseService = expenseService;
        this.budgetService = budgetService;
        this.analyticsService = analyticsService;
        this.addExpenseParser = addExpenseParser;
        this.formatter = formatter;
    }

    public String handle(TelegramUserContext context, String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (!StringUtils.hasText(text)) {
            return formatter.help();
        }

        try {
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
            if (isCommand(text, "/status")) {
                var user = userService.registerOrUpdate(context);
                return formatter.status(analyticsService.monthlySummary(user));
            }
            if (isCommand(text, "/recent")) {
                var user = userService.registerOrUpdate(context);
                return formatter.recent(expenseService.recent(user, 5));
            }
            if (isCommand(text, "/budget")) {
                var user = userService.registerOrUpdate(context);
                BigDecimal amount = parseBudget(commandBody(text));
                var budget = budgetService.setCurrentMonthBudget(user, amount);
                return "Budget set for this month: " + MoneyUtils.format(budget.getAmount()) + "\n\n"
                        + formatter.status(analyticsService.monthlySummary(user));
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
            if (isCommand(text, "/categories")) {
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

    private BigDecimal parseBudget(String body) {
        if (!StringUtils.hasText(body)) {
            throw new UserFacingException("Use /budget 600 to set your monthly budget.");
        }
        try {
            BigDecimal amount = new BigDecimal(body.trim());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new UserFacingException("Budget cannot be negative.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new UserFacingException("I could not read that budget. Try /budget 600");
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
