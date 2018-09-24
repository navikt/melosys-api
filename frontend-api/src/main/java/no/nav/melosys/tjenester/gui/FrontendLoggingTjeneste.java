package no.nav.melosys.tjenester.gui;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"Frontend-logger"})
@Path("/logger")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class FrontendLoggingTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger("no.nav.melosys.frontendlogg");

    @POST
    @Path("/trace")
    @ApiOperation(value = "Logger trace-melding", notes = ("Logger trace-melding"))
    public Response frontendTraceLogging(String loggMelding) {
        log.trace(loggMelding);
        return Response.ok().build();
    }

    @POST
    @Path("/debug")
    @ApiOperation(value = "Logger debug-melding", notes = ("Logger debug-melding"))
    public Response frontendDebugLogging(String loggMelding) {
        log.debug(loggMelding);
        return Response.ok().build();
    }

    @POST
    @Path("/info")
    @ApiOperation(value = "Logger info-melding", notes = ("Logger info-melding"))
    public Response frontendInfoLogging(String loggMelding) {
        log.info(loggMelding);
        return Response.ok().build();
    }

    @POST
    @Path("/warn")
    @ApiOperation(value = "Logger warn-melding", notes = ("Logger warn-melding"))
    public Response frontendWarnLogging(String loggMelding) {
        log.warn(loggMelding);
        return Response.ok().build();
    }

    @POST
    @Path("/error")
    @ApiOperation(value = "Logger error-melding", notes = ("Logger error-melding"))
    public Response frontendErrorLogging(String loggMelding) {
        log.error(loggMelding);
        return Response.ok().build();
    }

}
