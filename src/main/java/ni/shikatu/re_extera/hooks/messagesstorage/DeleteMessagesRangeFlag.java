package ni.shikatu.re_extera.hooks.messagesstorage;

public final class DeleteMessagesRangeFlag {
    private DeleteMessagesRangeFlag() {}

    static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);
}
