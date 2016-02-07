package hudson.plugins.templateproject;

import java.io.IOException;
import java.util.List;

import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.BuildListener;

public class ProxyMatrixAggregator extends MatrixAggregator {

    private List<MatrixAggregator> aggregatorList;

    protected ProxyMatrixAggregator(MatrixBuild build, Launcher launcher, BuildListener listener, List<MatrixAggregator> aggregatorList) {
        super(build, launcher, listener);
        this.aggregatorList = aggregatorList;
    }

    /* (non-Javadoc)
     * @see hudson.matrix.MatrixAggregator#startBuild()
     */
    @Override
    public boolean startBuild() throws InterruptedException, IOException {
        for (MatrixAggregator matrixAggregator : aggregatorList) {
            if (!matrixAggregator.startBuild()) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see hudson.matrix.MatrixAggregator#endRun(hudson.matrix.MatrixRun)
     */
    @Override
    public boolean endRun(MatrixRun run) throws InterruptedException, IOException {
        for (MatrixAggregator matrixAggregator : aggregatorList) {
            if (!matrixAggregator.endRun(run)) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see hudson.matrix.MatrixAggregator#endBuild()
     */
    @Override
    public boolean endBuild() throws InterruptedException, IOException {
        for (MatrixAggregator matrixAggregator : aggregatorList) {
            if (!matrixAggregator.endBuild()) {
                return false;
            }
        }
        return true;
    }

}