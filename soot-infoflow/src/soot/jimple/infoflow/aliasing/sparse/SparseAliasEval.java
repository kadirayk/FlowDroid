package soot.jimple.infoflow.aliasing.sparse;

import boomerang.scene.sparse.SparseCFGCache;
import boomerang.scene.sparse.eval.PropagationCounter;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;
import soot.jimple.infoflow.results.InfoflowPerformanceData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * to create evaluation data
 * targetProgram, sparse mode, sparseCFG build time, #cache hit, #cache miss, total query time, #total propagation
 */
public class SparseAliasEval {

    private static final String OUT_PUT_DIR = "./results";
    private static final String FILE = "alias_eval.csv";
    public static String targetProgram;

    private final SparseCFGCache.SparsificationStrategy sparsificationStrategy;
    private long sparseCFGBuildTime=0;
    private long sparseCFGNumberTime=0;
    private long sparseCFGFindTime=0;
    private long sparseCFGSparsifyTime=0;
    private long cacheHitCount=0;
    private long cacheMissCount=0;
    private long totalAliasQueryTime=0;
    private long totalAliasQueryPropagationCount =0;
    private long aliasQueryCount = 0;
    private long F1Count = 0;
    private long F2Count = 0;
    private long B1Count = 0;
    private long B2Count = 0;
    private long B3Count = 0;
    private long B4Count = 0;
    private InfoflowPerformanceData performanceData;
    private int leaks;
    private int maxContainerTypeCount = 0;

    public SparseAliasEval(SparseCFGCache.SparsificationStrategy sparsificationStrategy, InfoflowPerformanceData performanceData, int leaks) {
        this.sparsificationStrategy = sparsificationStrategy;
        this.performanceData = performanceData;
        this.leaks = leaks;
        handleSparseCacheData();
        handlePropagationData();
        handleAliasQueryTime();
        this.aliasQueryCount = SparseAliasManager.getInstance(sparsificationStrategy).getQueryCount();
    }

    private void handleSparseCacheData() {
        if(sparsificationStrategy!= SparseCFGCache.SparsificationStrategy.NONE){
            SparseCFGCache cache = SparseCFGCache.getInstance(sparsificationStrategy, true);
            List<SparseCFGQueryLog> queryLogs = cache.getQueryLogs();
            for (SparseCFGQueryLog queryLog : queryLogs) {
                sparseCFGBuildTime += queryLog.getDuration().toMillis();
                sparseCFGNumberTime += queryLog.getCFGNumberDuration().toMillis();
                sparseCFGFindTime += queryLog.getFindStmtsDuration().toMillis();
                sparseCFGSparsifyTime += queryLog.getSparsifyDuration().toMillis();
                if(queryLog.getContainerTypeCount()> maxContainerTypeCount){
                    maxContainerTypeCount = queryLog.getContainerTypeCount();
                }
                if (queryLog.isRetrievedFromCache()) {
                    cacheHitCount++;
                } else {
                    cacheMissCount++;
                }
                switch (queryLog.getCacheAccessType()){
                    case F1:
                        F1Count++;
                        break;
                    case F2:
                        F2Count++;
                        break;
                    case B1:
                        B1Count++;
                        break;
                    case B2:
                        B2Count++;
                        break;
                    case B3:
                        B3Count++;
                        break;
                    case B4:
                        B4Count++;
                        break;
                }
            }
        }
    }

    private void handlePropagationData() {
        PropagationCounter counter = PropagationCounter.getInstance(sparsificationStrategy);
        long fwd = counter.getForwardPropagation();
        long bwd = counter.getBackwardPropagation();
        totalAliasQueryPropagationCount = fwd + bwd;
    }

    private void handleAliasQueryTime() {
        Duration totalDuration = SparseAliasManager.getInstance(sparsificationStrategy).getTotalDuration();
        if(totalDuration!=null){
            totalAliasQueryTime = SparseAliasManager.getInstance(sparsificationStrategy).getTotalDuration().toMillis();
        }else{
            totalAliasQueryTime = 0;
        }
    }


    public void generate() {
        File dir = new File(OUT_PUT_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(OUT_PUT_DIR + File.separator + FILE);
        if(!file.exists()){
            try (FileWriter writer = new FileWriter(file)) {
                StringBuilder str = new StringBuilder();
                str.append("targetProgram");
                str.append(",");
                str.append("sparsificationStrategy");
                str.append(",");
                str.append("totalAliasQueryTime");
                str.append(",");
                str.append("sparseCFGBuildTime");
                str.append(",");
                str.append("totalAliasQueryPropagationCount");
                str.append(",");
                str.append("cacheHitCount");
                str.append(",");
                str.append("cacheMissCount");
                str.append(",");
                str.append("totalAnalysisTime");
                str.append(",");
                str.append("maxMemory");
                str.append(",");
                str.append("sourceCount");
                str.append(",");
                str.append("sinkCount");
                str.append(",");
                str.append("edgeCount");
                str.append(",");
                str.append("sparseCFGNumber");
                str.append(",");
                str.append("sparseCFGFind");
                str.append(",");
                str.append("sparseCFGSparsify");
                str.append(",");
                str.append("aliasQueryCount");
                str.append(",");
                str.append("leaks");
                str.append(",");
                str.append("F1");
                str.append(",");
                str.append("F2");
                str.append(",");
                str.append("B1");
                str.append(",");
                str.append("B2");
                str.append(",");
                str.append("B3");
                str.append(",");
                str.append("B4");
                str.append(",");
                str.append("maxContainerTypeCount");
                str.append(System.lineSeparator());
                writer.write(str.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileWriter writer = new FileWriter(file, true)) {
            StringBuilder str = new StringBuilder();
            str.append(targetProgram);
            str.append(",");
            str.append(sparsificationStrategy);
            str.append(",");
            str.append(totalAliasQueryTime);
            str.append(",");
            str.append(sparseCFGBuildTime);
            str.append(",");
            str.append(totalAliasQueryPropagationCount);
            str.append(",");
            str.append(cacheHitCount);
            str.append(",");
            str.append(cacheMissCount);
            str.append(",");
            str.append(performanceData.getTotalRuntimeSeconds());
            str.append(",");
            str.append(performanceData.getMaxMemoryConsumption());
            str.append(",");
            str.append(performanceData.getSourceCount());
            str.append(",");
            str.append(performanceData.getSinkCount());
            str.append(",");
            str.append(performanceData.getEdgePropagationCount());
            str.append(",");
            str.append(sparseCFGNumberTime);
            str.append(",");
            str.append(sparseCFGFindTime);
            str.append(",");
            str.append(sparseCFGSparsifyTime);
            str.append(",");
            str.append(aliasQueryCount);
            str.append(",");
            str.append(leaks);
            str.append(",");
            str.append(F1Count);
            str.append(",");
            str.append(F2Count);
            str.append(",");
            str.append(B1Count);
            str.append(",");
            str.append(B2Count);
            str.append(",");
            str.append(B3Count);
            str.append(",");
            str.append(B4Count);
            str.append(",");
            str.append(maxContainerTypeCount);
            str.append(System.lineSeparator());
            writer.write(str.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
