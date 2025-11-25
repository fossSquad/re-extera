package ni.shikatu.re_extera.utils;

import ni.shikatu.re_extera.settings.Settings;
import org.telegram.tgnet.TLRPC;

public class TextUtils {
    public static TLRPC.TL_messageEntityBlockquote createNewBlockQuote(String text) {
        TLRPC.TL_messageEntityBlockquote quote = new TLRPC.TL_messageEntityBlockquote();
        quote.offset = 0;
        quote.collapsed = Settings.getUseExpandableBlockQuote();
        quote.length = text.length();
        return quote;
    }

    public static TLRPC.TL_inputMessageEntityMentionName createNewMentionQuote(TLRPC.User user, int length) {
        TLRPC.TL_inputMessageEntityMentionName userment = new TLRPC.TL_inputMessageEntityMentionName();
        TLRPC.TL_inputUser tL_inputUser = new TLRPC.TL_inputUser();
        ((TLRPC.InputUser) tL_inputUser).user_id = user.id;
        ((TLRPC.InputUser) tL_inputUser).access_hash = user.access_hash;
        userment.user_id = tL_inputUser;
        userment.offset = 0;
        userment.length = length;
        return userment;
    }
}
