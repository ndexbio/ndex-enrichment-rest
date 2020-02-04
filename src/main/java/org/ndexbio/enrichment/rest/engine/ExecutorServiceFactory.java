package org.ndexbio.enrichment.rest.engine;

import java.util.concurrent.ExecutorService;

/**
 *
 * @author churas
 */
public interface ExecutorServiceFactory {
	
	public ExecutorService getExecutorService(int numWorkers);
	
}
