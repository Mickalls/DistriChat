package com.distri.chat.api.message.dto;

import com.distri.chat.shared.enums.ChatType;
import lombok.Data;

@Data
public class SendMsgRequest {
    // 会话类型: 私聊 / 群聊
    private ChatType chatType;

    // 发送方user_id
    private String fromUserId;

    // 接受方user_id / 群聊group_id
    private String toUserId;

    // 消息内容 (json格式)
    private String content;
}
