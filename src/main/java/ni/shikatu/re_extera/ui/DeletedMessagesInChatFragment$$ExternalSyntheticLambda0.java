package ni.shikatu.re_extera.ui;

/* JADX INFO: compiled from: D8$$SyntheticClass */
public final /* synthetic */ class DeletedMessagesInChatFragment$$ExternalSyntheticLambda0 implements Runnable {
    public final /* synthetic */ DeletedMessagesInChatFragment f$0;

    public /* synthetic */ DeletedMessagesInChatFragment$$ExternalSyntheticLambda0(DeletedMessagesInChatFragment deletedMessagesInChatFragment) {
        this.f$0 = deletedMessagesInChatFragment;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.reloadMessages();
    }
}
