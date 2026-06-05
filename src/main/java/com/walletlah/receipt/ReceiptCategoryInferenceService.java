package com.walletlah.receipt;

import com.walletlah.expense.ExpenseCategory;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ReceiptCategoryInferenceService {

    public ExpenseCategory infer(String merchant, String rawText) {
        String text = ((merchant == null ? "" : merchant) + " " + (rawText == null ? "" : rawText))
                .toLowerCase(Locale.ROOT);

        if (containsAny(text, "koufu", "kopitiam", "mcdonald", "mcdonald's", "kfc", "subway", "starbucks", "toast box", "yakun", "foodpanda", "grabfood", "hawker", "restaurant", "cafe")) {
            return ExpenseCategory.FOOD;
        }
        if (containsAny(text, "mrt", "simplygo", "grab", "gojek", "comfortdelgro", "cdg", "taxi", "bus", "smrt")) {
            return ExpenseCategory.TRANSPORT;
        }
        if (containsAny(text, "fairprice", "sheng siong", "cold storage", "giant", "supermarket", "grocery")) {
            return ExpenseCategory.GROCERIES;
        }
        if (containsAny(text, "popular", "stationery", "bookstore", "nus", "ntu", "smu", "sim", "printing")) {
            return ExpenseCategory.SCHOOL;
        }
        if (containsAny(text, "netflix", "spotify", "icloud", "youtube", "apple", "google", "openai", "github")) {
            return ExpenseCategory.SUBSCRIPTIONS;
        }
        return ExpenseCategory.OTHERS;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
