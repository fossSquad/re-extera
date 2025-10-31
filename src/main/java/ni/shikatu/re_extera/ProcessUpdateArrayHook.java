package ni.shikatu.re_extera;

import android.util.SparseArray;
import androidx.collection.LongSparseArray;
import de.robv.android.xposed.XC_MethodHook;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

public class ProcessUpdateArrayHook extends XC_MethodHook {
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        ArrayList<TLRPC.Update> updates;
        MessagesController mc;
        SparseArray<MessageObject> byIds;
        LongSparseArray<ArrayList<MessageObject>> dialogMessage;
        ArrayList<MessageObject> list;
        ArrayList<TLRPC.Update> updates2 = (ArrayList) param.args[0];
        if (updates2 != null && !updates2.isEmpty()) {
            MessagesController mc2 = (MessagesController) param.thisObject;
            SparseArray<MessageObject> byIds2 = mc2.dialogMessagesByIds;
            LongSparseArray<ArrayList<MessageObject>> dialogMessage2 = mc2.dialogMessage;
            ArrayList<TLRPC.Update> filtered = new ArrayList<>(updates2.size());
            LongSparseArray<ArrayList<Integer>> channelDeleted = new LongSparseArray<>();
            System.currentTimeMillis();
            for (TLRPC.Update update : updates2) {
                if (update instanceof TLRPC.TL_updateDeleteMessages) {
                    TLRPC.TL_updateDeleteMessages del = (TLRPC.TL_updateDeleteMessages) update;
                    if (byIds2 != null && del.messages != null) {
                        Iterator it = del.messages.iterator();
                        while (it.hasNext()) {
                            int id = ((Integer) it.next()).intValue();
                            MessageObject mo = byIds2.get(id);
                            if (mo != null) {
                                updates = updates2;
                                long did = mo.getDialogId();
                                mc = mc2;
                                byIds = byIds2;
                                Main.cachedDeleted.add(did + "_" + mo.getId());
                                try {
                                    DbDeletedStore.get().put(did, mo.getId());
                                } catch (Throwable th) {
                                }
                            } else {
                                updates = updates2;
                                mc = mc2;
                                byIds = byIds2;
                            }
                            updates2 = updates;
                            mc2 = mc;
                            byIds2 = byIds;
                        }
                    }
                } else {
                    ArrayList<TLRPC.Update> updates3 = updates2;
                    MessagesController mc3 = mc2;
                    SparseArray<MessageObject> byIds3 = byIds2;
                    if (update instanceof TLRPC.TL_updateDeleteChannelMessages) {
                        TLRPC.TL_updateDeleteChannelMessages delc = (TLRPC.TL_updateDeleteChannelMessages) update;
                        if (delc.messages == null) {
                            updates2 = updates3;
                            mc2 = mc3;
                            byIds2 = byIds3;
                        } else if (delc.messages.isEmpty()) {
                            updates2 = updates3;
                            mc2 = mc3;
                            byIds2 = byIds3;
                        } else {
                            long did2 = -delc.channel_id;
                            ArrayList<MessageObject> list2 = dialogMessage2 != null ? (ArrayList) dialogMessage2.get(did2) : null;
                            if (list2 == null || list2.isEmpty()) {
                                dialogMessage = dialogMessage2;
                            } else {
                                HashSet<Integer> idsSet = new HashSet<>(delc.messages);
                                for (MessageObject mo2 : list2) {
                                    if (mo2 != null) {
                                        list = list2;
                                        if (idsSet.contains(Integer.valueOf(mo2.getId()))) {
                                            Main.cachedDeleted.add(did2 + "_" + mo2.getId());
                                        }
                                    } else {
                                        list = list2;
                                    }
                                    idsSet = idsSet;
                                    list2 = list;
                                    dialogMessage2 = dialogMessage2;
                                }
                                dialogMessage = dialogMessage2;
                            }
                            ArrayList<Integer> acc = (ArrayList) channelDeleted.get(did2);
                            if (acc == null) {
                                acc = new ArrayList<>();
                                channelDeleted.put(did2, acc);
                            }
                            acc.addAll(delc.messages);
                            updates2 = updates3;
                            mc2 = mc3;
                            byIds2 = byIds3;
                            dialogMessage2 = dialogMessage;
                        }
                    } else {
                        filtered.add(update);
                        updates2 = updates3;
                        mc2 = mc3;
                        byIds2 = byIds3;
                    }
                }
            }
            if (channelDeleted.size() > 0) {
                for (int i = 0; i < channelDeleted.size(); i++) {
                    long did3 = channelDeleted.keyAt(i);
                    ArrayList<Integer> ids = (ArrayList) channelDeleted.valueAt(i);
                    if (ids != null && !ids.isEmpty()) {
                        try {
                            DbDeletedStore.get().batchPut(did3, ids);
                        } catch (Throwable th2) {
                        }
                    }
                }
            }
            ((ArrayList) param.args[0]).clear();
            ((ArrayList) param.args[0]).addAll(filtered);
        }
    }
}
