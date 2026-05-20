package ni.shikatu.re_extera.utils;

import ni.shikatu.re_extera.settings.Settings;
import org.telegram.tgnet.TLRPC;

public final class TextUtils {
    private TextUtils() {
    }

    public static TLRPC.TL_messageEntityBlockquote createNewBlockQuote(String text) {
        TLRPC.TL_messageEntityBlockquote quote = new TLRPC.TL_messageEntityBlockquote();
        quote.offset = 0;
        quote.collapsed = Settings.getUseExpandableBlockQuote();
        quote.length = text.length();
        return quote;
    }

    public static TLRPC.TL_inputMessageEntityMentionName createNewMentionQuote(TLRPC.User user, int length) {
        TLRPC.TL_inputMessageEntityMentionName mention = new TLRPC.TL_inputMessageEntityMentionName();
        TLRPC.TL_inputUser tL_inputUser = new TLRPC.TL_inputUser();
        ((TLRPC.InputUser) tL_inputUser).user_id = user.id;
        ((TLRPC.InputUser) tL_inputUser).access_hash = user.access_hash;
        mention.user_id = tL_inputUser;
        mention.offset = 0;
        mention.length = length;
        return mention;
    }

    public static TLRPC.TL_messageEntityTextUrl createNewTextUrlQuote(String url, int length) {
        TLRPC.TL_messageEntityTextUrl entity = new TLRPC.TL_messageEntityTextUrl();
        entity.url = url;
        entity.offset = 0;
        entity.length = length;
        return entity;
    }
}
