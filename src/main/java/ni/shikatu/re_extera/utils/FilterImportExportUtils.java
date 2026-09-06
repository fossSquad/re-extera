package ni.shikatu.re_extera.utils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public final class FilterImportExportUtils {
    private FilterImportExportUtils() {
    }

    public static List<String> parseFilters(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String trimmed = input.trim();
        LinkedHashSet<String> set = new LinkedHashSet<>();

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                if (trimmed.startsWith("{")) {
                    JSONObject obj = new JSONObject(trimmed);
                    // AyuGram backup: {"version": 2, "filters": [{"text": "...", "enabled": true, ...}]}
                    if (obj.has("filters")) {
                        JSONArray arr = obj.getJSONArray("filters");
                        for (int i = 0; i < arr.length(); i++) {
                            Object item = arr.get(i);
                            if (item instanceof JSONObject) {
                                JSONObject fo = (JSONObject) item;
                                if (fo.has("text")) {
                                    addIfValidRegex(set, fo.getString("text"));
                                } else if (fo.has("regex")) {
                                    addIfValidRegex(set, fo.getString("regex"));
                                }
                            } else if (item instanceof String) {
                                addIfValidRegex(set, (String) item);
                            }
                        }
                    }
                } else if (trimmed.startsWith("[")) {
                    JSONArray arr = new JSONArray(trimmed);
                    for (int i = 0; i < arr.length(); i++) {
                        Object item = arr.get(i);
                        if (item instanceof JSONObject) {
                            JSONObject fo = (JSONObject) item;
                            if (fo.has("regex")) {
                                addIfValidRegex(set, fo.getString("regex"));
                            } else if (fo.has("text")) {
                                addIfValidRegex(set, fo.getString("text"));
                            }
                        } else if (item instanceof String) {
                            addIfValidRegex(set, (String) item);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (set.isEmpty()) {
            try (BufferedReader reader = new BufferedReader(new StringReader(trimmed))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        addIfValidRegex(set, line);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return new ArrayList<>(set);
    }

    private static void addIfValidRegex(LinkedHashSet<String> set, String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return;
        }
        try {
            Pattern.compile(pattern.trim());
            set.add(pattern.trim());
        } catch (Exception ignored) {
        }
    }

    public static String exportToJson(List<String> patterns) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            JSONArray arr = new JSONArray();
            if (patterns != null) {
                for (String p : patterns) {
                    JSONObject fo = new JSONObject();
                    fo.put("regex", p);
                    arr.put(fo);
                }
            }
            root.put("filters", arr);
            return root.toString(2);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String exportToPlainText(List<String> patterns) {
        StringBuilder sb = new StringBuilder();
        if (patterns != null) {
            for (String p : patterns) {
                sb.append(p).append("\n");
            }
        }
        return sb.toString();
    }
}
