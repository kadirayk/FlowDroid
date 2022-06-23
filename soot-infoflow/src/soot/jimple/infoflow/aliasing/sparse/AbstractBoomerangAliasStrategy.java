package soot.jimple.infoflow.aliasing.sparse;

import boomerang.scene.Field;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleField;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.scene.jimple.JimpleVal;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.util.AccessPath;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import heros.solver.Pair;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.AbstractBulkAliasStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.data.AccessPathFragment;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractBoomerangAliasStrategy extends AbstractBulkAliasStrategy {

    private IInfoflowCFG icfg;
    private IInfoflowCFG bwicfg;
    private LinkedList<String> methodIgnoreList;

    private Multimap<Pair<SootMethod, Abstraction>, Pair<Unit, Abstraction>> incomingMap =
            HashMultimap.create();

    public AbstractBoomerangAliasStrategy(InfoflowManager manager) {
        super(manager);
        this.icfg = manager.getICFG();
        this.bwicfg = new BackwardsInfoflowCFG(this.icfg);

        methodIgnoreList = new LinkedList<String>();
        methodIgnoreList.add("int hashCode()");
        methodIgnoreList.add("boolean equals(java.lang.Object)");
    }

    protected abstract SparseAliasManager getSparseAliasManager();


    private boolean isIgnoredMethod(SootMethod m) {
        for (String ign : methodIgnoreList) {
            if (m.getSignature().contains(ign))
                return true;
        }

        return false;
    }

    @Override
    public void computeAliasTaints(Abstraction d1, Stmt src, Value targetValue, Set<Abstraction> taintSet, SootMethod method, Abstraction newAbs) {
        Local base = newAbs.getAccessPath().getPlainValue();
        if (isIgnoredMethod(icfg.getMethodOf(src)))
            return;
        if (base == null)
            return;

        // There are two different queries necessary: At field writes and at method return statements,
        // when there might be new alias in the caller scope.
//        if (src.containsInvokeExpr()) {
//            handleReturn(d1, src, taintSet, newAbs, base);
//        } else {
            handleFieldWrite(d1, src, taintSet, newAbs, base);
//        }
    }


    private void handleReturn(Abstraction d1, Stmt src, Set<Abstraction> taintSet, Abstraction newAbs, Local base) {
        SootMethod method = icfg.getMethodOf(src);
        // Upon return, the last field access is dropped from the abstraction and a query is triggered
        // for this access graph. Then for each of the result, the last field is re-appended and those
        // access paths are propagated forward
        SootField lastField = newAbs.getAccessPath().getLastField();
        if (lastField == null) {
            return;
        }
        if (d1.equals(newAbs)) {
            return;
        }
        List<SootField> fields = getFields(newAbs);
        SparseAliasManager aliasManager = getSparseAliasManager();
        Set<AccessPath> aliases = aliasManager.getAliases(src, method, base);
        aliases = removeRedundantAlias(newAbs, aliases);
        Set<AccessPath> aps = new HashSet<>();
        for (AccessPath ap : aliases) {
            aps.add(appendFields(ap, fields));
        }
        Set<Abstraction> fwaps = new HashSet<>();
        for (AccessPath ap : aps) {
            fwaps.add(toFlowDroidAcessPath(ap, src, newAbs));
        }
        taintSet.addAll(fwaps);
    }

//    private Set<AccessPath> popLastField(AccessPath accessPath, SootField initialLastField) {
//        Set<AccessPath> accessPaths = new HashSet<>();
//        if (!accessPath.isOverApproximated()) { // if it is not a set
//            Collection<Field> fields = accessPath.getFields();
//            Field lastField = null;
//            if (fields != null && !fields.isEmpty()) {
//                for (Field field : fields) {
//                    lastField = field;
//                }
//            }
//            if (lastField != null) {
//                fields.remove(lastField);
//            }
//            AccessPath withoutLastField = new AccessPath(accessPath.getBase(), fields);
//            accessPaths.add(withoutLastField);
//        } else { // it it is a set then don't remove the last field but return all fields
//            Collection<Field> fields = accessPath.getFields();
//            for (Field field : fields) {
//                if (field instanceof JimpleField) {
//                    JimpleField jField = (JimpleField) field;
//                    SootField sootField = jField.getSootField();
//                    if (sootField.equals(initialLastField)) {
//                        continue;
//                    }
//                    AccessPath ap = new AccessPath(accessPath.getBase(), field);
//                    accessPaths.add(ap);
//                }
//            }
//        }
//        return accessPaths;
//    }


//    private void handleReturn(Abstraction d1, Stmt src, Set<Abstraction> taintSet,
//                              Abstraction newAbs, Local base) {
//
//        // Upon return, the last field access is dropped from the abstraction and a query is triggered
//        // for this access graph. Then for each of the result, the last field is re-appended and those
//        // access paths are propagated forward
//        SootField lastField = newAbs.getAccessPath().getLastField();
//        if (newAbs.getAccessPath().getLastFieldType() instanceof ArrayType) {
//
//            // Special handling for arrays
//            SootField[] fields = newAbs.getAccessPath().getFields();
//            AccessGraph accessPath = new AccessGraph(base, base.getType(), toListFieldWithStmt(fields));
//            SootMethod method = boomerang.context.icfg.getMethodOf(src);
//
//            AliasResults res = queryBoomerang(accessPath, src, method, d1);
//            Set<AccessGraph> accessPathOnly = res.mayAliasSet();
//
//            for (AccessGraph ap : accessPathOnly) {
//                taintSet.add(toFlowDroidAccessPath(ap, src, newAbs));
//            }
//            return;
//        }
//        if (lastField == null)
//            return;
//        if (d1.equals(newAbs))
//            return;
//        SootField[] fields = newAbs.getAccessPath().getFields();
//        AccessGraph accessPath = new AccessGraph(base, base.getType(), toListFieldWithStmt(fields));
//        Set<AccessGraph> withoutLastFields = accessPath.popLastField();
//        SootMethod method = icfg.getMethodOf(src);
//        for (AccessGraph withoutLastField : withoutLastFields) {
//            // trigger query for access graph with droped/popped last field.
//            AliasResults res = queryBoomerang(withoutLastField, src, method, d1);
//            Set<AccessGraph> accessPathOnly = res.mayAliasSet();
//            if (lastField != null)
//                // add the last field again.
//                accessPathOnly =
//                        AliasResults.appendField(accessPathOnly,
//                                new WrappedSootField(lastField, lastField.getType(), null), boomerang.context);
//
//            for (AccessGraph ap : accessPathOnly) {
//                Abstraction flowDroidAccessPath = toFlowDroidAccessPath(ap, src, newAbs);
//                if (flowDroidAccessPath != null)
//                    taintSet.add(flowDroidAccessPath);
//            }
//        }
//    }

    private Collection<Field> toJimpleFields(List<SootField> fields) {
        List<Field> jimpleFields = new ArrayList<>();
        if (fields != null) {
            for (SootField field : fields) {
                JimpleField jf = new JimpleField(field);
                jimpleFields.add(jf);
            }
        }
        return jimpleFields;
    }

    private Val toJimpleVal(Local val, SootMethod method) {
        JimpleMethod jimpleMethod = JimpleMethod.of(method);
        JimpleVal jimpleVal = new JimpleVal(val, jimpleMethod);
        return jimpleVal;
    }

    private synchronized void handleFieldWrite(Abstraction d1, Stmt src, Set<Abstraction> taintSet, Abstraction newAbs, Local base) {
        if (base == null)
            return;
        SootMethod method = icfg.getMethodOf(src);
        SparseAliasManager aliasManager = getSparseAliasManager();
        // Query for the base variable
        Set<AccessPath> aliases = aliasManager.getAliases(src, method, base);
        //aliases.forEach(System.out::println);

        //remove the redundant query value from aliases
        aliases = removeRedundantAlias(newAbs, aliases);

        List<SootField> fields = getFields(newAbs);
        // append the fields the incoming access path had to the result set
        Set<AccessPath> aliasesWitFields = new HashSet<>();
        if (!fields.isEmpty()) {
            for (AccessPath alias : aliases) {
                AccessPath withField = appendFields(alias, fields);
                aliasesWitFields.add(withField);
            }
        } else {
            aliasesWitFields = aliases;
        }
        // TODO: check if we need to remove the aliases without fields
        for (AccessPath alias : aliasesWitFields) {
            Abstraction flowDroidAP = toFlowDroidAcessPath(alias, src, newAbs);
            // add all access path to the taintSet for further propagation
            if (flowDroidAP != null) {
                taintSet.add(flowDroidAP);
            }
        }

    }

    /**
     * simply removes the initial query val itself from the found aliases.
     *
     * @param newAbs
     * @param aliases
     * @return
     */
    private Set<AccessPath> removeRedundantAlias(Abstraction newAbs, Set<AccessPath> aliases) {
        return aliases.stream()
                .filter(a -> (a.getBase() instanceof JimpleVal)
                        && !newAbs.getAccessPath().getPlainValue().equals(((JimpleVal) a.getBase()).getDelegate()))
                .collect(Collectors.toSet());
    }

    private List<SootField> getFields(Abstraction newAbs) {
        AccessPathFragment[] fragments = newAbs.getAccessPath().getFragments();
        List<SootField> fields = new ArrayList<>();
        if (fragments != null) {
            for (AccessPathFragment fragment : fragments) {
                fields.add(fragment.getField());
            }
        }
        return fields;
    }


    /**
     * Maps an Boomerang access graph to a FlowDroid access path (Abstraction).
     *
     * @param boomerangAP
     * @param src
     * @param newAbs
     * @return
     */
    private synchronized Abstraction toFlowDroidAcessPath(AccessPath boomerangAP, Stmt src, Abstraction newAbs) {
        AccessPathFactory accessPathFactory = new AccessPathFactory(manager.getConfig());
        Value base = ((JimpleVal) boomerangAP.getBase()).getDelegate();
        Collection<Field> fields = boomerangAP.getFields();
        List<SootField> sootFieldList = fields.stream().map(e->((JimpleField) e).getSootField()).collect(Collectors.toList());
/*        for(Iterator<Field> iter = fields.iterator(); iter.hasNext();){
            Field next = iter.next();
            SootField sootField = ((JimpleField) next).getSootField();
            sootFieldList.add(sootField);
        }
*/
        soot.jimple.infoflow.data.AccessPath flowDroidAP = accessPathFactory.createAccessPath(base, sootFieldList.toArray(new SootField[sootFieldList.size()]), true);
        return newAbs.deriveNewAbstraction(flowDroidAP, src);
    }


    private AccessPath appendFields(AccessPath accessPath, List<SootField> fields) {
        if (fields.size() == 0) {
            return accessPath;
        }
        Collection<Field> apFields = accessPath.getFields();
        for (SootField field : fields) {
            Field apField = new JimpleField(field);
            apFields.add(apField);
        }
        return new AccessPath(accessPath.getBase(), apFields);
    }


    @Override
    public void injectCallingContext(Abstraction abs, IInfoflowSolver fSolver, SootMethod callee, Unit callSite, Abstraction source, Abstraction d1) {
        // This is called whenever something is added to the incoming set of the forward solver of the
        // FlowDroid IFDS solver.
        Pair<SootMethod, Abstraction> calleepair = new Pair<>(callee, abs);
        Pair<Unit, Abstraction> callerpair = new Pair<>(callSite, d1);
        incomingMap.put(calleepair, callerpair);
    }

    @Override
    public boolean isFlowSensitive() {
        return true;
    }

    @Override
    public boolean requiresAnalysisOnReturn() {
        return true;
    }

    @Override
    public IInfoflowSolver getSolver() {
        return null;
    }

    @Override
    public void cleanup() {

    }
}
