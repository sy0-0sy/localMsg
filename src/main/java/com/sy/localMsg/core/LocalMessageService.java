package com.sy.localMsg.core;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.sy.localMsg.repo.LocalMessagePO;
import com.sy.localMsg.repo.mapper.LocalMsgMapper;
import com.sy.localMsg.taskStatus.TaskStatus;
import com.sy.localMsg.util.LocalMessageUtil;
import com.sy.localMsg.util.TxOps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LocalMessageService implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    private final ExecutorService localMsgThreadPool = Executors.newFixedThreadPool(10);
    @Autowired
    private LocalMsgMapper localMsgMapper;
    public void invoke(LocalMessagePO localMessagePO, boolean async) {
        save(localMessagePO);
        boolean isInTx = TransactionSynchronizationManager.isActualTransactionActive();
        if (isInTx) {
            //TODO 在事务中,等待事务提交后, 回调触发被切方法 Why?
            TxOps.doAfterTxCompletion(() -> execute(localMessagePO, async));
        } else {
            //非事务中立即执行
            execute(localMessagePO, async);
        }

    }

    private void execute(LocalMessagePO localMessagePO, boolean async) {
        if (async) {
            doInvokeAsync(localMessagePO);
        } else {
            doInvoke(localMessagePO);
        }
    }

    private void doInvokeAsync(LocalMessagePO localMessagePO) {
        localMsgThreadPool.execute(() -> doInvoke(localMessagePO));
    }

    private void doInvoke(LocalMessagePO lmp) {
        String snapshot = lmp.getReqSnapshot();
        if (StrUtil.isBlank(snapshot)) {
            log.warn("Request snapshot is blank, recordId: {}", lmp.getId());
            invokeFail(lmp, "Request snapshot is blank");
            return;
        }
        InvokeCtx ctx = JSON.parseObject(snapshot, InvokeCtx.class);

        try {
            InvokeStatusHolder.startInvoke();
            Object[] parseParam = JSON.parseObject(ctx.getParamTypes(), Object[].class);
            List<Class<?>> parseParamType = getParamType(JSON.parseObject(ctx.getParamTypes(), List.class));
            Class<?> target = Class.forName(ctx.getClassName());
            Method method = ReflectUtil.getMethod(target,ctx.getMethodName(),parseParamType.toArray(new Class[0]));
            //从容器中拿到执行 任务的 Bean
            Object bean = applicationContext.getBean(target);
            
            method.invoke(bean,parseParam);
            //执行成功
            invokeSuccess(lmp);
        }catch (ClassNotFoundException e) {
            //这种错误 重试解决不了
            log.error("Class not found for invocation, className: {}, recordId: {}", ctx.getClassName(), lmp.getId(), e);
            invokeFail(lmp, e.getMessage());
        } catch (IllegalArgumentException e) {
            //这种错误 重试解决不了
            log.error("argument illegal for invocation, methodName: {}, recordId: {}", ctx.getMethodName(), lmp.getId(), e);
            invokeFail(lmp, e.getMessage());
        }catch (InvocationTargetException e) {
            // 这里拿到原始异常
            Throwable cause = e.getCause();
            String errorMsg = cause != null ? cause.getMessage() : "Unknown error";
            log.error("Invocation failed: {}.{}, recordId: {}, cause: {}",
                    ctx.getClassName(), ctx.getMethodName(), lmp.getId(), errorMsg, cause);
            retry(lmp, errorMsg);
        }catch (Throwable e) {
            //其他错误尝试重试
            log.error("Invocation failed, recordId: {}, error: {}", lmp.getId(), e.getMessage(), e);
            retry(lmp, e.getMessage());
        }finally{
            InvokeStatusHolder.endInvoke();
        }
    }

    private void invokeSuccess(LocalMessagePO lmp) {
        lmp.setStatus(TaskStatus.SUCCESS.name());
        localMsgMapper.updateById(lmp);
    }

    private void retry(LocalMessagePO lmp, String message) {
        //重试次数加一
        int retryTimes = lmp.getRetryTimes()+1;
        LocalMessagePO updateDO = new LocalMessagePO();
        updateDO.setId(lmp.getId());
        updateDO.setFailReason(message);
        //计算下次重试时间
        updateDO.setNextRetryTime(getNextRetryTime(retryTimes));

        if(retryTimes >= lmp.getMaxRetryTimes()){
            //最大重试时间已经耗尽
            updateDO.setStatus(TaskStatus.FAIL.name());
        }else{
            //设置重试时间/状态 等待定时任务调度
            updateDO.setRetryTimes(retryTimes);
            updateDO.setStatus(TaskStatus.RETRY.name());
        }
        localMsgMapper.updateById(updateDO);
    }

    /**
     * 计算下次重试时间, 为RETRY_INTERVAL_MINUTES的retryTimes次方
     * @param retryTimes
     * @return
     */
    private Long getNextRetryTime(int retryTimes) {
        double waitMin = Math.pow(Constant.RETRY_INTERVAL_MINUTES, retryTimes);
        return LocalMessageUtil.offsetTimestamp((long)waitMin);
    }

    private List<Class<?>> getParamType(List<String> params) {
        //解析全类名
        return params.stream().map(name->{
            try{
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                log.error("未找到类:{}",name);
                throw new IllegalArgumentException("未找到类:"+name,e);
            }
        }).collect(Collectors.toList());
    }

    private void save(LocalMessagePO localMessagePO) {
        localMsgMapper.save(localMessagePO);
    }

    private void invokeFail(LocalMessagePO localMessagePO, String failMsg) {
        localMessagePO.setStatus(TaskStatus.FAIL.name());
        localMessagePO.setFailReason(failMsg);
        localMsgMapper.updateById(localMessagePO);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext  = applicationContext;
    }

    @Scheduled(initialDelay = Constant.INIT_DELAY_TIME,fixedRate = Constant.RETRY_TASK_INTERVAL)
    public void compensation(){
        log.info("本地消息表 定时任务 补偿开始!");
        loadWaitRetryRecords().forEach(this::doInvokeAsync);
    }

    private static final List<String> needRetryTaskStatus =new ArrayList<>(){{
        add(TaskStatus.INIT.name());
        add(TaskStatus.RETRY.name());
    }};
    private List<LocalMessagePO> loadWaitRetryRecords() {
        // 查询需要重试的任务
        return localMsgMapper.loadWaitRetryRecords(
                needRetryTaskStatus,
                System.currentTimeMillis(),
                Constant.RETRY_INTERVAL_MINUTES
        );
    }
}
