package com.ai.cloud.skywalking.analysis.model;

import com.ai.cloud.skywalking.analysis.config.Config;
import com.ai.cloud.skywalking.analysis.config.Constants;
import com.ai.cloud.skywalking.analysis.util.HBaseUtil;
import com.google.gson.Gson;
import org.apache.hadoop.hbase.client.Put;

import java.io.IOException;
import java.util.*;

public class ChainRelate {
    private String key;
    private Map<String, CategorizedChainInfo> categorizedChainInfoMap = new HashMap<String, CategorizedChainInfo>();
    private List<UncategorizeChainInfo> uncategorizeChainInfoList = new ArrayList<UncategorizeChainInfo>();
    private Map<String, ChainDetail> chainDetailMap = new HashMap<String, ChainDetail>();

    public ChainRelate(String key) {
        this.key = key;
    }

    private void categoryUncategorizedChainInfo(CategorizedChainInfo parentChains) {
        if (uncategorizeChainInfoList != null && uncategorizeChainInfoList.size() > 0) {
            Iterator<UncategorizeChainInfo> uncategorizeChainInfoIterator = uncategorizeChainInfoList.iterator();
            while (uncategorizeChainInfoIterator.hasNext()) {
                UncategorizeChainInfo uncategorizeChainInfo = uncategorizeChainInfoIterator.next();
                if (parentChains.isContained(uncategorizeChainInfo)) {
                    parentChains.add(uncategorizeChainInfo);
                    uncategorizeChainInfoIterator.remove();
                }
            }
        }
    }

    private void classifiedChains(UncategorizeChainInfo child) {
        boolean isContained = false;
        for (Map.Entry<String, CategorizedChainInfo> entry : categorizedChainInfoMap.entrySet()) {
            if (entry.getValue().isContained(child)) {
                entry.getValue().add(child);
                isContained = true;


            }
        }

        if (!isContained) {
            uncategorizeChainInfoList.add(child);

            if (!uncategorizeChainInfoList.contains(child)){
                chainDetailMap.put(child.getChainToken(), new ChainDetail(child));
            }

        }

    }

    private CategorizedChainInfo addCategorizedChain(ChainInfo chainInfo) {
        if (!categorizedChainInfoMap.containsKey(chainInfo.getChainToken())) {
            categorizedChainInfoMap.put(chainInfo.getChainToken(),
                    new CategorizedChainInfo(chainInfo));

            chainDetailMap.put(chainInfo.getChainToken(), new ChainDetail(chainInfo));
        }
        return categorizedChainInfoMap.get(chainInfo.getChainToken());
    }

    public void addRelate(ChainInfo chainInfo) {
        if (chainInfo.getChainStatus() == ChainInfo.ChainStatus.NORMAL) {
            CategorizedChainInfo categorizedChainInfo = addCategorizedChain(chainInfo);
            categoryUncategorizedChainInfo(categorizedChainInfo);
        } else {
            UncategorizeChainInfo uncategorizeChainInfo = new UncategorizeChainInfo(chainInfo);
            classifiedChains(uncategorizeChainInfo);
        }
    }

    public void save() {
        Put put = new Put(getKey().getBytes());

        put.addColumn(Config.HBase.CHAIN_RELATIONSHIP_COLUMN_FAMILY.getBytes(), Constants.UNCATEGORIZED_QUALIFIER_NAME.getBytes()
                , new Gson().toJson(getUncategorizeChainInfoList()).getBytes());

        for (Map.Entry<String, CategorizedChainInfo> entry : getCategorizedChainInfoMap().entrySet()) {
            put.addColumn(Config.HBase.CHAIN_RELATIONSHIP_COLUMN_FAMILY.getBytes(), entry.getKey().getBytes()
                    , entry.getValue().toString().getBytes());
        }

        try {
            HBaseUtil.saveChainRelate(put);
        } catch (IOException e) {
            //TODO
            e.printStackTrace();
        }
    }

    public void addUncategorizeChain(UncategorizeChainInfo uncategorizeChainInfo) {
        uncategorizeChainInfoList.add(uncategorizeChainInfo);
    }

    public void addCategorizeChain(String qualifierName, CategorizedChainInfo categorizedChainInfo) {
        categorizedChainInfoMap.put(qualifierName, categorizedChainInfo);
    }

    public String getKey() {
        return key;
    }

    public Map<String, CategorizedChainInfo> getCategorizedChainInfoMap() {
        return categorizedChainInfoMap;
    }

    public List<UncategorizeChainInfo> getUncategorizeChainInfoList() {
        return uncategorizeChainInfoList;
    }

    public void addUncategorizeChain(List<UncategorizeChainInfo> uncategorizeChainInfos) {
        uncategorizeChainInfoList.addAll(uncategorizeChainInfos);
    }
}
