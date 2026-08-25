package ni.shikatu.re_extera.hooks.translate;

/**
 * Thread-local scope used to spoof {@code UserConfig.isPremium()} ONLY while
 * {@code ChatActivity.updateTopPanel(boolean)} is running.
 *
 * <p>exteraGram's {@code updateTopPanel} decides whether to show the in-chat
 * "Translate to English" bar with an inline {@code getUserConfig().isPremium()}
 * check (a non-Premium DM has {@code currentChat == null}, so it is skipped). We
 * cannot intercept that inline field read directly, so we hook the surrounding
 * method to raise this flag for the duration of the call and let the existing
 * {@code UserConfig.isPremium()} hook read it. This keeps the premium spoof
 * narrowly scoped to the translate-bar decision instead of faking Premium
 * app-wide (which would collide with the Local Premium feature).
 */
public final class TranslateScope {

    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    private TranslateScope() {
    }

    public static void enter() {
        ACTIVE.set(Boolean.TRUE);
    }

    public static void exit() {
        ACTIVE.remove();
    }

    public static boolean isActive() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }
}
