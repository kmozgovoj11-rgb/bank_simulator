package presentation.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object value = new Parser(json).parseValue();
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("JSON body must be an object");
        }
        return (Map<String, Object>) map;
    }

    public static String stringify(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "\"" + escape(string) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(stringify(String.valueOf(entry.getKey())));
                sb.append(':');
                sb.append(stringify(entry.getValue()));
            }
            return sb.append('}').toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(stringify(item));
            }
            return sb.append(']').toString();
        }
        if (value.getClass().isRecord()) {
            return stringify(recordToMap(value));
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    private static Map<String, Object> recordToMap(Object record) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            for (var component : record.getClass().getRecordComponents()) {
                result.put(component.getName(), component.getAccessor().invoke(record));
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot serialize record " + record.getClass().getName(), e);
        }
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source == null ? "" : source.trim();
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= source.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char current = source.charAt(index);
            return switch (current) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (current == '-' || Character.isDigit(current)) {
                        yield parseNumber();
                    }
                    throw new IllegalArgumentException("Unexpected token at position " + index);
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return result;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (index < source.length()) {
                char ch = source.charAt(index++);
                if (ch == '"') {
                    return sb.toString();
                }
                if (ch == '\\') {
                    if (index >= source.length()) {
                        throw new IllegalArgumentException("Invalid escape sequence");
                    }
                    char escaped = source.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> sb.append(escaped);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (index + 4 > source.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape");
                            }
                            String hex = source.substring(index, index + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> throw new IllegalArgumentException("Invalid escape sequence");
                    }
                } else {
                    sb.append(ch);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private Object parseNumber() {
            int start = index;
            if (source.charAt(index) == '-') {
                index++;
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            if (index < source.length() && source.charAt(index) == '.') {
                index++;
                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            String token = source.substring(start, index);
            return token.contains(".") ? Double.parseDouble(token) : Long.parseLong(token);
        }

        private Object parseLiteral(String expected, Object value) {
            if (!source.startsWith(expected, index)) {
                throw new IllegalArgumentException("Unexpected token at position " + index);
            }
            index += expected.length();
            return value;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= source.length() || source.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + index);
            }
            index++;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < source.length() && source.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }
    }
}
