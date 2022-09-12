package soot.jimple.infoflow.aliasing.sparse;

import boomerang.scene.sparse.SparseCFGCache;
import boomerang.scene.sparse.eval.PropagationCounter;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;

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
    private long cacheHitCount=0;
    private long cacheMissCount=0;
    private long totalAliasQueryTime=0;
    private long totalPropagationCount=0;

    public SparseAliasEval(SparseCFGCache.SparsificationStrategy sparsificationStrategy) {
        this.sparsificationStrategy = sparsificationStrategy;
        handleSparseCacheData();
        handlePropagationData();
        handleAliasQueryTime();
    }

    private void handleSparseCacheData() {
        if(sparsificationStrategy!= SparseCFGCache.SparsificationStrategy.NONE){
            SparseCFGCache cache = SparseCFGCache.getInstance(sparsificationStrategy);
            List<SparseCFGQueryLog> queryLogs = cache.getQueryLogs();

            for (SparseCFGQueryLog queryLog : queryLogs) {
                sparseCFGBuildTime += queryLog.getDuration();
                if (queryLog.isRetrievedFromCache()) {
                    cacheHitCount++;
                } else {
                    cacheMissCount++;
                }
            }
            sparseCFGBuildTime /= 1000; //total in micro to millis
        }
    }

    private void handlePropagationData() {
        PropagationCounter counter = PropagationCounter.getInstance(sparsificationStrategy);
        long fwd = counter.getForwardPropagation();
        long bwd = counter.getBackwardPropagation();
        totalPropagationCount = fwd + bwd;
    }

    private void handleAliasQueryTime() {
        Duration totalDuration = SparseAliasManager.getTotalDuration();
        if(totalDuration!=null){
            totalAliasQueryTime = SparseAliasManager.getTotalDuration().toMillis();
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
                str.append("totalPropagationCount");
                str.append(",");
                str.append("cacheHitCount");
                str.append(",");
                str.append("cacheMissCount");
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
            str.append(totalPropagationCount);
            str.append(",");
            str.append(cacheHitCount);
            str.append(",");
            str.append(cacheMissCount);
            str.append(System.lineSeparator());
            writer.write(str.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
