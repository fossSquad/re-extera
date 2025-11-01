package ni.shikatu.re_extera;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;

public class Localization{
	public static String HIDE_READING_MESSAGE;
	public static String HIDE_TYPING_STATUS;
	public static String HIDE_ONLINE_STATUS;
	public static String NO_READ_STORIES;

	public static String GENERAL;

	public static String THANKS;

	public static String RE_EXTERA_SETTINGS;
	public static String ENABLE_ALPHA;
	public static String ALPHA_WARNING;

	public static String SAVE_DELETED_MESSAGES;

	public static String MESSAGE_HISTORY;
	public static String MESSAGE_HISTORY_TOGGLE;
	public static String MESSAGE_HISTORY_TITLE;
	public static String REMOVE_FLAG_SECURE;

	public static String USE_SCHEDULE;

	public static String CLEAR_DB;

	public static String YES;
	public static String NO;

	public static String CUSTOM_PREFIX;


	public static String GHOST_MODE;
	public static String DELETED_AND_EDITED_MESSAGES;

	public static String OTHER;
	public static void updateStrings() {
		String currentLang = LocaleController.getInstance().getCurrentLocale().getLanguage();
		if ("ru".equals(currentLang)) {
			HIDE_READING_MESSAGE = "Не читать сообщения";
			HIDE_TYPING_STATUS = "Не показывать статус \"печатает\"";
			HIDE_ONLINE_STATUS = "Не показывать статус \"онлайн\"";
			NO_READ_STORIES = "Не читать истории";
			ENABLE_ALPHA = "Полупрозрачные удаленки";
			ALPHA_WARNING = "Эта функция кушает очень много";
			GENERAL = "Общие";
			THANKS = "Спасибо @bleizix";
			RE_EXTERA_SETTINGS = "Настройки re:extera";
			SAVE_DELETED_MESSAGES = "Сохранять удаленные сообщения";
			MESSAGE_HISTORY = "История";
			MESSAGE_HISTORY_TOGGLE = "Сохранить историю правок";
			MESSAGE_HISTORY_TITLE = "История правок";
			REMOVE_FLAG_SECURE = "Игнорировать FLAG_SECURE";
			USE_SCHEDULE = "Использовать отложку";
			CLEAR_DB = "Очистить БД";
			YES = "Да";
			NO = "Нет";
			CUSTOM_PREFIX = "Метка удаленок";
			GHOST_MODE = "Режим призрака";
			DELETED_AND_EDITED_MESSAGES = "Измененные и удаленные сообщения";
			OTHER = "Другое";

		} else {
			HIDE_READING_MESSAGE = "Don't read messages";
			HIDE_TYPING_STATUS = "Don't show \"typing\" status";
			HIDE_ONLINE_STATUS = "Don't show \"online\" status";
			NO_READ_STORIES = "Don't read stories";
			GENERAL = "General";
			THANKS = "Thanks to @bleizix";
			ENABLE_ALPHA = "Half opacity for deleted messages";
			ALPHA_WARNING = "This function eats a lot of cpu";
			RE_EXTERA_SETTINGS = "re:extera Settings";
			SAVE_DELETED_MESSAGES = "Save deleted messages";
			MESSAGE_HISTORY = "History";
			MESSAGE_HISTORY_TOGGLE = "Save message history";
			MESSAGE_HISTORY_TITLE = "Message history";
			REMOVE_FLAG_SECURE = "Ignore FLAG_SECURE";
			USE_SCHEDULE = "Use schedule to send";
			CLEAR_DB = "Clear DB";
			YES = "Yes";
			NO = "No";
			CUSTOM_PREFIX = "Deleted message mark";
			GHOST_MODE = "Ghost mode";
			DELETED_AND_EDITED_MESSAGES = "Edited and deleted messages";
			OTHER = "Other";
		}
	}
}
