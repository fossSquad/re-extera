package ni.shikatu.re_extera.db;

public class DialogExclusion {
    public final long dialogId;
    public final int readExclusion;
    public final int typeExclusion;

    public DialogExclusion(long dialogId, int readExclusion, int typeExclusion) {
        this.dialogId = dialogId;
        this.readExclusion = readExclusion;
        this.typeExclusion = typeExclusion;
    }

    public String toString() {
        return "DialogException{dialogId=" + this.dialogId + ", readExclusion=" + this.readExclusion + ", typeExclusion=" + this.typeExclusion + '}';
    }
}
