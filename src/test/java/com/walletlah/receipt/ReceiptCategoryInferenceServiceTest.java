package com.walletlah.receipt;

import static org.assertj.core.api.Assertions.assertThat;

import com.walletlah.expense.ExpenseCategory;
import org.junit.jupiter.api.Test;

class ReceiptCategoryInferenceServiceTest {

    private final ReceiptCategoryInferenceService service = new ReceiptCategoryInferenceService();

    @Test
    void infersFoodMerchants() {
        assertThat(service.infer("Koufu", "")).isEqualTo(ExpenseCategory.FOOD);
        assertThat(service.infer("McDonald's", "")).isEqualTo(ExpenseCategory.FOOD);
    }

    @Test
    void infersTransportMerchants() {
        assertThat(service.infer("SimplyGo", "")).isEqualTo(ExpenseCategory.TRANSPORT);
        assertThat(service.infer("ComfortDelGro", "")).isEqualTo(ExpenseCategory.TRANSPORT);
    }

    @Test
    void infersGroceriesAndSchool() {
        assertThat(service.infer("FairPrice", "")).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(service.infer("Popular bookstore", "")).isEqualTo(ExpenseCategory.SCHOOL);
    }
}
