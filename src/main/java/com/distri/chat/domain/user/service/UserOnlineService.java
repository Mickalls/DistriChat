package com.distri.chat.domain.user.service;

import com.distri.chat.infrastructure.websocket.WebSocketConnectionRegistry;
import com.distri.chat.shared.enums.ChatType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserOnlineService {

    private final WebSocketConnectionRegistry wsConnectCenter;

    public UserOnlineService(WebSocketConnectionRegistry wsConnectCenter) {
        this.wsConnectCenter = wsConnectCenter;
    }

    public Map<String, Boolean> getReceiverOnlineStatus(String receiverId, ChatType chatType) {
        List<String> receiverIds = List.of(receiverId);
        if (chatType == ChatType.GROUP) {
            // todo: 群聊场景替换为群成员user_id
        }
        return wsConnectCenter.batchGetUserOnlineStatus(receiverIds);
    }

}
