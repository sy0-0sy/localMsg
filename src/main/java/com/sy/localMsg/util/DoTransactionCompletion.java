package com.sy.localMsg.util;

import org.springframework.transaction.support.TransactionSynchronization;

public class DoTransactionCompletion implements TransactionSynchronization {
    private final Runnable action;

    public DoTransactionCompletion(Runnable action) {
        this.action = action;
    }

    @Override
    public void afterCompletion(int status) {
        if (TransactionSynchronization.STATUS_COMMITTED != status) {
            return;
        }
        // 只有事务提交了，才执行操作
        action.run();
    }
}
