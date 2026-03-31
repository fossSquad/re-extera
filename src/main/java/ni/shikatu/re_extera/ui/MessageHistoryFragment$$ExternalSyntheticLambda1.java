package ni.shikatu.re_extera.ui;

/* JADX INFO: compiled from: D8$$SyntheticClass */
public final /* synthetic */ class MessageHistoryFragment$$ExternalSyntheticLambda1 implements Runnable {
    public final /* synthetic */ MessageHistoryFragment f$0;

    public /* synthetic */ MessageHistoryFragment$$ExternalSyntheticLambda1(MessageHistoryFragment messageHistoryFragment) {
        this.f$0 = messageHistoryFragment;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.reloadMessages();
    }
}
