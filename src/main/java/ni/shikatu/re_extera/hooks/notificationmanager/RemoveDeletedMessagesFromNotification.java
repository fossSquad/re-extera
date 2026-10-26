package ni.shikatu.re_extera.hooks.notificationmanager;

import androidx.collection.LongSparseArray;
import de.robv.android.xposed.XC_MethodHook;
import ni.shikatu.re_extera.db.ReExteraDb;

public class RemoveDeletedMessagesFromNotification extends XC_MethodHook {
    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
        boolean isReact = param.args.length > 1 && param.args[1] instanceof Boolean && ((Boolean) param.args[1]).booleanValue();
        if (isReact) {
            return;
        }
        Object arg0 = param.args[0];
        if (arg0 instanceof LongSparseArray) {
            LongSparseArray<?> map = (LongSparseArray<?>) arg0;
            for (int i = map.size() - 1; i >= 0; i--) {
                long dialogId = map.keyAt(i);
                if (ReExteraDb.get().shouldSaveDeletedMessages(dialogId)) {
                    map.removeAt(i);
                }
            }
            if (map.isEmpty()) {
                param.setResult((Object) null);
            }
        } else if (arg0 instanceof android.util.LongSparseArray) {
            android.util.LongSparseArray<?> map = (android.util.LongSparseArray<?>) arg0;
            for (int i = map.size() - 1; i >= 0; i--) {
                long dialogId = map.keyAt(i);
                if (ReExteraDb.get().shouldSaveDeletedMessages(dialogId)) {
                    map.removeAt(i);
                }
            }
            if (map.size() == 0) {
                param.setResult((Object) null);
            }
        }
    }
}
