package org.ndexbio.enrichment.rest.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author churas
 */
public class ExecutorServiceFactoryImpl implements ExecutorServiceFactory {

	
	public ExecutorServiceFactoryImpl(){
	}
	
	@Override
	public ExecutorService getExecutorService(final int numWorkers) {
		return Executors.newFixedThreadPool(numWorkers);
	}
	
}
