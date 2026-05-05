package com.andrew.smartielts.dashboard.query;

import com.andrew.smartielts.dashboard.constants.DashboardTableNameConstants;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DashboardAiSqlPolicyGuard {

    private static final Set<String> ALLOWED_TABLE_TOKENS = Set.copyOf(DashboardTableNameConstants.ALL_TABLES);

    private static final Pattern TABLE_PATTERN =
            Pattern.compile("\\b(?:from|join)\\s+([a-z_][a-z0-9_]*)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern MUTATION_PATTERN =
            Pattern.compile("\\b(insert|update|delete|drop|alter|truncate|create|replace|merge|grant|revoke)\\b",
                    Pattern.CASE_INSENSITIVE);

    public void validate(String sql, SecureDashboardQueryRequest request) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL cannot be blank");
        }

        String normalizedSql = normalize(sql);
        if (!normalizedSql.startsWith("select")) {
            throw new AccessDeniedException("Only SELECT SQL is allowed");
        }

        if (MUTATION_PATTERN.matcher(normalizedSql).find()) {
            throw new AccessDeniedException("Mutation SQL is not allowed");
        }

        Set<String> referencedTables = extractReferencedTables(normalizedSql);
        if (referencedTables.isEmpty()) {
            throw new AccessDeniedException("No supported table found in AI SQL");
        }

        for (String table : referencedTables) {
            if (!ALLOWED_TABLE_TOKENS.contains(table)) {
                throw new AccessDeniedException("AI SQL references unsupported table: " + table);
            }
        }
    }

    private Set<String> extractReferencedTables(String sql) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1);
            if (table != null && !table.isBlank()) {
                result.add(table.trim().toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private String normalize(String sql) {
        return sql == null ? "" : sql.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}