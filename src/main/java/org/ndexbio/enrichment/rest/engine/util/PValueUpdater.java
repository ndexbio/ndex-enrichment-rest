package org.ndexbio.enrichment.rest.engine.util;

import java.util.List;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;

/**
 * Interface for classes that adjust PValues in a list of EnrichmentQueryResults
 * 
 * @author churas
 */
public interface PValueUpdater {
	
	/**
	 * Adjusts PValue of results pass in
	 * @param eqrList List of results to adjust
	 */
	public void updatePValues(List<EnrichmentQueryResult> eqrList); 
}
