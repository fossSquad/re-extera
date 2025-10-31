package ni.shikatu.re_extera;

import android.content.Context;

import org.telegram.ui.LaunchActivity;

public class Settings {
	private static int get(String settingName, int defaultValue){
		return LaunchActivity.instance.getApplicationContext().getSharedPreferences("re_extera", Context.MODE_PRIVATE).getInt(settingName, defaultValue);
	}

	private static String get(String settingName, String defaultValue){
		return LaunchActivity.instance.getApplicationContext().getSharedPreferences("re_extera", Context.MODE_PRIVATE).getString(settingName, defaultValue);
	}

	private static boolean get(String settingName, boolean defaultValue){
		return LaunchActivity.instance.getApplicationContext().getSharedPreferences("re_extera", Context.MODE_PRIVATE).getBoolean(settingName, defaultValue);
	}

	private static void set(String settingName, int value){
		LaunchActivity.instance.getApplicationContext().getSharedPreferences("re_extera", Context.MODE_PRIVATE).edit().putInt(settingName, value).apply();
	}

	private static void set(String settingName, String value){
		LaunchActivity.instance.getApplicationContext().getSharedPreferences("re_extera", Context.MODE_PRIVATE).edit().putString(settingName, value).apply();
	}

	private static void set(String settingName, boolean value){
		LaunchActivity.instance.getApplicationContext().getSharedPreferences("re_extera", Context.MODE_PRIVATE).edit().putBoolean(settingName, value).apply();
	}
	public static boolean getHideOnline(){
		return get("hide_online", false);
	}
	public static boolean getHideTyping(){
		return get("hide_typing", false);
	}
	public static boolean getHideReading(){
		return get("hide_reading", false);
	}
	public static boolean getNoReadStories(){
		return get("no_read_stories", false);
	}
	public static boolean getRemoveFlagSecure(){
		return get("remove_flag_secure", false);
	}

	public static boolean getSaveEditedMessages(){
		return get("save_edited_messages", false);
	}

	public static boolean getEnableAlpha(){
		return get("enable_alpha", false);
	}

	public static boolean getSaveDeletedMessages(){
		return get("save_deleted_messages", false);
	}

	public static void setSaveDeletedMessages(boolean value){
		set("save_deleted_messages", value);
	}
	public static void setSaveEditedMessages(boolean value){
		set("save_edited_messages", value);
	}

	public static void setEnableAlpha(boolean value){
		set("enable_alpha", value);
	}
	public static void setRemoveFlagSecure(boolean value){ set("remove_flag_secure", value); }


	public static void setSaveDeletedMessages(){
		set("save_deleted_messages", true);
	}

	public static void setHideOnline(boolean value){
		set("hide_online", value);
	}
	public static void setHideTyping(boolean value){
		set("hide_typing", value);
	}
	public static void setHideReading(boolean value){
		set("hide_reading", value);
	}
	public static void setNoReadStories(boolean value){
		set("no_read_stories", value);
	}
}
