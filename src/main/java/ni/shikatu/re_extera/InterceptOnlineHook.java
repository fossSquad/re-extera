package ni.shikatu.re_extera;

import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

import de.robv.android.xposed.XC_MethodHook;

public class InterceptOnlineHook extends XC_MethodHook{

	private final static TL_account.updateStatus offlineStatus = new TL_account.updateStatus();
	InterceptOnlineHook(){
		offlineStatus.offline = true;
	}
	@Override
	protected void beforeHookedMethod(MethodHookParam param) {
		TLObject obj = (TLObject) param.args[0];
		if(obj instanceof TLRPC.TL_updateUserStatus && Settings.getHideOnline()){
			Global.log("Intercept user status");
			param.setResult(null);
			return;
		}
		if(Global.readingRequests.contains(obj.getClass()) && Settings.getHideReading()) {
			Global.log("Intercept read");
			param.setResult(null);
			return;
		}
		if(Global.typingRequests.contains(obj.getClass()) && Settings.getHideTyping()) {
			Global.log("Intercept typing");
			param.setResult(null);

			return;
		}
		if(Global.storiesRequests.contains(obj.getClass()) && Settings.getNoReadStories()) {
			Global.log("Intercept stories");
			param.setResult(null);
			return;
		}
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) {
		Global.log("sending offline");
		if(Global.onlineRequests.contains(param.args[0].getClass())) {
			ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(
					offlineStatus, (unused, unused2) -> {}
			);
		}
	}
}
