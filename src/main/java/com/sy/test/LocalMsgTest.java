package com.sy.test;

import com.sy.localMsg.annotiation.LocalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LocalMsgTest {

    @LocalMessage(bizCode = "test-sy")
    public void fun(String msg){
        if(System.currentTimeMillis()%2 != 0){
            throw new RuntimeException("test error!");
        }
        log.info("收到消息:{}",msg);
    }
}
