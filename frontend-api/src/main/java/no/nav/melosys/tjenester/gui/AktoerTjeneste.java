package no.nav.melosys.tjenester.gui;

import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.sak.FagsakService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import static java.util.stream.Collectors.toList;

@Api(tags = {"fagsaker"})
@Path("/fagsaker")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AktoerTjeneste extends RestTjeneste {

    private final TilgangService tilgangService;

    private final AktoerService aktoerService;

    private final FagsakService fagsakService;

    @Autowired
    public AktoerTjeneste(TilgangService tilgangService, AktoerService aktoerService, FagsakService fagsakService) {
        this.tilgangService = tilgangService;
        this.aktoerService = aktoerService;
        this.fagsakService = fagsakService;
    }

    @GET
    @Path("{saksnummer}/aktoerer/")
    @ApiOperation(
        value = "Henter aktører knyttet til et gitt saksnummer.",
        response = AktoerDto.class,
        responseContainer = "List")
    public List<AktoerDto> hentAktoerer(@PathParam("saksnummer") String saksnummer,
                                        @QueryParam("rolleKode") String rolleKode,
                                        @QueryParam("representerer") String representerer)
        throws SikkerhetsbegrensningException, TekniskException, IkkeFunnetException {

        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        tilgangService.sjekkSak(fagsak);

        Aktoersroller rolle = null;
        Representerer representantRepresenterer = null;
        if (StringUtils.isNotEmpty(rolleKode)) {
            rolle = Aktoersroller.valueOf(rolleKode);
        }
        if (StringUtils.isNotEmpty(representerer)) {
            representantRepresenterer = Representerer.valueOf(representerer);
        }

        List<Aktoer> aktører = aktoerService.hentfagsakAktører(fagsak, rolle, representantRepresenterer);
        return aktører.stream().map(this::tilDto).collect(toList());
    }

    @POST
    @Path("{saksnummer}/aktoerer/")
    @ApiOperation(
        value = "Lagrer/oppdaterer aktør informasjon for et gitt saksnummer.",
        response = AktoerDto.class)
    public Response lagAktoerer(@PathParam("saksnummer") String saksnummer, @ApiParam AktoerDto aktoerDto) throws FunksjonellException, TekniskException {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        tilgangService.sjekkSak(fagsak);
        Long databaseId = aktoerService.lagEllerOppdaterAktoer(fagsak, aktoerDto);
        aktoerDto.setDatabaseID(databaseId);
        return Response.ok(aktoerDto).build();
    }

    @DELETE
    @Path("/aktoerer/{databaseID}")
    @ApiOperation(
        value = "Sletter aktøren med en gitt database-id.",
        response = AktoerDto.class)
    public Response slettAktoer(@PathParam("databaseID") long databaseID) throws TekniskException, FunksjonellException {
        aktoerService.slettAktoer(databaseID);
        return Response.ok().build();
    }

    private AktoerDto tilDto(Aktoer aktoer) {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID(aktoer.getAktørId());
        aktoerDto.setInstitusjonsID(aktoer.getInstitusjonId());
        aktoerDto.setOrgnr(aktoer.getOrgnr());
        aktoerDto.setRolleKode(aktoer.getRolle().getKode());
        aktoerDto.setUtenlandskPersonID(aktoer.getUtenlandskPersonId());
        if (aktoer.getRepresenterer() != null) {
            aktoerDto.setRepresentererKode(aktoer.getRepresenterer().getKode());
        }
        aktoerDto.setDatabaseID(aktoer.getId());
        return aktoerDto;
    }
}
