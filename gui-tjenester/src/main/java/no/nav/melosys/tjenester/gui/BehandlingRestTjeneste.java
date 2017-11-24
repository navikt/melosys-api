package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.patch.ObjectPatch;
import no.nav.melosys.tjenester.gui.patch.ObjectPatchException;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

@Api(tags = { "behandling" })
@Path("/behandlinger")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class BehandlingRestTjeneste extends RestTjeneste {

    private BehandlingRepository behandlingrepo;

    private ModelMapper modelMapper;

    @Autowired
    public BehandlingRestTjeneste(BehandlingRepository behandlingrepo, DokumentFactory dokumentFactory) {
        this.behandlingrepo = behandlingrepo;

        this.modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);

        TypeMap<Behandling, BehandlingDto> typeMapBehandlingUt = modelMapper.createTypeMap(Behandling.class, BehandlingDto.class);

        Converter<Set<Saksopplysning>, Set<SaksopplysningDokument>> saksopplysningTilDokument = ctx -> {
            Set dokumenter = new HashSet();
            if (ctx.getSource() != null) {
                //ctx.getSource().forEach(x -> dokumenter.add(((Saksopplysning) x).getDokument()));
                // TODO dokumentFactory er ikke nødvendig hvis vi lagrer den interne modellen fra starten.
                ctx.getSource().forEach(x -> dokumenter.add(dokumentFactory.lagDokument((Saksopplysning) x)));
            }
            return dokumenter;
        };
        typeMapBehandlingUt.addMappings(mapper -> mapper.using(saksopplysningTilDokument).map(Behandling::getSaksopplysninger, BehandlingDto::setSaksopplysninger));

        TypeMap<BehandlingDto, Behandling> typeMapBehandlingInn = modelMapper.createTypeMap(BehandlingDto.class, Behandling.class);
        Converter<Set<SaksopplysningDokument>, Set<Saksopplysning>> dokumentTilSaksopplysning = ctx -> {
            Set opplysninger = new HashSet();
            if (ctx.getSource() != null) {
                ctx.getSource().forEach(x -> opplysninger.add(new Saksopplysning(x))); // ny saksopplysning ut fra dokument
            }
            return opplysninger;
        };
        typeMapBehandlingInn.addMappings(mapper -> mapper.using(dokumentTilSaksopplysning).map(BehandlingDto::getSaksopplysninger, Behandling::setSaksopplysninger));
    }

    @GET
    @Path("{id}")
    public BehandlingDto hentBehandling(@PathParam("id") Long id) {
        Behandling behandling = behandlingrepo.findOne(id);

        BehandlingDto behandlingDto = new BehandlingDto();
        modelMapper.map(behandling, behandlingDto);
        return behandlingDto;
    }

    @PATCH
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON_PATCH_JSON})
    public Response patchBehandling(@PathParam("id") long id, ObjectPatch patch)  {
        Behandling behandling = behandlingrepo.findOne(id);

        if (behandling == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            BehandlingDto behandlingDto = tilDto(behandling);

            BehandlingDto patchedDto;
            try {
                patchedDto = patch.apply(behandlingDto);
                modelMapper.map(patchedDto, behandling);
                behandlingrepo.save(behandling);
            } catch (ObjectPatchException e) {
                return Response.serverError().build();
            }
            return Response.ok(patchedDto).build();
        }
    }

    private BehandlingDto tilDto(Behandling behandling) {
        BehandlingDto behandlingDto = new BehandlingDto();
        modelMapper.map(behandling, behandlingDto);
        return behandlingDto;
    }

}
