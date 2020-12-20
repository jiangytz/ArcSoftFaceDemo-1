package com.itboyst.facedemo.util;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author HardAlways
 * @date 2020/12/19 23:07
 */
public class HistoryRamCache {

    private static ConcurrentHashMap<String, List<History>> historyMap = new ConcurrentHashMap<>();

    //供用户注册完毕后调用，为每个用户添加一个记录签到历史的列表
    public static void addHistory(String id) {
        List<History> historyList = Lists.newLinkedList();
        historyMap.put(id, historyList);
    }

    //为某用户的签到历史添加一项
    public static void addHistoryItem(String id, History historyinfo) {
        List<History> historyList = historyMap.get(id);
        historyList.add(historyinfo);
        historyMap.put(id, historyList);
    }

    public static void removeHistory(String faceId) {
        historyMap.remove(faceId);
    }

    public static List<History> getHistoryList(String id) {
        return historyMap.get(id);
    }

    @Data
    public static class History {

        private String faceId;
        private String time;

        public History(String faceId, String time) {
            this.faceId = faceId;
            this.time = time;
        }
    }
}
