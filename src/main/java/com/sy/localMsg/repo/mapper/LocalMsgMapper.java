package com.sy.localMsg.repo.mapper;

import com.sy.localMsg.repo.LocalMessagePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface LocalMsgMapper {


    void save(LocalMessagePO localMessagePO);


    void updateById(LocalMessagePO lmp);

    List<LocalMessagePO> loadWaitRetryRecords(List<String> status, Long nextRetryTime, int retryIntervalMinutes);
}
