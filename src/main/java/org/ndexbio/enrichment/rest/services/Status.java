package org.ndexbio.enrichment.rest.services; // Note your package will be {{ groupId }}.rest

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.core.Response;
import org.ndexbio.enrichment.rest.engine.EnrichmentEngine;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.enrichment.rest.model.ServerStatus;

/**
 * Returns status of Server
 * @author churas
 */
@Path(Configuration.V_ONE_PATH)
public class Status {
    
    /**
     * REST endpoint for status 
     */
    public static final String STATUS_PATH = "/status";
    
    static Logger _logger = LoggerFactory.getLogger(Status.class);
    
    
    /**
     * Returns status of server 
     * @return {@link org.ndexbio.enrichment.rest.model.ServerStatus} as JSON
     */
    @GET // This annotation indicates GET request
    @Path(STATUS_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets server status",
               description="Gets version, load, and diskusage of server",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Server Status",
                           content = @Content(mediaType = MediaType.APPLICATION_JSON,
                           schema = @Schema(implementation = ServerStatus.class))),
                   @ApiResponse(responseCode = "500", description = "Server Error",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ErrorResponse.class)))
               })
    public Response status() {

        try {
            EnrichmentEngine enricher = Configuration.getInstance().getEnrichmentEngine();
            if (enricher == null){
                throw new NullPointerException("Enrichment Engine not loaded");
            }
            ServerStatus sObj = enricher.getServerStatus();
            if (sObj == null){
                throw new NullPointerException("No Server Status object returned");
            }
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(sObj).build();
        } 
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error retreiving server status", ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
    }
}