package org.ndexbio.enrichment.rest.engine.util;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;

/**
 *
 * @author churas
 */
public class TestBenjaminiHochbergPValueUpdator {
	
	@Test
	public void testEmptyList(){
		BenjaminiHochbergPValueUpdator updater = new BenjaminiHochbergPValueUpdator();
		updater.updatePValues(new ArrayList<EnrichmentQueryResult>());
	}
	
	@Test
	public void testOneEntry(){
		BenjaminiHochbergPValueUpdator updater = new BenjaminiHochbergPValueUpdator();
		List<EnrichmentQueryResult> eqrList = new ArrayList<>();
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.5);
		eqrList.add(eqr);
		updater.updatePValues(eqrList);
		assertEquals(0.5, eqrList.get(0).getpValue(), 0.001);
	}
	
	@Test
	public void testTwoEntries(){
		BenjaminiHochbergPValueUpdator updater = new BenjaminiHochbergPValueUpdator();
		List<EnrichmentQueryResult> eqrList = new ArrayList<>();
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.5);
		eqr.setDescription("0.5");
		eqrList.add(eqr);
		updater.updatePValues(eqrList);
		
		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.4);
		eqr.setDescription("0.4");
		eqrList.add(eqr);
		updater.updatePValues(eqrList);
		assertEquals(0.8, eqrList.get(0).getpValue(), 0.001);
		assertEquals(0.5, eqrList.get(1).getpValue(), 0.001);

	}
	
	@Test
	public void testThreeEntries(){
		BenjaminiHochbergPValueUpdator updater = new BenjaminiHochbergPValueUpdator();
		List<EnrichmentQueryResult> eqrList = new ArrayList<>();
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.05);
		eqr.setDescription("0.05");
		eqrList.add(eqr);
		updater.updatePValues(eqrList);

		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.03);
		eqr.setDescription("0.03");
		eqrList.add(eqr);
		updater.updatePValues(eqrList);

		
		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.04);
		eqr.setDescription("0.04");
		eqrList.add(eqr);
		updater.updatePValues(eqrList);
		assertEquals(0.12, eqrList.get(0).getpValue(), 0.001);
		assertEquals(0.075, eqrList.get(1).getpValue(), 0.001);
		assertEquals(0.06, eqrList.get(2).getpValue(), 0.001);

	}
}
