package com.distri.chat.shared.dto;

import com.distri.chat.shared.enums.ChatType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageDto {
    // 发送方 uid
    String fromUserId;
    // 接受方 uid
    String toUserId;
    // 会话 id
    String sessionId;
    // 会话内容
    String content;
    // 时序 id (用户维度递增 用户排序)
    String sequenceId;
    // 消息 id (全局唯一 用于去重)
    String messageId;
    // 会话类型
    ChatType chatType;
}
