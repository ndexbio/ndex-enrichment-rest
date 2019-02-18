package org.ndexbio.enrichment.app; 

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

import org.ndexbio.enrichment.rest.HelloRestService;
import org.ndexbio.enrichment.rest.HelloRestService; 
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnrichmentApplication extends Application {
    private Set<Object> singletons = new HashSet<Object>();
    public EnrichmentApplication() {
        // Register our hello service
        singletons.add(new HelloRestService());
    }
    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        return Stream.of(OpenApiResource.class).collect(Collectors.toSet());
    }
}