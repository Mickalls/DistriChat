package com.distri.chat.domain.message.service;

import com.distri.chat.api.message.dto.SendMsgRequest;
import org.springframework.stereotype.Service;

@Service
public class PrivateChatService {
    public PrivateChatService() {
    }

    public void sendMsg(SendMsgRequest sendMsgRequest) {
        // 1. 如果接受方在线就即使推送

        // 2. 存储离线消息
    }


}
