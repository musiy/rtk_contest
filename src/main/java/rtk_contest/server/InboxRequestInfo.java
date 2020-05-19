package rtk_contest.server;

import com.google.protobuf.ByteString;

/**
 * Содержит входящее сообщение с ключём и значением
 */
class InboxRequestInfo {
    String key;
    ByteString value;

    public InboxRequestInfo(String key, ByteString value) {
        this.key = key;
        this.value = value;
    }
}
