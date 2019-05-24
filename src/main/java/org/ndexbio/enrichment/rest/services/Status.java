package org.ndexbio.enrichment.rest.services; // Note your package will be {{ groupId }}.rest

import com.fasterxml.jackson.core.JsonProcessingException;
import java.lang.management.ManagementFactory;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.File;
import java.lang.management.OperatingSystemMXBean;
import javax.ws.rs.core.Response;
import org.ndexbio.enrichment.rest.engine.EnrichmentEngine;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.enrichment.rest.model.ServerStatus;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;

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
        ObjectMapper omappy = new ObjectMapper();

        try {
            EnrichmentEngine enricher = Configuration.getInstance().getEnrichmentEngine();
            if (enricher == null){
                throw new NullPointerException("Enrichment Engine not loaded");
            }
            ServerStatus sObj = enricher.getServerStatus();
            if (sObj == null){
                throw new NullPointerException("No Server Status object returned");
            }
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(omappy.writeValueAsString(sObj)).build();
        } 
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error retreiving server status", ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
    }
}