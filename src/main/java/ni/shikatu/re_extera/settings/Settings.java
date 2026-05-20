package ni.shikatu.re_extera.settings;

import android.content.SharedPreferences;
import ni.shikatu.re_extera.Main;

public final class Settings {
    public static final String PREFS_NAME = "re_extera";
    private static volatile SharedPreferences cachedPrefs;

    private Settings() {
    }

    public enum SendSilence {
        NO(0),
        YES(1),
        ONLY_WITH_GHOST(2);

        private final int type;

        SendSilence(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }

        public static SendSilence getValue(int value) {
            for (SendSilence s : values()) {
                if (s.type == value) {
                    return s;
                }
            }
            return NO;
        }
    }

    private static SharedPreferences prefs() {
        SharedPreferences sharedPreferences;
        SharedPreferences local = cachedPrefs;
        if (local != null) {
            return local;
        }
        synchronized (Settings.class) {
            if (cachedPrefs == null) {
                cachedPrefs = Main.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
            }
            sharedPreferences = cachedPrefs;
        }
        return sharedPreferences;
    }

    private static int getInt(String key, int def) {
        return prefs().getInt(key, def);
    }

    private static String getString(String key, String def) {
        return prefs().getString(key, def);
    }

    private static boolean getBool(String key, boolean def) {
        return prefs().getBoolean(key, def);
    }

    private static void putInt(String key, int value) {
        prefs().edit().putInt(key, value).apply();
    }

    private static void putString(String key, String value) {
        prefs().edit().putString(key, value).apply();
    }

    private static void putBool(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    public static boolean getGhostModeEnabledGlobal() {
        return getBool("ghost_mode_enabled", true);
    }

    public static void setGhostModeEnabledGlobal(boolean v) {
        putBool("ghost_mode_enabled", v);
    }

    public static boolean getHideOnline() {
        return getBool("hide_online", false);
    }

    public static void setHideOnline(boolean v) {
        putBool("hide_online", v);
    }

    public static boolean getHideTyping() {
        return getBool("hide_typing", false);
    }

    public static void setHideTyping(boolean v) {
        putBool("hide_typing", v);
    }

    public static boolean getHideReading() {
        return getBool("hide_reading", false);
    }

    public static void setHideReading(boolean v) {
        putBool("hide_reading", v);
    }

    public static boolean getNoReadStories() {
        return getBool("no_read_stories", false);
    }

    public static void setNoReadStories(boolean v) {
        putBool("no_read_stories", v);
    }

    public static boolean getImmediateOffline() {
        return getBool("immediate_offline", false);
    }

    public static void setImmediateOffline(boolean v) {
        putBool("immediate_offline", v);
    }

    public static boolean getReadOnInteract() {
        return getBool("read_on_interact", false);
    }

    public static void setReadOnInteract(boolean v) {
        putBool("read_on_interact", v);
    }

    public static int getSendSilence() {
        return getInt("send_silence", SendSilence.NO.getType());
    }

    public static void setSendSilence(SendSilence value) {
        putInt("send_silence", value.getType());
    }

    public static boolean getHideOnlineWithGhost() {
        return getHideOnline() && getGhostModeEnabledGlobal();
    }

    public static boolean getHideTypingWithGhost() {
        return getHideTyping() && getGhostModeEnabledGlobal();
    }

    public static boolean getHideReadingWithGhost() {
        return getHideReading() && getGhostModeEnabledGlobal();
    }

    public static boolean getNoReadStoriesWithGhost() {
        return getNoReadStories() && getGhostModeEnabledGlobal();
    }

    public static boolean getImmediateOfflineWithGhost() {
        return getImmediateOffline() && getGhostModeEnabledGlobal();
    }

    public static int countOfGhost() {
        int c = getHideOnline() ? 0 + 1 : 0;
        if (getHideTyping()) {
            c++;
        }
        if (getHideReading()) {
            c++;
        }
        if (getNoReadStories()) {
            c++;
        }
        return getImmediateOffline() ? c + 1 : c;
    }

    public static boolean getSaveDeletedMessages() {
        return getBool("save_deleted_messages", false);
    }

    public static void setSaveDeletedMessages(boolean v) {
        putBool("save_deleted_messages", v);
    }

    public static boolean getSaveEditedMessages() {
        return getBool("save_edited_messages", false);
    }

    public static void setSaveEditedMessages(boolean v) {
        putBool("save_edited_messages", v);
    }

    public static boolean getSaveManuallyDeleted() {
        return getBool("save_manually_deleted", false);
    }

    public static void setSaveManuallyDeleted(boolean v) {
        putBool("save_manually_deleted", v);
    }

    public static boolean getSaveOneTimeMessages() {
        return getBool("save_one_time_messages", false);
    }

    public static void setSaveOneTimeMessages(boolean v) {
        putBool("save_one_time_messages", v);
    }

    public static String getCustomPrefix() {
        return getString("custom_prefix", "");
    }

    public static void setCustomPrefix(String v) {
        putString("custom_prefix", v);
    }

    public static boolean getRedMark() {
        return getBool("red_mark", false);
    }

    public static void setRedMark(boolean v) {
        putBool("red_mark", v);
    }

    public static boolean getUseExpandableBlockQuote() {
        return getBool("use_expandable_blockquote", false);
    }

    public static void setUseExpandableBlockQuote(boolean v) {
        putBool("use_expandable_blockquote", v);
    }

    public static boolean noForward() {
        return getBool("no_forward", false);
    }

    public static void setNoForward(boolean v) {
        putBool("no_forward", v);
    }

    public static boolean getRemoveFlagSecure() {
        return getBool("remove_flag_secure", false);
    }

    public static void setRemoveFlagSecure(boolean v) {
        putBool("remove_flag_secure", v);
    }

    public static boolean getUseSchedule() {
        return getBool("use_schedule", false);
    }

    public static void setUseSchedule(boolean v) {
        putBool("use_schedule", v);
    }

    public static boolean getFiltersEnabled() {
        return getBool("filters_enabled", false);
    }

    public static void setFiltersEnabled(boolean v) {
        putBool("filters_enabled", v);
    }

    public static boolean getAddGhostToDrawer() {
        return getBool("add_ghost_to_drawer", true);
    }

    public static void setAddGhostToDrawer(boolean v) {
        putBool("add_ghost_to_drawer", v);
    }

    public static boolean getShowSettingsInDrawer() {
        return getBool("show_settings_in_drawer", true);
    }

    public static void setShowSettingsInDrawer(boolean v) {
        putBool("show_settings_in_drawer", v);
    }

    public static boolean getLocalPremium() {
        return getBool("local_premium", false);
    }

    public static void setLocalPremium(boolean v) {
        putBool("local_premium", v);
    }

    public static boolean getGhostInMainMenu() {
        return getBool("ghost_in_main_menu", false);
    }

    public static void setGhostInMainMenu(boolean v) {
        putBool("ghost_in_main_menu", v);
    }

    public static int getGhostMenuIndex() {
        return getInt("ghost_menu_index", 0);
    }

    public static void setGhostMenuIndex(int v) {
        putInt("ghost_menu_index", v);
    }

    public static boolean isGhostPositionInitialized() {
        return getBool("ghost_position_initialized", false);
    }

    public static void setGhostPositionInitialized(boolean v) {
        putBool("ghost_position_initialized", v);
    }
}
