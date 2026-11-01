package com.perisic.fish.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser (objects, arrays, strings, numbers, booleans, null) so
 * the project needs no external libraries.
 */
final class Json {
    private final String s;
    private int i = 0;

    private Json(String s) {
        this.s = s;
    }

    static Object parse(String text) {
        Json p = new Json(text);
        return p.value();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    static List<Object> arr(Object o) {
        return (List<Object>) o;
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i)))
            i++;
    }

    private void expect(char c) {
        if (s.charAt(i) != c)
            throw new IllegalArgumentException("Expected '" + c + "' at " + i);
        i++;
    }

    private Object value() {
        ws();
        switch (s.charAt(i)) {
            case '{':
                return object();
            case '[':
                return array();
            case '"':
                return string();
            case 't':
                i += 4;
                return Boolean.TRUE;
            case 'f':
                i += 5;
                return Boolean.FALSE;
            case 'n':
                i += 4;
                return null;
            default:
                return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        expect('{');
        ws();
        if (s.charAt(i) == '}') {
            i++;
            return m;
        }
        while (true) {
            ws();
            String k = string();
            ws();
            expect(':');
            m.put(k, value());
            ws();
            char c = s.charAt(i++);
            if (c == '}')
                return m;
            if (c != ',')
                throw new IllegalArgumentException("Bad object at " + i);
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        expect('[');
        ws();
        if (s.charAt(i) == ']') {
            i++;
            return l;
        }
        while (true) {
            l.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ']')
                return l;
            if (c != ',')
                throw new IllegalArgumentException("Bad array at " + i);
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"')
                return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = s.charAt(i++);
            switch (e) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                    break;
                default: sb.append(e); // \" \\ \/
            }
        }
    }

    private Double number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0)
            i++;
        return Double.valueOf(s.substring(start, i));
    }
}
