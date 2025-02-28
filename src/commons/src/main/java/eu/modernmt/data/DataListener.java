package eu.modernmt.data;

import java.util.Map;

/**
 * Created by davide on 06/09/16.
 */
public interface DataListener {

    void onDataReceived(DataBatch batch) throws Exception;

    Map<Short, Long> getLatestChannelPositions();

    boolean needsProcessing();

    boolean needsAlignment();

    boolean includeDiscardedTranslationUnits();

}
