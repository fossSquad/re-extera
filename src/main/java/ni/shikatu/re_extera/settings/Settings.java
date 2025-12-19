package ni.shikatu.re_extera.settings;

import ni.shikatu.re_extera.Main;

public class Settings {
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

    public static boolean getDisableAds() {
        return get("disable_ads", true);
    }

    public static void setDisableAds(boolean value) {
        set("disable_ads", value);
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

    public static boolean getDisableBlurOnOneTimeMessages() {
        return get("disable_blur", false);
    }

    public static void setDisableBlurOnOneTimeMessages(boolean value) {
        set("disable_blur", value);
    }

    public static String getCustomPrefix() {
        return get("custom_prefix", "️");
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

    public static boolean getEnableAlpha() {
        return get("enable_alpha", false);
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

    public static boolean getSaveOwnMessages() {
        return get("save_own_messages", true);
    }

    public static void setSaveOwnMessages(boolean value) {
        set("save_own_messages", value);
    }

    public static void setEnableAlpha(boolean value) {
        set("enable_alpha", value);
    }

    public static void setRemoveFlagSecure(boolean value) {
        set("remove_flag_secure", value);
    }

    public static void setSaveDeletedMessages() {
        set("save_deleted_messages", true);
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
}
