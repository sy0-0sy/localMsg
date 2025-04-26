package com.sy.localMsg.core;

import com.alibaba.fastjson.JSON;
import com.sy.localMsg.annotiation.LocalMessage;
import com.sy.localMsg.repo.LocalMessagePO;
import com.sy.localMsg.util.LocalMessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;


@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class LocalMsgAspect {

    private final LocalMessageService localMessageService;

    @Around("@annotation(com.sy.localMsg.annotiation.LocalMessage)")
    public Object doAsp(ProceedingJoinPoint jp) throws Throwable{
        log.info("进入AOP");
        Method method = ((MethodSignature) jp.getSignature()).getMethod();
        LocalMessage localMessage = AnnotationUtils.findAnnotation(method, LocalMessage.class);
        //注解信息为空 执行原方法
        if(localMessage == null){
            return jp.proceed();
        }
        //已经在切面中了,就直接执行目标方法 不要再往下走了
        if(InvokeStatusHolder.inInvoke()){
            return jp.proceed();
        }
        boolean async = localMessage.async();
        String bizCode = localMessage.bizCode();

        //获取方法参数
        List<String> parmeterTypeList = Arrays.stream(method.getParameterTypes())
                .map(Class::getName).toList();

        //执行方法的上下文信息
        InvokeCtx ctx = InvokeCtx.builder()
                .className(method.getDeclaringClass().getName())
                .paramTypes(JSON.toJSONString(parmeterTypeList))
                .args(JSON.toJSONString(jp.getArgs()))
                .methodName(method.getName())
                .build();
        LocalMessagePO localMessagePO = new LocalMessagePO(
                bizCode,
                JSON.toJSONString(ctx),
                localMessage.maxRetryTimes(),
                LocalMessageUtil.offsetTimestamp(Constant.RETRY_INTERVAL_MINUTES)
        );
        log.info("本地消息表写入记录: {}, 是否异步:{}",JSON.toJSONString(localMessagePO),async);
        localMessageService.invoke(localMessagePO,async);
//        LocalMessagePO parsePo =  JSON.parseObject(JSON.toJSONString(localMessagePO),LocalMessagePO.class);
//        InvokeCtx parseCtx = JSON.parseObject(parsePo.getReqSnapshot(), InvokeCtx.class);
//        Object[] parseParam = JSON.parseObject(parseCtx.getParamTypes(),Object[].class);
//        List<String> parseParamType = JSON.parseObject(parseCtx.getParamTypes(),List.class);

        // 真正执行 不在这了 直接返回null
        return null;
    }

}
