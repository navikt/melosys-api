package no.nav.melosys.tjenester.gui;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.aggregate.OppgaveAG;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.service.OppgaveService;
import no.nav.melosys.service.Oppgaveplukker;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.MinSakDto;
import no.nav.melosys.tjenester.gui.dto.PlukkOppgaveInnDto;
import no.nav.melosys.tjenester.gui.dto.PlukketOppgaveDto;
import no.nav.melosys.tjenester.gui.dto.TilbakeleggingDto;
import no.nav.melosys.tjenester.gui.dto.converter.OppgaverAGTilMineSakerDTOConverter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;


@Api(tags = {"oppgaver"})
@Path("/oppgaver")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class OppgaveTjeneste {

    private Oppgaveplukker oppgaveplukker;
    private OppgaveService oppgaveService;
    private ModelMapper modelMapper;

    @Autowired
    public OppgaveTjeneste(Oppgaveplukker oppgaveplukker, OppgaveService oppgaveService) {
        this.oppgaveplukker = oppgaveplukker;
        this.oppgaveService = oppgaveService;
    }

    @POST
    @Path("/plukk")
    @ApiOperation(value = "Plukker fra GSAK neste oppgave som saksbehandler skal arbeide med.")
    public Response plukkOppgave(PlukkOppgaveInnDto plukkDto) {
        String ident = SubjectHandler.getInstance().getUserID();

        Optional<Oppgave> plukket = oppgaveplukker.plukkOppgave(ident, plukkDto.getOppgavetype(), plukkDto.getSakstyper(), plukkDto.getBehandlingstyper());

        if (plukket.isPresent()) {
            Oppgave oppgave = plukket.get();

            PlukketOppgaveDto dto = new PlukketOppgaveDto();
            dto.setOppgaveID(oppgave.getOppgaveId());
            dto.setOppgavetype(oppgave.getOppgavetype().name());
            dto.setSaksnummer(oppgave.getSaksnummer());
            dto.setJournalpostID(oppgave.getDokumentId());

            return Response.ok(dto).build();
        } else {
            return Response.ok().build();
        }

    }

    @POST
    @ApiOperation(value = "Legger tilbake oppgaven med gitt oppgaveId i GSAK")
    public Response leggTilbakeOppgave(@ApiParam("Tilbakeleggingsinformasjon") TilbakeleggingDto tilbakelegging) {
        String ident = SubjectHandler.getInstance().getUserID();

        oppgaveplukker.leggTilbakeOppgave(tilbakelegging.getOppgaveId(), ident, tilbakelegging.getBegrunnelse());

        return Response.ok().build();
    }

    @GET
    @Path("{minesaker}")
    @ApiOperation(value = "hent mine saker.")
    public Response mineSaker() {
        List<OppgaveAG> oppgaveAGS = oppgaveService.hentMineSaker(SpringSubjectHandler.getInstance().getUserID());
        return Response.ok(mappeOppgaveDtoTilMinSak(oppgaveAGS)).build();
    }


    private List<MinSakDto> mappeOppgaveDtoTilMinSak(List<OppgaveAG> oppgaveAGList) {

        Function<OppgaveAG, MinSakDto> transformToMineSaker = new Function<OppgaveAG, MinSakDto>() {
            @Override
            public MinSakDto apply(OppgaveAG oppgaveAG) {
                MinSakDto dest = new MinSakDto();
                dest.setOppgaveId(oppgaveAG.getOppgave().getOppgaveId());
                dest.setDokumentID(oppgaveAG.getOppgave().getDokumentId());
                dest.setAktivTil(oppgaveAG.getOppgave().getAktivTil().toString());
                dest.setSammensattNavn(oppgaveAG.getPersonDokument().sammensattNavn);
                dest.setSaksnummer(oppgaveAG.getFagsak().getId());
                dest.setLand(OppgaverAGTilMineSakerDTOConverter.mappeLander(oppgaveAG.getSoeknadDokument()));
                dest.setSoknadsperiode(OppgaverAGTilMineSakerDTOConverter.mappeDato(oppgaveAG.getSoeknadDokument()));
                dest.setSaksTypeDto(OppgaverAGTilMineSakerDTOConverter.mappeSaksTypeOgBehandling(oppgaveAG.getFagsak()));
                return dest;
            }
        };
        return oppgaveAGList.stream().map(transformToMineSaker).collect(Collectors.<MinSakDto>toList());
    }

}
