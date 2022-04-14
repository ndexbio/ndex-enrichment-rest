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
public class TestBenjaminiPValueUpdator {
	
	@Test
	public void testEmptyList(){
		BenjaminiPValueUpdator updater = new BenjaminiPValueUpdator();
		updater.updatePValues(new ArrayList<EnrichmentQueryResult>());
	}
	
	@Test
	public void testOneEntry(){
		BenjaminiPValueUpdator updater = new BenjaminiPValueUpdator();
		List<EnrichmentQueryResult> eqrList = new ArrayList<>();
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.5);
		eqrList.add(eqr);
		updater.updatePValues(eqrList);
		assertEquals(0.5, eqrList.get(0).getpValue(), 0.001);
	}
	
	@Test
	public void testTwoEntries(){
		BenjaminiPValueUpdator updater = new BenjaminiPValueUpdator();
		List<EnrichmentQueryResult> eqrList = new ArrayList<>();
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.0005);
		eqr.setDescription("0.0005");
		eqrList.add(eqr);
		
		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.0004);
		eqr.setDescription("0.0004");
		eqrList.add(eqr);
		
		updater.updatePValues(eqrList);
		
		assertEquals(0.0005, eqrList.get(0).getpValue(), 0.001);
		assertEquals(0.0005, eqrList.get(1).getpValue(), 0.001);

	}
	
	@Test
	public void testThreeEntriesUnsorted(){
		BenjaminiPValueUpdator updater = new BenjaminiPValueUpdator();
		List<EnrichmentQueryResult> eqrList = new ArrayList<>();
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.05);
		eqr.setDescription("0.05");
		eqrList.add(eqr);

		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.03);
		eqr.setDescription("0.03");
		eqrList.add(eqr);

		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.04);
		eqr.setDescription("0.04");
		eqrList.add(eqr);
		
		updater.updatePValues(eqrList);
		
		assertEquals("0.03", eqrList.get(0).getDescription());
		assertEquals("0.04", eqrList.get(1).getDescription());
		assertEquals("0.05", eqrList.get(2).getDescription());
		
		assertEquals(0.05, eqrList.get(0).getpValue(), 0.001);
		assertEquals(0.05, eqrList.get(1).getpValue(), 0.001);
		assertEquals(0.05, eqrList.get(2).getpValue(), 0.001);

	}
	
	@Test
	public void testThreeEntriesSorted(){
		BenjaminiPValueUpdator updater = new BenjaminiPValueUpdator();
		List<EnrichmentQueryResult> eqrList = new ArrayList<>();
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.03);
		eqr.setDescription("0.03");
		eqrList.add(eqr);

		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.04);
		eqr.setDescription("0.04");
		eqrList.add(eqr);
		
		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.05);
		eqr.setDescription("0.05");
		eqrList.add(eqr);
		
		updater.updatePValues(eqrList);
		
		assertEquals("0.03", eqrList.get(0).getDescription());
		assertEquals("0.04", eqrList.get(1).getDescription());
		assertEquals("0.05", eqrList.get(2).getDescription());
		assertEquals(0.05, eqrList.get(0).getpValue(), 0.001);
		assertEquals(0.05, eqrList.get(1).getpValue(), 0.001);
		assertEquals(0.05, eqrList.get(2).getpValue(), 0.001);

	}
	
	@Test
	public void testThreeEntriesVeryDifferentPValues(){
		BenjaminiPValueUpdator updater = new BenjaminiPValueUpdator();
		List<EnrichmentQueryResult> eqrList = new ArrayList<>();
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.000001);
		eqr.setDescription("0.000001");
		eqrList.add(eqr);

		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.0001);
		eqr.setDescription("0.0001");
		eqrList.add(eqr);
		
		eqr = new EnrichmentQueryResult();
		eqr.setpValue(0.001);
		eqr.setDescription("0.001");
		eqrList.add(eqr);
		
		updater.updatePValues(eqrList);
		
		assertEquals("0.000001", eqrList.get(0).getDescription());
		assertEquals("0.0001", eqrList.get(1).getDescription());
		assertEquals("0.001", eqrList.get(2).getDescription());
		assertEquals(0.000003, eqrList.get(0).getpValue(), 0.001);
		assertEquals(0.00015, eqrList.get(1).getpValue(), 0.001);
		assertEquals(0.001, eqrList.get(2).getpValue(), 0.001);

	}

}
