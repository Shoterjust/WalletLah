package com.walletlah.dashboard.api;

import com.walletlah.common.UserFacingException;
import com.walletlah.dashboard.auth.CurrentDashboardUser;
import com.walletlah.dashboard.auth.DashboardPrincipal;
import com.walletlah.expense.ExpenseCategory;
import com.walletlah.recurring.AddRecurringExpenseRequest;
import com.walletlah.recurring.RecurringExpenseService;
import com.walletlah.recurring.RecurringFrequency;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseApiController {

    private final CurrentDashboardUser currentDashboardUser;
    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseApiController(
            CurrentDashboardUser currentDashboardUser,
            RecurringExpenseService recurringExpenseService
    ) {
        this.currentDashboardUser = currentDashboardUser;
        this.recurringExpenseService = recurringExpenseService;
    }

    @GetMapping
    public List<RecurringExpenseResponse> list(@AuthenticationPrincipal DashboardPrincipal principal) {
        var user = currentDashboardUser.requireUser(principal);
        return recurringExpenseService.listActive(user).stream()
                .map(RecurringExpenseResponse::from)
                .toList();
    }

    @PostMapping
    public RecurringExpenseResponse create(
            @AuthenticationPrincipal DashboardPrincipal principal,
            @Valid @RequestBody RecurringExpenseCreateRequest request
    ) {
        var user = currentDashboardUser.requireUser(principal);
        var category = ExpenseCategory.from(request.category())
                .orElseThrow(() -> new UserFacingException("Unknown category. Use a valid WalletLah category."));
        var recurringExpense = recurringExpenseService.add(user, new AddRecurringExpenseRequest(
                request.amount(),
                category,
                request.description(),
                request.merchant(),
                RecurringFrequency.parse(request.frequency()),
                request.nextRunDate()
        ));
        return RecurringExpenseResponse.from(recurringExpense);
    }

    @DeleteMapping("/{id}")
    public RecurringExpenseResponse delete(
            @AuthenticationPrincipal DashboardPrincipal principal,
            @PathVariable Long id
    ) {
        var user = currentDashboardUser.requireUser(principal);
        return RecurringExpenseResponse.from(recurringExpenseService.cancel(user, id));
    }
}
