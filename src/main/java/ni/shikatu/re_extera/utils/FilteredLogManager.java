package ni.shikatu.re_extera.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class FilteredLogManager {
    public static final class Entry {
        public final long timestamp;
        public final long dialogId;
        public final int messageId;
        public final String text;
        public final List<String> matchedPatterns;

        public Entry(long timestamp, long dialogId, int messageId, String text, List<String> matchedPatterns) {
            this.timestamp = timestamp;
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.text = text;
            this.matchedPatterns = Collections.unmodifiableList(new ArrayList<>(matchedPatterns));
        }
    }

    private static final int MAX_ENTRIES = 100;
    private static final LinkedList<Entry> entries = new LinkedList<>();

    private FilteredLogManager() {
    }

    public static synchronized void add(long dialogId, int messageId, String text, List<String> matchedPatterns) {
        if (matchedPatterns == null || matchedPatterns.isEmpty()) {
            return;
        }
        if (entries.size() >= MAX_ENTRIES) {
            entries.removeLast();
        }
        entries.addFirst(new Entry(System.currentTimeMillis(), dialogId, messageId, text, matchedPatterns));
    }

    public static synchronized List<Entry> getEntries() {
        return new ArrayList<>(entries);
    }

    public static synchronized void clear() {
        entries.clear();
    }
}
