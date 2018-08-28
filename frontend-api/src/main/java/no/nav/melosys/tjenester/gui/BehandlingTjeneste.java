package no.nav.melosys.tjenester.gui;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.converter.SaksopplysningerTilDtoConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * FIXME Dette brukes bare til test så langt
 */
@Api(tags = { "behandling" })
@Path("/behandlinger")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class BehandlingTjeneste extends RestTjeneste {

    private BehandlingRepository behandlingrepo;

    private ModelMapper modelMapper;

    @Autowired
    public BehandlingTjeneste(BehandlingRepository behandlingrepo) {
        this.behandlingrepo = behandlingrepo;

        this.modelMapper = new ModelMapper();

        TypeMap<Behandling, BehandlingDto> typeMapBehandlingUt = modelMapper.createTypeMap(Behandling.class, BehandlingDto.class);
        typeMapBehandlingUt.<Long>addMapping(Behandling::getId, (dest, id) -> dest.getOppsummering().setBehandlingID(id));
        typeMapBehandlingUt.<BehandlingStatus>addMapping(Behandling::getStatus, (dest, status) -> dest.getOppsummering().setStatus(status));
        typeMapBehandlingUt.<BehandlingType>addMapping(Behandling::getType, (dest, type) -> dest.getOppsummering().setType(type));
        typeMapBehandlingUt.<LocalDateTime>addMapping(Behandling::getRegistrertDato, (dest, dato) -> dest.getOppsummering().setRegistrertDato(dato));
        typeMapBehandlingUt.<LocalDateTime>addMapping(Behandling::getEndretDato, (dest, dato) -> dest.getOppsummering().setEndretDato(dato));
        typeMapBehandlingUt.addMappings(mapper -> mapper.using(new SaksopplysningerTilDtoConverter()).map(Behandling::getSaksopplysninger, BehandlingDto::setSaksopplysninger));
    }

    @GET
    @Path("{id}")
    public BehandlingDto hentBehandling(@PathParam("id") Long id) {
        Behandling behandling = behandlingrepo.findOne(id);

        BehandlingDto behandlingDto = new BehandlingDto();
        modelMapper.map(behandling, behandlingDto);
        return behandlingDto;
    }

    @GET
    public List<BehandlingDto> hentAlle() {
        Iterable<Behandling> alle = behandlingrepo.findAll();

        List<BehandlingDto> behandlingDtoListe = new ArrayList<>();
        for (Behandling b : alle) {
            BehandlingDto behandlingDto = new BehandlingDto();
            modelMapper.map(b, behandlingDto);
            behandlingDtoListe.add(behandlingDto);
        }

        return behandlingDtoListe;
    }

}
