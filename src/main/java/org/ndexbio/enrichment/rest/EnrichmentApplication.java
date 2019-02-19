package org.ndexbio.enrichment.rest; 

import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.ndexbio.enrichment.rest.services.Enrichment;
import org.ndexbio.enrichment.rest.services.EnrichmentDatabase;
import org.ndexbio.enrichment.rest.services.Status;

public class EnrichmentApplication extends Application {

    private final Set<Object> _singletons = new HashSet<Object>();
    public EnrichmentApplication() {        
        // Register our hello service
        CorsFilter corsFilter = new CorsFilter();
        corsFilter.getAllowedOrigins().add("*");
        corsFilter.setAllowCredentials(true);
        _singletons.add(corsFilter);
    }
    @Override
    public Set<Object> getSingletons() {
        return _singletons;
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        return Stream.of(Enrichment.class,
                EnrichmentDatabase.class,
                Status.class,
                OpenApiResource.class,
                AcceptHeaderOpenApiResource.class).collect(Collectors.toSet());
    }
}