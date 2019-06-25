package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.*;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.tjenester.gui.dto.periode.AnmodningsperiodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"anmodningsperioder"})
@Service
@Path("/anmodningsperioder")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AnmodningsperiodeTjeneste extends RestTjeneste {

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final Tilgang tilgang;

    @Autowired
    public AnmodningsperiodeTjeneste(AnmodningsperiodeService anmodningsperiodeService, Tilgang tilgang) {
        super();
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.tilgang = tilgang;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Henter en anmodningsperiode for en gitt behandling", response = AnmodningsperiodeDto.class)
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingID-en ikke fins.")})
    public Collection<AnmodningsperiodeDto> hentAnmodningsperioder(@PathParam("behandlingID") long behandlingID) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgang.sjekk(behandlingID);
        return anmodningsperiodeService
            .hentAnmodningsperioder(behandlingID)
            .stream()
            .map(AnmodningsperiodeDto::av)
            .collect(Collectors.toList());
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation("Lagrer en anmodningsperiode for en gitt behandling.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingID-en ikke fins.")})
    public Collection<AnmodningsperiodeDto> lagreAnmodningsperioder(@PathParam("behandlingID") long behandlingID,
                                                                @ApiParam(value = "En liste av anmodningsperioder å lagre.") Collection<AnmodningsperiodeDto> anmodningsperiodeDtoer)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgang.sjekk(behandlingID);
        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.lagreAnmodningsperioder(
            behandlingID, anmodningsperiodeDtoer.stream().map(AnmodningsperiodeDto::til).collect(Collectors.toList())
        );
        return anmodningsperioder.stream().map(AnmodningsperiodeDto::av).collect(Collectors.toList());
    }
}