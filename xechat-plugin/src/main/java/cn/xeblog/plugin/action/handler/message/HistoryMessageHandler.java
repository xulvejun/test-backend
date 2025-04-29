package cn.xeblog.plugin.action.handler.message;

import cn.hutool.core.collection.CollectionUtil;
import cn.xeblog.commons.entity.HistoryMsgDTO;
import cn.xeblog.commons.entity.Response;
import cn.xeblog.commons.enums.MessageType;
import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.annotation.DoMessage;
import cn.xeblog.plugin.factory.MessageHandlerFactory;

import java.util.List;

/**
 * @author anlingyi
 * @date 2021/9/11 7:21 下午
 */
@DoMessage(MessageType.HISTORY_MSG)
public class HistoryMessageHandler extends AbstractMessageHandler<HistoryMsgDTO> {

    @Override
    protected void process(Response<HistoryMsgDTO> response) {
        List<Response> msgList = response.getBody().getMsgList();
        if (CollectionUtil.isNotEmpty(msgList)) {
            ConsoleAction.atomicExec(() -> {
                ConsoleAction.showSimpleMsg("正在加载历史消息...");
                msgList.forEach(msg -> MessageHandlerFactory.INSTANCE.produce(msg.getType()).handle(msg));
                ConsoleAction.showSimpleMsg("------以上是历史消息------");
            });
        }
    }

}
