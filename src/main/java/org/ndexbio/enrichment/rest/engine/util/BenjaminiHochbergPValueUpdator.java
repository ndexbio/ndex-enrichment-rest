package org.ndexbio.enrichment.rest.engine.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.comparators.EnrichmentQueryResultByPvalue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adjusts the PValues in the list of EnrichmentQueryResult objects by
 * following formula found in this 2009 paper by Benjamini, Heller, and Yekutieli
 * https://doi.org/10.1098/rsta.2009.0127
 * 
 * @author churas
 */
public class BenjaminiHochbergPValueUpdator implements PValueUpdater {

	static Logger _logger = LoggerFactory.getLogger(BenjaminiHochbergPValueUpdator.class);
	
	private Comparator<EnrichmentQueryResult> _comparator;
	
	/**
	 * Constructor
	 */
	public BenjaminiHochbergPValueUpdator(){
		_comparator = new EnrichmentQueryResultByPvalue();
	}
	
	/**
	 * This method converts the PValues in each EnrichmentQueryResult to
	 * Benjamini & Hotchberg adjusted p-values using the formula in the 2009
	 * paper mentioned in the class documentation. 
	 * 
	 * Basically the EnrichmentQueryResults are sorted by p-value in ascending
	 * order with a rank set starting with 1 and updated as follows:
	 * 
	 * New BH p-value = OLD PVALUE * # of EnrichmentQueryResults / RANK #
	 * 
	 * @param eqrList 
	 */
	@Override
	public void updatePValues(List<EnrichmentQueryResult> eqrList) {
		Collections.sort(eqrList, _comparator);
		int rank = 1;
		int num_networks = eqrList.size();
		for (EnrichmentQueryResult eqr : eqrList){
			eqr.setpValue(Math.min(1.0, (eqr.getpValue()*(double)num_networks)/(double)rank));
			rank++;
		}
	}
	
}
