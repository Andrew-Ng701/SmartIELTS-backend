package com.andrew.smartielts.dashboard.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DashboardSqlSchemaGuard {

    private static final Pattern FROM_OR_JOIN_PATTERN =
            Pattern.compile("\\b(from|join)\\s+([a-z_][a-z0-9_]*)(?:\\s+(?:as\\s+)?([a-z_][a-z0-9_]*))?",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern QUALIFIED_COLUMN_PATTERN =
            Pattern.compile("\\b([a-z_][a-z0-9_]*)\\.([a-z_][a-z0-9_]*)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern EXPLICIT_ALIAS_PATTERN =
            Pattern.compile("\\bas\\s+([a-z_][a-z0-9_]*)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern NULL_AS_ALIAS_PATTERN =
            Pattern.compile("^null\\s+as\\s+[a-z_][a-z0-9_]*$", Pattern.CASE_INSENSITIVE);

    private static final Set<String> SQL_KEYWORDS = Set.of(
            "where", "group", "order", "limit", "offset", "join", "left", "right",
            "inner", "outer", "on", "having", "union", "cross", "full"
    );

    private final DashboardTableSchemaRegistry schemaRegistry;

    public void validate(String sql, List<String> expectedColumns) {
        validate(sql, expectedColumns, null);
    }

    public void validate(String sql, List<String> expectedColumns, String primaryTable) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql cannot be blank");
        }

        String effectivePrimaryTable = normalize(primaryTable);
        if (effectivePrimaryTable.isBlank()) {
            effectivePrimaryTable = inferPrimaryTable(sql);
        }
        if (effectivePrimaryTable.isBlank() || !schemaRegistry.supports(effectivePrimaryTable)) {
            return;
        }

        DashboardTableSchemaRegistry.DashboardTableSchemaContract contract =
                schemaRegistry.getRequired(effectivePrimaryTable);

        Map<String, String> aliasToTable = extractAliasToTable(sql);
        if (aliasToTable.isEmpty()) {
            throw new IllegalArgumentException("unable to parse from/join tables from sql");
        }

        validateTables(contract, aliasToTable);
        validateReferencedColumns(contract, aliasToTable, sql);

        List<String> selectedAliases = extractSelectAliases(sql);
        validateSelectedAliases(contract, selectedAliases);
        validateExpectedColumns(selectedAliases, expectedColumns);
    }

    private void validateTables(DashboardTableSchemaRegistry.DashboardTableSchemaContract contract,
                                Map<String, String> aliasToTable) {
        for (String table : aliasToTable.values()) {
            if (!contract.allowsTable(table)) {
                throw new IllegalArgumentException(
                        "table " + table + " is not allowed by primaryTable contract " + contract.primaryTable());
            }
        }
    }

    private void validateReferencedColumns(DashboardTableSchemaRegistry.DashboardTableSchemaContract contract,
                                           Map<String, String> aliasToTable,
                                           String sql) {
        Matcher matcher = QUALIFIED_COLUMN_PATTERN.matcher(sql);
        while (matcher.find()) {
            String alias = normalize(matcher.group(1));
            String column = normalize(matcher.group(2));
            String table = aliasToTable.get(alias);
            if (table == null) {
                continue;
            }
            if (!contract.allowsColumn(table, column)) {
                throw new IllegalArgumentException(
                        "column " + alias + "." + column + " is not allowed by contract " + contract.primaryTable());
            }
        }
    }

    private void validateSelectedAliases(DashboardTableSchemaRegistry.DashboardTableSchemaContract contract,
                                         List<String> selectedAliases) {
        if (selectedAliases.isEmpty()) {
            throw new IllegalArgumentException("no select aliases found");
        }

        LinkedHashSet<String> deduplicated = new LinkedHashSet<>(selectedAliases);
        if (deduplicated.size() != selectedAliases.size()) {
            throw new IllegalArgumentException("select aliases must be unique: " + selectedAliases);
        }

        for (String alias : selectedAliases) {
            if (!contract.allowsOutputAlias(alias)) {
                throw new IllegalArgumentException(
                        "output alias " + alias + " is not allowed by contract " + contract.primaryTable());
            }
        }
    }

    private void validateExpectedColumns(List<String> selectedAliases, List<String> expectedColumns) {
        List<String> normalizedExpected = normalizeList(expectedColumns);
        if (normalizedExpected.isEmpty()) {
            throw new IllegalArgumentException("expectedColumns cannot be empty");
        }
        if (!selectedAliases.equals(normalizedExpected)) {
            throw new IllegalArgumentException(
                    "expectedColumns must exactly match select aliases. selected=" + selectedAliases
                            + ", expected=" + normalizedExpected);
        }
    }

    private String inferPrimaryTable(String sql) {
        Matcher matcher = FROM_OR_JOIN_PATTERN.matcher(sql);
        while (matcher.find()) {
            String table = normalize(matcher.group(2));
            if (schemaRegistry.supports(table)) {
                return table;
            }
        }
        return "";
    }

    private Map<String, String> extractAliasToTable(String sql) {
        Matcher matcher = FROM_OR_JOIN_PATTERN.matcher(sql);
        Map<String, String> result = new LinkedHashMap<>();
        while (matcher.find()) {
            String table = normalize(matcher.group(2));
            String alias = normalize(matcher.group(3));
            if (table.isBlank()) {
                continue;
            }
            if (alias.isBlank() || SQL_KEYWORDS.contains(alias)) {
                alias = table;
            }
            result.put(alias, table);
        }
        return result;
    }

    private List<String> extractSelectAliases(String sql) {
        String selectBlock = extractMainSelectBlock(sql);
        if (selectBlock.isBlank()) {
            return List.of();
        }

        List<String> items = splitTopLevelComma(selectBlock);
        List<String> aliases = new ArrayList<>();
        for (String item : items) {
            String trimmed = item == null ? "" : item.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (NULL_AS_ALIAS_PATTERN.matcher(trimmed).matches()) {
                throw new IllegalArgumentException("NULL AS alias placeholders are not allowed: " + trimmed);
            }
            String alias = extractExplicitAlias(trimmed);
            if (alias.isBlank()) {
                throw new IllegalArgumentException("every selected column must use explicit AS alias: " + trimmed);
            }
            aliases.add(alias);
        }
        return aliases;
    }

    private String extractMainSelectBlock(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        int selectIndex = lower.indexOf("select");
        if (selectIndex < 0) {
            return "";
        }
        int fromIndex = findTopLevelFromIndex(lower, selectIndex + 6);
        if (fromIndex <= selectIndex) {
            return "";
        }
        return sql.substring(selectIndex + 6, fromIndex).trim();
    }

    private int findTopLevelFromIndex(String lowerSql, int startIndex) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = startIndex; i < lowerSql.length(); i++) {
            char ch = lowerSql.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (ch == '(') {
                depth++;
                continue;
            }
            if (ch == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0 && lowerSql.startsWith(" from ", i)) {
                return i;
            }
        }
        return -1;
    }

    private List<String> splitTopLevelComma(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(ch);
                continue;
            }
            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(ch);
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                current.append(ch);
                continue;
            }
            if (ch == '(') {
                depth++;
                current.append(ch);
                continue;
            }
            if (ch == ')') {
                depth = Math.max(0, depth - 1);
                current.append(ch);
                continue;
            }
            if (ch == ',' && depth == 0) {
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }

        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private String extractExplicitAlias(String selectItem) {
        Matcher matcher = EXPLICIT_ALIAS_PATTERN.matcher(selectItem);
        if (matcher.find()) {
            return normalize(matcher.group(1));
        }
        return "";
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}