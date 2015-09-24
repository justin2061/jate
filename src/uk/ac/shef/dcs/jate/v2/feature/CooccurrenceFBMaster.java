package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.lucene.index.IndexReader;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 20/09/2015.
 */
public class CooccurrenceFBMaster extends AbstractFeatureBuilder {
    private static final Logger LOG = Logger.getLogger(CooccurrenceFBMaster.class.getName());
    protected FrequencyCtxBased frequencyCtxBased;
    protected FrequencyTermBased frequencyTermBased;
    protected int minTTF;
    protected int minTCF;

    public CooccurrenceFBMaster(IndexReader index, JATEProperties properties,
                                FrequencyTermBased termFeature,
                                int minTTF,
                                FrequencyCtxBased contextFeature,
                                int minTCF) {
        super(index, properties);
        this.frequencyCtxBased = contextFeature;
        this.frequencyTermBased=termFeature;
        this.minTTF=minTTF;
        this.minTCF=minTCF;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        List<String> contextIds = new ArrayList<>(frequencyCtxBased.getMapCtx2TTF().keySet());
        //start workers
        int cores = Runtime.getRuntime().availableProcessors();
        cores = (int) (cores * properties.getFeatureBuilderMaxCPUsage());
        cores = cores == 0 ? 1 : cores;
        StringBuilder sb = new StringBuilder("Building features using cpu cores=");
        sb.append(cores).append(", total ctx=").append(contextIds.size()).append(", max per worker=")
                .append(properties.getFeatureBuilderMaxDocsPerWorker());
        LOG.info(sb.toString());
        CooccurrenceFBWorker worker = new
                CooccurrenceFBWorker(contextIds, frequencyTermBased, minTTF, frequencyCtxBased, minTCF,properties.getFeatureBuilderMaxTermsPerWorker());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        Cooccurrence feature = forkJoinPool.invoke(worker);
        sb = new StringBuilder("Complete building features.");
        LOG.info(sb.toString());

        return feature;
    }
}
