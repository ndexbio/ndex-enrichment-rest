package org.ndexbio.enrichment.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 *
 * @author churas
 */
@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {
    static final JsonMapper MAPPER = JsonMapper.builder()
            .addModule(new JakartaXmlBindAnnotationModule())
            .build();

    @Override
    public ObjectMapper getContext(final Class<?> type) {
        return MAPPER;
    }
}