package soot.jimple.infoflow.aliasing.sparse;

import boomerang.scene.sparse.SparseCFGCache;
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
    private long totalAliasQueryTime=0;
    private long aliasQueryCount = 0;
    private InfoflowPerformanceData performanceData;
    private float initalStmtCount = 0;
    private float finalStmtCount = 0;

    public SparseAliasEval(SparseCFGCache.SparsificationStrategy sparsificationStrategy, InfoflowPerformanceData performanceData) {
        this.sparsificationStrategy = sparsificationStrategy;
        this.performanceData = performanceData;
        handleSparsificationSpecificData();
        handleAliasQueryTime();
        this.aliasQueryCount = SparseAliasManager.getInstance(sparsificationStrategy).getQueryCount();
    }

    private void handleSparsificationSpecificData() {
        if(sparsificationStrategy!= SparseCFGCache.SparsificationStrategy.NONE){
            SparseCFGCache cache = SparseCFGCache.getInstance(sparsificationStrategy, true);
            List<SparseCFGQueryLog> queryLogs = cache.getQueryLogs();
            for (SparseCFGQueryLog queryLog : queryLogs) {
                sparseCFGBuildTime += queryLog.getDuration().toMillis();
                initalStmtCount += queryLog.getInitialStmtCount();
                finalStmtCount += queryLog.getFinalStmtCount();
            }
        }
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
                str.append("apk");
                str.append(",");
                str.append("strategy");
                str.append(",");
                str.append("qTime");
                str.append(",");
                str.append("SCFG");
                str.append(",");
                str.append("runtime");
                str.append(",");
                str.append("qCount");
                str.append(",");
                str.append("degree");
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
            str.append(performanceData.getTotalRuntimeSeconds());
            str.append(",");
            str.append(performanceData.getMaxMemoryConsumption());
            str.append(",");
            str.append(aliasQueryCount);
            str.append(",");
            str.append(degreeOfSparsification());
            str.append(System.lineSeparator());
            writer.write(str.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String degreeOfSparsification(){
        if(finalStmtCount!=0){
            return String.format("%.2f",initalStmtCount/finalStmtCount);
        }
        return "0";
    }

}
