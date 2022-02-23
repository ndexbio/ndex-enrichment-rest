package org.ndexbio.enrichment.rest.model.comparators;

import java.util.Comparator;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;

/**
 * Sorts {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} objects 
 * by PValue
 * @author churas
 */
public class EnrichmentQueryResultByPvalue implements Comparator<EnrichmentQueryResult> {

    /**
     * Compares two {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} objects
     * by Pvalue
     * @param o1 {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} object 1 to compare
     * @param o2 {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} object 2 to compare
     * @return If both {@code o1} and {@code o2} are null 0 is returned. If only
     *         {@code o1} is null 1 is returned. If only {@code o2} is null -1 is returned.
     *        -1 if {@code o1}'s pvalue is lower then {@code o2}, 0 if same else 1.
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
        if (o1.getpValue() < o2.getpValue()){
            return -1;
        }
        if (o1.getpValue() == o2.getpValue()){
            return 0;
        }
        return 1;
    }
    
}
