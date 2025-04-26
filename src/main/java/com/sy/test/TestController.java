package com.sy.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TestController {
    @Autowired
    private LocalMsgTest localMsgTest;
    @GetMapping("/test")
    public String test(String msg){
        localMsgTest.fun(msg);
        return "ok";
    }

}
