package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.*;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeGetDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodePostDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSkrivDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSvarDto;
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
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final TilgangService tilgangService;

    @Autowired
    public AnmodningsperiodeTjeneste(AnmodningsperiodeService anmodningsperiodeService, LovvalgsperiodeService lovvalgsperiodeService, TilgangService tilgangService) {
        super();
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.tilgangService = tilgangService;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Henter anmodningsperioder for en gitt behandling", response = AnmodningsperiodeSkrivDto.class)
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingID-en ikke fins.")})
    public AnmodningsperiodeGetDto hentAnmodningsperioder(@PathParam("behandlingID") long behandlingID)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        return AnmodningsperiodeGetDto.av(anmodningsperiodeService.hentAnmodningsperioder(behandlingID));
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation("Lagrer anmodningsperioder for en gitt behandling.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingID-en ikke fins.")})
    public AnmodningsperiodeGetDto lagreAnmodningsperioder(@PathParam("behandlingID") long behandlingID,
                                                           @ApiParam(value = "En liste av anmodningsperioder å lagre.")
                                                                 AnmodningsperiodePostDto anmodningsperiodePostDto)
        throws TekniskException, FunksjonellException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.lagreAnmodningsperioder(
            behandlingID, anmodningsperiodePostDto.getAnmodningsperioder().stream().map(AnmodningsperiodeSkrivDto::til)
                .collect(Collectors.toList())
        );
        return AnmodningsperiodeGetDto.av(anmodningsperioder);
    }

    @GET
    @Path("{anmodningsperiodeID}/svar")
    @ApiOperation("Henter svar på en anmodningsperiode.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom anmodningsperioden ikke fins.")})
    public AnmodningsperiodeSvarDto hentAnmodningsperiodeSvar(@PathParam("anmodningsperiodeID") long anmodningsperiodeID)
        throws FunksjonellException, TekniskException {

        Optional<Anmodningsperiode> anmodningsperiodeOptional = anmodningsperiodeService.hentAnmodningsperiode(anmodningsperiodeID);

        long behandlingID = anmodningsperiodeOptional.map(Anmodningsperiode::getBehandlingsresultat)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke anmodningsperiode med id " + anmodningsperiodeID)).getId();

        tilgangService.sjekkTilgang(behandlingID);

        Optional<AnmodningsperiodeSvar> svar = anmodningsperiodeOptional.map(Anmodningsperiode::getAnmodningsperiodeSvar);

        return svar.map(AnmodningsperiodeSvarDto::av).orElseGet(AnmodningsperiodeSvarDto::new);
    }

    @POST
    @Path("{anmodningsperiodeID}/svar")
    @ApiOperation("Lagrer svar på en anmodningsperiode.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom anmodningsperioden ikke fins.")})
    public AnmodningsperiodeSvarDto lagreAnmodningsperiodeSvar(@PathParam("anmodningsperiodeID") long anmodningsperiodeID,
                                                               @ApiParam(value = "Svar på anmodningsperiode som skal lagres.") AnmodningsperiodeSvarDto anmodningsperiodeSvarDto)
        throws FunksjonellException, TekniskException {

        long behandlingID = anmodningsperiodeService.hentAnmodningsperiode(anmodningsperiodeID)
            .map(Anmodningsperiode::getBehandlingsresultat)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke anmodningsperiode med id " + anmodningsperiodeID)).getId();
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        AnmodningsperiodeSvar svar = anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, anmodningsperiodeSvarDto.til());

        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(svar.getAnmodningsperiode(), Medlemskapstyper.PLIKTIG);
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID, Collections.singleton(lovvalgsperiode));

        return AnmodningsperiodeSvarDto.av(svar);
    }
}