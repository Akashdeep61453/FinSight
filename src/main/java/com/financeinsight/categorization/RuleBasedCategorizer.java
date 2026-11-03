package com.financeinsight.categorization;

import com.financeinsight.parser.dto.ParsedTransaction;
import com.financeinsight.transaction.TransactionCategory;
import com.financeinsight.transaction.TransactionDirection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class RuleBasedCategorizer implements Categorizer {

    private static final List<Rule> RULES = List.of(
            new Rule(TransactionCategory.REFUND, List.of("refund", "reversal", "chargeback"), TransactionDirection.CREDIT),
            new Rule(TransactionCategory.TRANSFER, List.of("transfer", "xfer", "upi transfer", "neft", "imps", "rtgs"), null),
            new Rule(TransactionCategory.INCOME, List.of("salary", "payroll", "wages", "interest credit", "dividend"), TransactionDirection.CREDIT),
            new Rule(TransactionCategory.HOUSING, List.of("rent", "landlord", "mortgage", "home loan"), TransactionDirection.DEBIT),
            new Rule(TransactionCategory.GROCERIES, List.of("grocery", "groceries", "supermarket", "food market"), TransactionDirection.DEBIT),
            new Rule(TransactionCategory.TRANSPORT, List.of("fuel", "petrol", "diesel", "metro", "bus", "taxi", "uber", "ola", "parking"), TransactionDirection.DEBIT),
            new Rule(TransactionCategory.UTILITIES, List.of("electricity", "electric bill", "water bill", "internet", "broadband", "mobile recharge", "utility"), TransactionDirection.DEBIT),
            new Rule(TransactionCategory.HEALTHCARE, List.of("pharmacy", "medical", "hospital", "clinic", "doctor", "medicine"), TransactionDirection.DEBIT),
            new Rule(TransactionCategory.DINING, List.of("restaurant", "cafe", "coffee", "food delivery", "swiggy", "zomato"), TransactionDirection.DEBIT),
            new Rule(TransactionCategory.FEES, List.of("bank fee", "service charge", "maintenance charge", "late fee", "annual fee"), TransactionDirection.DEBIT),
            new Rule(TransactionCategory.SHOPPING, List.of("shopping", "retail", "amazon", "flipkart", "mall"), TransactionDirection.DEBIT)
    );

    @Override
    public CategorySuggestion categorize(ParsedTransaction transaction) {
        String description = transaction.description().toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            if (rule.direction() != null && rule.direction() != transaction.direction()) {
                continue;
            }
            for (String keyword : rule.keywords()) {
                if (description.contains(keyword)) {
                    return new CategorySuggestion(rule.category(),
                            "Matched description keyword: " + keyword);
                }
            }
        }
        return new CategorySuggestion(TransactionCategory.UNCATEGORIZED,
                "No description rule matched; review the category");
    }

    private record Rule(TransactionCategory category, List<String> keywords, TransactionDirection direction) {
    }
}





