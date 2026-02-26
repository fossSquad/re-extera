package ni.shikatu.re_extera.settings;

import ni.shikatu.re_extera.Defaults;
import ni.shikatu.re_extera.Main;

public class Settings {

    public enum SendSilence {
        YES,
        NO,
        ONLY_WITH_GHOST;

        public int getType() {
            switch (ordinal()) {
                case Defaults.GLOBAL_VALUE /* 0 */:
                    return 1;
                case Defaults.ALWAYS /* 1 */:
                    return 0;
                case 2:
                    return 2;
                default:
                    return 0;
            }
        }

        public static SendSilence getValue(int value) {
            switch (value) {
                case Defaults.ALWAYS /* 1 */:
                    return YES;
                case 2:
                    return ONLY_WITH_GHOST;
                default:
                    return NO;
            }
        }
    }

    private static int get(String settingName, int defaultValue) {
        return Main.getApplicationContext().getSharedPreferences("re_extera", 0).getInt(settingName, defaultValue);
    }

    private static String get(String settingName, String defaultValue) {
        return Main.getApplicationContext().getSharedPreferences("re_extera", 0).getString(settingName, defaultValue);
    }

    private static boolean get(String settingName, boolean defaultValue) {
        return Main.getApplicationContext().getSharedPreferences("re_extera", 0).getBoolean(settingName, defaultValue);
    }

    private static void set(String settingName, int value) {
        Main.getApplicationContext().getSharedPreferences("re_extera", 0).edit().putInt(settingName, value).apply();
    }

    private static void set(String settingName, String value) {
        Main.getApplicationContext().getSharedPreferences("re_extera", 0).edit().putString(settingName, value).apply();
    }

    private static void set(String settingName, boolean value) {
        Main.getApplicationContext().getSharedPreferences("re_extera", 0).edit().putBoolean(settingName, value).apply();
    }

    public static boolean getImmediateOffline() {
        return get("immediate_offline", false);
    }

    public static void setImmediateOffline(boolean value) {
        set("immediate_offline", value);
    }

    public static void setSaveManuallyDeleted(boolean value) {
        set("save_manually_deleted", value);
    }

    public static boolean getSaveManuallyDeleted() {
        return get("save_manually_deleted", false);
    }

    public static boolean getSaveOneTimeMessages() {
        return get("save_one_time_messages", false);
    }

    public static void setSaveOneTimeMessages(boolean value) {
        set("save_one_time_messages", value);
    }

    public static String getCustomPrefix() {
        return get("custom_prefix", "");
    }

    public static void setCustomPrefix(String value) {
        set("custom_prefix", value);
    }

    public static boolean noForward() {
        return get("no_forward", false);
    }

    public static void setNoForward(boolean value) {
        set("no_forward", value);
    }

    public static boolean getRedMark() {
        return get("red_mark", false);
    }

    public static void setRedMark(boolean value) {
        set("red_mark", value);
    }

    public static boolean getHideOnline() {
        return get("hide_online", false);
    }

    public static boolean getHideTyping() {
        return get("hide_typing", false);
    }

    public static boolean getHideReading() {
        return get("hide_reading", false);
    }

    public static boolean getNoReadStories() {
        return get("no_read_stories", false);
    }

    public static boolean getRemoveFlagSecure() {
        return get("remove_flag_secure", false);
    }

    public static boolean getSaveEditedMessages() {
        return get("save_edited_messages", false);
    }

    public static boolean getSaveDeletedMessages() {
        return get("save_deleted_messages", false);
    }

    public static void setSaveDeletedMessages(boolean value) {
        set("save_deleted_messages", value);
    }

    public static void setSaveEditedMessages(boolean value) {
        set("save_edited_messages", value);
    }

    public static boolean getGhostModeEnabledGlobal() {
        return get("ghost_mode_enabled", true);
    }

    public static void setGhostModeEnabledGlobal(boolean value) {
        set("ghost_mode_enabled", value);
    }

    public static void setRemoveFlagSecure(boolean value) {
        set("remove_flag_secure", value);
    }

    public static boolean getUseSchedule() {
        return get("use_schedule", false);
    }

    public static void setUseSchedule(boolean value) {
        set("use_schedule", value);
    }

    public static void setHideOnline(boolean value) {
        set("hide_online", value);
    }

    public static void setHideTyping(boolean value) {
        set("hide_typing", value);
    }

    public static void setHideReading(boolean value) {
        set("hide_reading", value);
    }

    public static void setNoReadStories(boolean value) {
        set("no_read_stories", value);
    }

    public static boolean getFiltersEnabled() {
        return get("filters_enabled", false);
    }

    public static void setFiltersEnabled(boolean value) {
        set("filters_enabled", value);
    }

    public static boolean getUseExpandableBlockQuote() {
        return get("use_expandable_blockquote", false);
    }

    public static void setUseExpandableBlockQuote(boolean value) {
        set("use_expandable_blockquote", value);
    }

    public static boolean getAddGhostToDrawer() {
        return get("add_ghost_to_drawer", true);
    }

    public static void setAddGhostToDrawer(boolean value) {
        set("add_ghost_to_drawer", value);
    }

    public static void setShowSettingsInDrawer(boolean value) {
        set("show_settings_in_drawer", value);
    }

    public static boolean getShowSettingsInDrawer() {
        return get("show_settings_in_drawer", true);
    }

    public static void setLocalPremium(boolean value) {
        set("local_premium", value);
    }

    public static boolean getLocalPremium() {
        return get("local_premium", false);
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

    public static boolean getReadOnInteract() {
        return get("read_on_interact", false);
    }

    public static void setReadOnInteract(boolean value) {
        set("read_on_interact", value);
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

    public static int getSendSilence() {
        return get("send_silence", SendSilence.NO.getType());
    }

    public static void setSendSilence(SendSilence value) {
        set("send_silence", value.getType());
    }

    public static boolean getGhostInMainMenu() {
        return get("ghost_in_main_menu", false);
    }

    public static void setGhostInMainMenu(boolean value) {
        set("ghost_in_main_menu", value);
    }

    public static int getGhostMenuIndex() {
        return get("ghost_menu_index", 0);
    }

    public static void setGhostMenuIndex(int value) {
        set("ghost_menu_index", value);
    }

    public static boolean isGhostPositionInitialized() {
        return get("ghost_position_initialized", false);
    }

    public static void setGhostPositionInitialized(boolean value) {
        set("ghost_position_initialized", value);
    }
}
