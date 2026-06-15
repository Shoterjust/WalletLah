package com.walletlah.email;

import java.util.Comparator;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

@Component
public class EmailExpenseParser {

    private final List<BankEmailParser> parsers;

    public EmailExpenseParser(List<BankEmailParser> parsers) {
        this.parsers = parsers.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();
    }

    public ParsedEmailExpense parse(EmailExpenseIngestRequest request) {
        return parsers.stream()
                .filter(parser -> parser.supports(request))
                .min(Comparator.comparingInt(parser -> parser instanceof Ordered ordered ? ordered.getOrder() : Ordered.LOWEST_PRECEDENCE))
                .orElseThrow(() -> new IllegalStateException("No email expense parser is available."))
                .parse(request);
    }
}
