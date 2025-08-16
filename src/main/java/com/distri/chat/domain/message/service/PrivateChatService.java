package com.distri.chat.domain.message.service;

import com.distri.chat.api.message.dto.SendMsgRequest;
import com.distri.chat.shared.dto.MessageDto;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class PrivateChatService {
    private final MessageService messageService;

    public PrivateChatService(MessageService messageService) {
        this.messageService = messageService;
    }

    public void sendMsg(SendMsgRequest sendMsgRequest, Map<String, Boolean> receiverOnlineStatus) {
        // 1. 如果接受方在线就即使推送
        for (Map.Entry<String, Boolean> entry : receiverOnlineStatus.entrySet()) {
            if (!entry.getValue()) {
                continue;
            }
            String userId = entry.getKey();
            MessageDto dto = MessageDto.builder()
                    .fromUserId(sendMsgRequest.getFromUserId())
                    .toUserId(userId)
                    .content(sendMsgRequest.getContent())
                    .chatType(sendMsgRequest.getChatType())
                    .sessionId("session_id_todo")
                    .sequenceId(messageService.generateMessageId(sendMsgRequest.getFromUserId()))
                    .messageId(UUID.randomUUID().toString())
                    .build();
            // 推送消息
            messageService.publishMsg(dto);
        }

        // 2. todo: 存储离线消息
    }
}
