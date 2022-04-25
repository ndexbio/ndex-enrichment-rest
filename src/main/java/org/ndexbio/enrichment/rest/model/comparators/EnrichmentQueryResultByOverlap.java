package org.ndexbio.enrichment.rest.model.comparators;

import java.util.Comparator;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;

/**
 * Sorts {@code EnrichmentQueryResult} objects by Percent Overlap in descending order,
 * ie higher values are 1st. Fallback is lexigraphical sort by description.
 * 
 * @author churas
 */
public class EnrichmentQueryResultByOverlap implements Comparator<EnrichmentQueryResult> {
	
	
	
	public EnrichmentQueryResultByOverlap(){
	}
	
	/**
     * Compares two {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} objects
     * by Overlap. If overlap  is the same then the description is compared lexigraphically
	 * aka alphabetically
	 * 
     * @param o1 {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} object 1 to compare
     * @param o2 {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} object 2 to compare
     * @return If both {@code o1} and {@code o2} are null 0 is returned. If only
     *         {@code o1} is null 1 is returned. If only {@code o2} is null -1 is returned.
     *        -1 if {@code o1}'s overlap is larger then {@code o2}, if same overlap then 
	 *         lexigraphical comparison value is returned
     * @throws ClassCastException if either input parameter cannot be cast to {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult}       
     */
    @Override
    public int compare(EnrichmentQueryResult o1, EnrichmentQueryResult o2) {
        if (o1 == null && o2 == null){
            return 0;
        }
        if (o1 != null && o2 == null){
            return -1;
        }
        if (o1 == null && o2 != null){
            return 1;
        }
        if (o1.getPercentOverlap() > o2.getPercentOverlap()){
            return -1;
        }
        if (o1.getPercentOverlap() == o2.getPercentOverlap()){
			if (o1.getDescription() == null && o2.getDescription() == null){
				return 0;
			}
			if (o1.getDescription() == null){
				return 1;
			}
			if (o2.getDescription() == null){
				return -1;
			}
            return o1.getDescription().compareTo(o2.getDescription());
        }
        return 1;
    }
}
