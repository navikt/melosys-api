package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"fagsaker"})
@Path("/fagsaker")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AktoerTjeneste extends RestTjeneste {

    private final Tilgang tilgang;

    private final AktoerService aktoerService;

    private final FagsakRepository fagsakRepository;

    @Autowired
    public AktoerTjeneste(Tilgang tilgang, AktoerService aktoerService, FagsakRepository fagsakRepository) {
        this.tilgang = tilgang;
        this.aktoerService = aktoerService;
        this.fagsakRepository = fagsakRepository;
    }

    @GET
    @Path("{saksnummer}/aktoerer/")
    @ApiOperation(
        value = "Henter aktører til en gitt saksnummer.",
        response = AktoerDto.class,
        responseContainer = "List")
    public Response hentAktoerer(@PathParam("saksnummer") String saksnummer,
                                 @QueryParam("aktoersrolle") String aktoersrolle,
                                 @QueryParam("representerer") String representerer
                                ) throws SikkerhetsbegrensningException, TekniskException, IkkeFunnetException {

        Fagsak fagsak = validerFagsak(saksnummer);
        tilgang.sjekkSak(fagsak);

        Aktoer aktoer = aktoerService.hentfagsakAktoerer(fagsak, aktoersrolle, representerer);
            return Response.ok(tilDto(aktoer)).build();
    }

    @POST
    @Path("{saksnummer}/aktoerer/")
    @ApiOperation(
        value = "lagrer/oppdaterer aktør informasjon til en gitt saksnummer.",
        response = AktoerDto.class)
    public Response lagAktoerer(@PathParam("saksnummer") String saksnummer, @ApiParam AktoerDto aktoerDto) throws FunksjonellException, TekniskException {

        Fagsak fagsak = validerFagsak(saksnummer);
        tilgang.sjekkSak(fagsak);
        aktoerService.lagEllerOppdaterAktoer(fagsak, aktoerDto);
        return Response.ok().build();
    }

    private AktoerDto tilDto(Aktoer aktoer) {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID(aktoer.getAktørId());
        aktoerDto.setInstitusjonsID(aktoer.getInstitusjonId());
        aktoerDto.setOrgnr(aktoer.getOrgnr());
        aktoerDto.setRolleKode(aktoer.getRolle().getKode());
        aktoerDto.setUtenlandskPersonID(aktoer.getUtenlandskPersonId());
        if (aktoer.getRepresenterer() != null ) {
            aktoerDto.setRepresentererKode(aktoer.getRepresenterer().getKode());
        }
        return aktoerDto;
    }

    private Fagsak validerFagsak(String saksnummer) throws IkkeFunnetException {
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak == null) {
            throw new IkkeFunnetException("Det finnes ingen fagsak med saksnummer : " + saksnummer);
        }
        return fagsak;
    }
}
