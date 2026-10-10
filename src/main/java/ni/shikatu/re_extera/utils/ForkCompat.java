package ni.shikatu.re_extera.utils;

/**
 * Runtime detection of the host client.
 *
 * The DEX is loaded through a parent-first class loader, so on exteraGram the real
 * extera SDK classes win over the shims bundled in this DEX. Their presence is
 * therefore a reliable marker of the exteraGram fork.
 */
public final class ForkCompat {

    private static Boolean exteraGram;

    private ForkCompat() {
    }

    public static boolean isExteraGram() {
        Boolean cached = exteraGram;
        if (cached != null) {
            return cached;
        }
        boolean result;
        try {
            Class.forName("com.exteragram.messenger.ExteraConfig");
            result = true;
        } catch (Throwable t) {
            result = false;
        }
        exteraGram = result;
        return result;
    }
}
