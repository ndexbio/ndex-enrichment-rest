/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model.comparators;

import java.util.Comparator;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;

/**
 * Sorts {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} objects 
 * by PValue
 * @author churas
 */
public class EnrichmentQueryResultByPvalue implements Comparator {

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
    public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null){
            return 0;
        }
        if (o1 != null && o2 == null){
            return -1;
        }
        if (o1 == null && o2 != null){
            return 1;
        }
        if (o1 instanceof EnrichmentQueryResult == false){
            throw new ClassCastException ("o1 is not of type EnrichmentQueryResult");
        }
        if (o2 instanceof EnrichmentQueryResult == false){
            throw new ClassCastException ("o2 is not of type EnrichmentQueryResult");
        }
        EnrichmentQueryResult eqr1 = (EnrichmentQueryResult)o1;
        EnrichmentQueryResult eqr2 = (EnrichmentQueryResult)o2;
        if (eqr1.getpValue() < eqr2.getpValue()){
            return -1;
        }
        if (eqr1.getpValue() == eqr2.getpValue()){
            return 0;
        }
        return 1;
    }
    
}
