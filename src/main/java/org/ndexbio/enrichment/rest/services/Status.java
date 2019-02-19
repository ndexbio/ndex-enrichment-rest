package org.ndexbio.enrichment.rest.services; // Note your package will be {{ groupId }}.rest

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
import java.lang.management.OperatingSystemMXBean;
import org.ndexbio.enrichment.rest.model.ServerStatus;

/**
 * Returns status of Server
 * @author churas
 */
@Path("/")
public class Status {
    
    static Logger logger = LoggerFactory.getLogger(Status.class);
    
    /**
     * Returns status of server 
     * @return {@link org.ndexbio.enrichment.rest.model.ServerStatus} as JSON
     */
    @GET // This annotation indicates GET request
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets server status",
               description="Gets version, load, and diskusage of server",
               responses = {
                   @ApiResponse(description = "Server Status",
                           content = @Content(mediaType = MediaType.APPLICATION_JSON,
                           schema = @Schema(implementation = ServerStatus.class)))
               })
    public ServerStatus status() {
        String version = "unknown";
        ObjectMapper omappy = new ObjectMapper();
        ServerStatus sObj = new ServerStatus();
        sObj.setStatus(ServerStatus.OK_STATUS);
        sObj.setRestVersion(version);
        OperatingSystemMXBean omb = ManagementFactory.getOperatingSystemMXBean();
        sObj.setSystemLoad(omb.getSystemLoadAverage());
        return sObj;        
    }
}