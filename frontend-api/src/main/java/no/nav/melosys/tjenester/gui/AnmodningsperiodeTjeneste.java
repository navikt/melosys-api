package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.*;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeListeDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodePostDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSvarDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"anmodningsperioder"})
@Service
@Path("/")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AnmodningsperiodeTjeneste extends RestTjeneste {
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final TilgangService tilgangService;

    @Autowired
    public AnmodningsperiodeTjeneste(AnmodningsperiodeService anmodningsperiodeService, TilgangService tilgangService) {
        super();
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.tilgangService = tilgangService;
    }

    @GET
    @Path("anmodningsperioder/{behandlingID}")
    @ApiOperation(value = "Henter anmodningsperioder for en gitt behandling", response = AnmodningsperiodeDto.class)
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingID-en ikke fins.")})
    public AnmodningsperiodeListeDto hentAnmodningsperioder(@PathParam("behandlingID") long behandlingID)
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        return AnmodningsperiodeListeDto.av(anmodningsperiodeService.hentAnmodningsperioder(behandlingID));
    }

    @POST
    @Path("anmodningsperioder/{behandlingID}")
    @ApiOperation("Lagrer anmodningsperioder for en gitt behandling.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingID-en ikke fins.")})
    public AnmodningsperiodeListeDto lagreAnmodningsperioder(@PathParam("behandlingID") long behandlingID,
                                                             @ApiParam(value = "En liste av anmodningsperioder å lagre.")
                                                                 AnmodningsperiodePostDto anmodningsperiodePostDto)
        throws TekniskException, FunksjonellException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.lagreAnmodningsperioder(
            behandlingID, anmodningsperiodePostDto.getAnmodningsperioder().stream().map(AnmodningsperiodeDto::til)
                .collect(Collectors.toList())
        );
        return AnmodningsperiodeListeDto.av(anmodningsperioder);
    }

    @GET
    @Path("anmodningsperioder/svar/{anmodningperiodeID}")
    @ApiOperation("Henter svar på en anmodningsperiode.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom anmodningsperioden ikke fins.")})
    public AnmodningsperiodeSvarDto hentAnmodningsperiodeSvar(@PathParam("anmodningperiodeID") long anmodningperiodeID)
        throws FunksjonellException, TekniskException {

        Optional<Anmodningsperiode> anmodningsperiodeOptional = anmodningsperiodeService.hentAnmodningsperiode(anmodningperiodeID);

        long behandlingID = anmodningsperiodeOptional.map(Anmodningsperiode::getBehandlingsresultat)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke anmodningsperiode med id " + anmodningperiodeID)).getId();

        tilgangService.sjekkTilgang(behandlingID);

        Optional<AnmodningsperiodeSvar> svar = anmodningsperiodeOptional.map(Anmodningsperiode::getAnmodningsperiodeSvar);

        return svar.map(AnmodningsperiodeSvarDto::fra).orElseGet(AnmodningsperiodeSvarDto::new);
    }

    @POST
    @Path("anmodningsperioder/svar/{anmodningperiodeID}")
    @ApiOperation("Lagrer svar på en anmodningsperiode.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom anmodningsperioden ikke fins.")})
    public AnmodningsperiodeSvarDto lagreAnmodningsperiodeSvar(@PathParam("anmodningperiodeID") long anmodningperiodeID,
                                                               @ApiParam(value = "Svar på anmodningsperiode som skal lagres.") AnmodningsperiodeSvarDto anmodningsperiodeSvarDto)
        throws FunksjonellException, TekniskException {

        long behandlingID = anmodningsperiodeService.hentAnmodningsperiode(anmodningperiodeID)
            .map(Anmodningsperiode::getBehandlingsresultat)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke anmodningsperiode med id " + anmodningperiodeID)).getId();
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        AnmodningsperiodeSvar svar = anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningperiodeID, anmodningsperiodeSvarDto.til());
        return AnmodningsperiodeSvarDto.fra(svar);
    }
}