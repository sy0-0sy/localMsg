package com.sy.localMsg.util;

import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TxOps {
    /**
     * 在当前事务执行成功之后执行 action
     *
     * @param action 需要执行的操作
     */
    public static void doAfterTxCompletion(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new DoTransactionCompletion(action));
        }
    }
}
