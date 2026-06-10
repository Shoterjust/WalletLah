package com.walletlah.dashboard.api;

import com.walletlah.common.MonthRange;
import com.walletlah.common.UserFacingException;
import com.walletlah.dashboard.auth.CurrentDashboardUser;
import com.walletlah.dashboard.auth.DashboardPrincipal;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.expense.ExpenseService;
import com.walletlah.expense.ExpenseSource;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseApiController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CurrentDashboardUser currentDashboardUser;
    private final ExpenseService expenseService;
    private final Clock clock;

    public ExpenseApiController(
            CurrentDashboardUser currentDashboardUser,
            ExpenseService expenseService,
            Clock clock
    ) {
        this.currentDashboardUser = currentDashboardUser;
        this.expenseService = expenseService;
        this.clock = clock;
    }

    @GetMapping
    public PageResponse<ExpenseResponse> list(
            @AuthenticationPrincipal DashboardPrincipal principal,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        var user = currentDashboardUser.requireUser(principal);
        MonthRange range = MonthRange.from(parseMonth(month));
        var expensePage = expenseService.dashboardPage(
                user,
                range.startInclusive(),
                range.endExclusive(),
                parseCategory(category),
                parseSource(source),
                PageRequest.of(Math.max(page, 0), clampSize(size), Sort.unsorted())
        ).map(ExpenseResponse::from);
        return PageResponse.from(expensePage);
    }

    @PostMapping
    public ExpenseResponse create(
            @AuthenticationPrincipal DashboardPrincipal principal,
            @Valid @RequestBody ExpenseCreateRequest request
    ) {
        var user = currentDashboardUser.requireUser(principal);
        var category = parseRequiredCategory(request.category());
        return ExpenseResponse.from(expenseService.addManual(
                user,
                request.amount(),
                category,
                request.description(),
                request.expenseDate(),
                request.merchant()
        ));
    }

    @PatchMapping("/{id}")
    public ExpenseResponse update(
            @AuthenticationPrincipal DashboardPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ExpenseUpdateRequest request
    ) {
        var user = currentDashboardUser.requireUser(principal);
        return ExpenseResponse.from(expenseService.updateFromDashboard(
                user,
                id,
                request.amount(),
                request.category(),
                request.description(),
                request.expenseDate(),
                request.merchant()
        ));
    }

    @DeleteMapping("/{id}")
    public ExpenseResponse delete(
            @AuthenticationPrincipal DashboardPrincipal principal,
            @PathVariable Long id
    ) {
        var user = currentDashboardUser.requireUser(principal);
        return ExpenseResponse.from(expenseService.delete(user, id));
    }

    private YearMonth parseMonth(String value) {
        if (!StringUtils.hasText(value)) {
            return YearMonth.now(clock);
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new UserFacingException("Month must use YYYY-MM format, for example 2026-06.");
        }
    }

    private ExpenseCategory parseCategory(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseRequiredCategory(value);
    }

    private ExpenseCategory parseRequiredCategory(String value) {
        return ExpenseCategory.from(value)
                .orElseThrow(() -> new UserFacingException("Unknown category. Use a valid WalletLah category."));
    }

    private ExpenseSource parseSource(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ExpenseSource.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UserFacingException("Unknown expense source. Use MANUAL, RECEIPT_SCAN, or RECURRING.");
        }
    }

    private int clampSize(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }
}
