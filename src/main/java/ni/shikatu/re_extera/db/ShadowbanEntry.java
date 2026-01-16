package ni.shikatu.re_extera.db;

public class ShadowbanEntry {
    public final long addedTs;
    public final boolean hideDialog;
    public final boolean hideInGroups;
    public final long userId;

    public ShadowbanEntry(long userId, boolean hideDialog, boolean hideInGroups, long addedTs) {
        this.userId = userId;
        this.hideDialog = hideDialog;
        this.hideInGroups = hideInGroups;
        this.addedTs = addedTs;
    }
}
