package soot.jimple.infoflow.aliasing.sparse;

import boomerang.scene.sparse.SparseCFGCache;
import soot.jimple.infoflow.InfoflowManager;

public class DefaultBoomerangAliasStrategy extends AbstractBoomerangAliasStrategy {
    public DefaultBoomerangAliasStrategy(InfoflowManager manager) {
        super(manager);
    }

    @Override
    public SparseAliasManager getSparseAliasManager() {
        return SparseAliasManager.getInstance(SparseCFGCache.SparsificationStrategy.NONE);
    }
}
