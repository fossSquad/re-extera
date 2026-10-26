package ni.shikatu.re_extera.db;

public final class DialogExclusion {
    public final long dialogId;
    public final int readExclusion;
    public final int typeExclusion;
    public final int filterExclusion;
    public final int deletedExclusion;

    public DialogExclusion(long dialogId, int readExclusion, int typeExclusion, int filterExclusion, int deletedExclusion) {
        this.dialogId = dialogId;
        this.readExclusion = readExclusion;
        this.typeExclusion = typeExclusion;
        this.filterExclusion = filterExclusion;
        this.deletedExclusion = deletedExclusion;
    }

    public DialogExclusion(long dialogId, int readExclusion, int typeExclusion, int filterExclusion) {
        this(dialogId, readExclusion, typeExclusion, filterExclusion, 0);
    }

    public String toString() {
        return "DialogExclusion{dialogId=" + this.dialogId + ", readExclusion=" + this.readExclusion + ", typeExclusion=" + this.typeExclusion + ", filterExclusion=" + this.filterExclusion + ", deletedExclusion=" + this.deletedExclusion + '}';
    }
}
