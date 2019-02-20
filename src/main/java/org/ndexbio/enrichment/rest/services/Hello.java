package org.ndexbio.enrichment.rest.services; // Note your package will be {{ groupId }}.rest

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class Hello {
    
    static Logger logger = LoggerFactory.getLogger(Hello.class);
    
    @GET // This annotation indicates GET request
    @Path("/hello")
    public Response hello() {
        logger.info("I am responding to get request :)");
        return Response.status(200).entity("hello there").build();
    }
}