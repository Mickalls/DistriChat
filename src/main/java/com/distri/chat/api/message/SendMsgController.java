package com.distri.chat.api.message;

import com.distri.chat.api.message.dto.SendMsgRequest;
import com.distri.chat.api.message.dto.SendMsgResponse;
import com.distri.chat.domain.message.service.GroupChatService;
import com.distri.chat.domain.message.service.PrivateChatService;
import com.distri.chat.domain.user.service.UserOnlineService;
import com.distri.chat.shared.dto.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/message")
@Tag(name = "消息发送", description = "消息发送相关接口")
public class SendMsgController {
    private final PrivateChatService privateChatService;
    private final GroupChatService groupChatService;
    private final UserOnlineService userOnlineService;

    public SendMsgController(PrivateChatService privateChatService, GroupChatService groupChatService, UserOnlineService userOnlineService) {
        this.privateChatService = privateChatService;
        this.groupChatService = groupChatService;
        this.userOnlineService = userOnlineService;
    }

    @PostMapping("/send")
    public Result<SendMsgResponse> sendMsg(@RequestBody SendMsgRequest sendMsgRequest) {
        // todo: 参数校验
        // todo: 根据关系领域, 判断是否为合法关系(好友/群成员/...)
        // todo: 根据关系领域返回会话Id

        // LoadReceiverOnlineStatus
        Map<String, Boolean> receiverOnlineStatus = userOnlineService.getReceiverOnlineStatus(sendMsgRequest.getToUserId(), sendMsgRequest.getChatType());

        // 根据不同对话类型分流
        switch (sendMsgRequest.getChatType()) {
            // 私聊
            case PRIVATE:
                privateChatService.sendMsg(sendMsgRequest, receiverOnlineStatus);
                break;
            // 群聊
            case GROUP:
                groupChatService.sendMsg(sendMsgRequest, receiverOnlineStatus);
                break;
        }

        return Result.success();
    }
}
