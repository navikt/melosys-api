package no.nav.melosys.tjenester.gui;

import java.time.LocalDateTime;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import no.nav.melosys.tjenester.gui.dto.converter.SaksopplysningerTilDtoConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"fagsak"})
@Path("/fagsaker")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class FagsakTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(FagsakTjeneste.class);

    private FagsakService fagsakService;

    private ModelMapper modelMapper;

    @Autowired
    public FagsakTjeneste(FagsakService fagsakService, DokumentFactory dokumentFactory) {
        this.fagsakService = fagsakService;

        this.modelMapper = new ModelMapper();

        TypeMap<Behandling, BehandlingDto> typeMapBehandlingUt = modelMapper.createTypeMap(Behandling.class, BehandlingDto.class);
        typeMapBehandlingUt.<Long>addMapping(src -> src.getId(), (dest, id) -> dest.getOppsummering().setBehandlingID(id));
        typeMapBehandlingUt.<Long>addMapping(src -> src.getGsakID(), (dest, id) -> dest.getOppsummering().setGsakId(id));
        typeMapBehandlingUt.<BehandlingStatus>addMapping(src -> src.getStatus(), (dest, status) -> dest.getOppsummering().setStatus(status));
        typeMapBehandlingUt.<BehandlingType>addMapping(src -> src.getType(), (dest, type) -> dest.getOppsummering().setType(type));
        typeMapBehandlingUt.<LocalDateTime>addMapping(src -> src.getRegistrertDato(), (dest, dato) -> dest.getOppsummering().setRegistrertDato(dato));
        typeMapBehandlingUt.addMappings(mapper -> mapper.using(new SaksopplysningerTilDtoConverter()).map(Behandling::getSaksopplysninger, BehandlingDto::setSaksopplysninger));
    }

    @GET
    @Path("{saksnr}")
    @ApiOperation(value = "Henter en sak med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public Response hentFagsak(@PathParam("saksnr") @ApiParam("Saksnummer.") Long saksnummer) {
        Fagsak sak = fagsakService.hentFagsak(saksnummer);

        if (sak == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            FagsakDto fagsakDto = tilDto(sak);
            return Response.ok(fagsakDto).build();
        }

    }

    @GET
    @Path("ny/{fnr}")
    @ApiOperation(value = "Oppretter en ny sak med et gitt fødselsnummer.", notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer."))
    public Response nyFagsakSikkret(@PathParam("fnr") @ApiParam("Fødselsnummer.") String fnr) {

        // FIXME Midlertidig tilgangskontroll
        Tilgangskontroll.sjekk();

        return nyFagsak(fnr);
    }

    public Response nyFagsak(String fnr) {
        try {
            Fagsak fagsak = fagsakService.nyFagsak(fnr);

            if (fagsak == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            } else {
                FagsakDto fagsakDto = tilDto(fagsak);
                return Response.ok(fagsakDto).build();
            }
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    private FagsakDto tilDto(Fagsak fagsak) {
        FagsakDto fagsakDto = new FagsakDto();
        modelMapper.map(fagsak, fagsakDto);
        // FIXME saksnummer fra Fagsak bruker id fra DB (midlertidig)
        fagsakDto.setSaksnummer(fagsak.getId());
        return fagsakDto;
    }

}
