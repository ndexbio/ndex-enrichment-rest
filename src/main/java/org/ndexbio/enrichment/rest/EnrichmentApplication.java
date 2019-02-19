package org.ndexbio.enrichment.rest; 

import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import java.io.IOException;
import java.io.InputStream;

import org.ndexbio.enrichment.rest.services.Hello; 
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ndexbio.enrichment.rest.services.Status;

public class EnrichmentApplication extends Application {

    private final Set<Object> _singletons = new HashSet<Object>();
    public EnrichmentApplication() {        
        // Register our hello service
        _singletons.add(new Hello());
    }
    @Override
    public Set<Object> getSingletons() {
        return _singletons;
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        return Stream.of(Hello.class,
                Status.class,
                OpenApiResource.class,
                AcceptHeaderOpenApiResource.class).collect(Collectors.toSet());
    }
}