package no.nav.melosys.tjenester.gui.fagsaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.sak.*;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import no.nav.security.token.support.core.api.Protected;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.util.MottatteOpplysningerUtils.hentLand;
import static no.nav.melosys.domain.util.MottatteOpplysningerUtils.hentPeriode;

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = {"fagsaker"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class FagsakController {
    private static final Logger log = LoggerFactory.getLogger(FagsakController.class);
    private static final String UKJENT_NAVN = "UKJENT";

    private final FagsakService fagsakService;
    private final OpprettSak opprettSak;
    private final EndreSakService endreSakService;
    private final Aksesskontroll aksesskontroll;
    private final MottatteOpplysningerService mottatteOpplysningerService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final PersondataFasade persondataFasade;
    private final SaksopplysningerService saksopplysningerService;
    private final OrganisasjonOppslagService organisasjonOppslagService;
    private final OpprettBehandlingForSak opprettBehandlingForSak;
    private final FerdigbehandleService ferdigbehandleService;

    public FagsakController(FagsakService fagsakService,
                            Aksesskontroll aksesskontroll,
                            MottatteOpplysningerService mottatteOpplysningerService,
                            OpprettSak opprettSak,
                            EndreSakService endreSakService,
                            BehandlingsresultatService behandlingsresultatService,
                            PersondataFasade persondataFasade,
                            SaksopplysningerService saksopplysningerService,
                            OrganisasjonOppslagService organisasjonOppslagService,
                            OpprettBehandlingForSak opprettBehandlingForSak,
                            FerdigbehandleService ferdigbehandleService) {
        this.fagsakService = fagsakService;
        this.aksesskontroll = aksesskontroll;
        this.mottatteOpplysningerService = mottatteOpplysningerService;
        this.opprettSak = opprettSak;
        this.endreSakService = endreSakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.persondataFasade = persondataFasade;
        this.saksopplysningerService = saksopplysningerService;
        this.organisasjonOppslagService = organisasjonOppslagService;
        this.opprettBehandlingForSak = opprettBehandlingForSak;
        this.ferdigbehandleService = ferdigbehandleService;
    }

    @GetMapping("/{saksnr}")
    @ApiOperation(value = "Henter en sak med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public ResponseEntity<FagsakDto> hentFagsak(@PathVariable("saksnr") String saksnummer) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(fagsak);
        FagsakDto fagsakDto = tilFagsakDto(fagsak);
        log.info("Henting av sak {} ({})", fagsakDto.getSaksnummer(), fagsakDto.getGsakSaksnummer());
        return ResponseEntity.ok(fagsakDto);
    }

    @PostMapping
    @ApiOperation(value = "Oppretter en ny sak.")
    public ResponseEntity<Void> opprettNySak(@RequestBody OpprettSakDto opprettSakDto) {
        if (opprettSakDto.getBrukerID() == null && opprettSakDto.getVirksomhetOrgnr() == null) {
            throw new FunksjonellException("BrukerID eller organisasjonsnummer trengs for å opprette en sak.");
        }
        if (opprettSakDto.getBrukerID() != null) {
            aksesskontroll.autoriserFolkeregisterIdent(opprettSakDto.getBrukerID());
        }

        if (opprettSakDto.getOppgaveID() == null) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto);
        } else {
            opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto);
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{saksnr}/behandlinger")
    @ApiOperation(value = "Oppretter en ny behandling for sak.")
    public ResponseEntity<Void> opprettNyBehandlingForSak(@PathVariable("saksnr") String saksnummer, @RequestBody OpprettSakDto opprettSakDto) {
        if (opprettSakDto.getBrukerID() != null) {
            aksesskontroll.autoriserFolkeregisterIdent(opprettSakDto.getBrukerID());
        }

        opprettBehandlingForSak.opprettBehandling(saksnummer, opprettSakDto);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{saksnr}")
    @ApiOperation(value = "Endre en sak.")
    public ResponseEntity<Void> endreFagsak(@PathVariable("saksnr") String saksnummer, @RequestBody EndreSakDto endreDto) {
        log.debug("Saksbehandler {} ber om å endre fagsak {} med sakstype {}, sakstema {}",
            SubjectHandler.getInstance().getUserID(), saksnummer, endreDto.getSakstype(), endreDto.getSakstema());
        aksesskontroll.autoriserSakstilgang(saksnummer);

        if (endreDto.getBehandlingstype() == Behandlingstyper.ÅRSAVREGNING && endreDto.getBehandlingID() != null) {
            endreSakService.endreÅrsavregningOppsummering(endreDto.getBehandlingID(), endreDto.getBehandlingsstatus(), endreDto.getMottaksdato());
        } else {
            endreSakService.endre(saksnummer, endreDto.getSakstype(), endreDto.getSakstema(), endreDto.getBehandlingstema(),
                endreDto.getBehandlingstype(), endreDto.getBehandlingsstatus(), endreDto.getMottaksdato());
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sok")
    @ApiOperation(
        value = "Søk etter saker på ident eller saksnummer eller orgnr",
        notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer. Saker knyttet til en organisasjon søkes via organisasjonsnummer."),
        response = FagsakOppsummeringDto.class,
        responseContainer = "List")
    public List<FagsakOppsummeringDto> hentFagsaker(@RequestBody FagsakSokDto fagsakSokDto) {

        if (StringUtils.isNotEmpty(fagsakSokDto.ident())) {
            aksesskontroll.auditAutoriserFolkeregisterIdent(fagsakSokDto.ident(), "Søk på person med ident. Oversikt over saker og behandlinger.");
            return tilFagsakOppsummeringDtoer(fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, fagsakSokDto.ident()));
        } else if (StringUtils.isNotEmpty(fagsakSokDto.saksnummer())) {
            Optional<Fagsak> fagsak = fagsakService.finnFagsakFraSaksnummer(fagsakSokDto.saksnummer());
            if (fagsak.isPresent()) {
                aksesskontroll.auditAutoriserSakstilgang(fagsak.get(), "Søk på sak med saksnummer. Oversikt over saker og behandlinger.");
                return tilFagsakOppsummeringDtoer(Collections.singletonList(fagsak.get()));
            }
        } else if (StringUtils.isNotEmpty(fagsakSokDto.orgnr())) {
            return tilFagsakOppsummeringDtoer(fagsakService.hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, fagsakSokDto.orgnr()));
        }

        return Collections.emptyList();
    }

    @PutMapping("/{behandlingID}/ferdigbehandle")
    @ApiOperation("Avslutt behandling med Ferdigbehandlet som resultat og oppdatere saksstatus")
    public ResponseEntity<Void> ferdigbehandleSak(@PathVariable("behandlingID") long behandlingID) {
        log.info("Saksbehandler {} ber om å ferdigbehandle behandling {}", SubjectHandler.getInstance().getUserID(), behandlingID);
        aksesskontroll.autoriserSkriv(behandlingID);

        ferdigbehandleService.ferdigbehandle(behandlingID);

        return ResponseEntity.noContent().build();
    }


    private FagsakDto tilFagsakDto(Fagsak fagsak) {
        FagsakDto fagsakDto = new FagsakDto();
        fagsakDto.setSaksnummer(fagsak.getSaksnummer());
        fagsakDto.setGsakSaksnummer(fagsak.getGsakSaksnummer());
        fagsakDto.setSakstema(fagsak.getTema());
        fagsakDto.setSakstype(fagsak.getType());
        fagsakDto.setSaksstatus(fagsak.getStatus());
        fagsakDto.setRegistrertDato(fagsak.getRegistrertDato());
        fagsakDto.setEndretDato(fagsak.getEndretDato());
        fagsakDto.setHovedpartRolle(fagsak.getHovedpartRolle());

        return fagsakDto;
    }

    private List<FagsakOppsummeringDto> tilFagsakOppsummeringDtoer(Iterable<Fagsak> saker) {
        List<FagsakOppsummeringDto> fagsakListe = new ArrayList<>();
        for (Fagsak fagsak : saker) {
            FagsakOppsummeringDto fagsakOppsummeringDto = new FagsakOppsummeringDto();
            fagsakOppsummeringDto.setSaksnummer(fagsak.getSaksnummer());
            fagsakOppsummeringDto.setSakstema(fagsak.getTema());
            fagsakOppsummeringDto.setSakstype(fagsak.getType());
            fagsakOppsummeringDto.setSaksstatus(fagsak.getStatus());
            fagsakOppsummeringDto.setOpprettetDato(fagsak.getRegistrertDato());
            fagsakOppsummeringDto.setHovedpartRolle(fagsak.getHovedpartRolle());

            List<Behandling> behandlinger = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato();

            List<BehandlingOversiktDto> behandlingOversiktDtoer = behandlinger.stream()
                .map(this::tilBehandlingOversiktDto)
                .toList();

            fagsakOppsummeringDto.setNavn(hentNavn(behandlinger));
            fagsakOppsummeringDto.setBehandlingOversikter(behandlingOversiktDtoer);
            fagsakListe.add(fagsakOppsummeringDto);
        }
        return fagsakListe;
    }

    private BehandlingOversiktDto tilBehandlingOversiktDto(Behandling behandling) {
        BehandlingOversiktDto behandlingOversiktDto = new BehandlingOversiktDto();
        if (behandling != null) {
            Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

            behandlingOversiktDto.setBehandlingID(behandling.getId());
            behandlingOversiktDto.setBehandlingsstatus(behandling.getStatus());
            behandlingOversiktDto.setBehandlingstype(behandling.getType());
            behandlingOversiktDto.setBehandlingstema(behandling.getTema());
            behandlingOversiktDto.setOpprettetDato(behandling.getRegistrertDato());
            behandlingOversiktDto.setBehandlingsresultattype(behandlingsresultat.getType());
            behandlingOversiktDto.setSvarFrist(behandling.getDokumentasjonSvarfristDato());

            setPeriodeOpplysninger(behandling, behandlingOversiktDto);
        }
        return behandlingOversiktDto;
    }

    private void setPeriodeOpplysninger(Behandling behandling, BehandlingOversiktDto behandlingOversiktDto) {
        saksopplysningerService.finnSedOpplysninger(behandling.getId()).ifPresentOrElse(sedDoument -> {
                var land = SoeknadslandDto.av(sedDoument.getLovvalgslandKode());
                behandlingOversiktDto.setLand(land);

                var periode = sedDoument.getLovvalgsperiode();
                behandlingOversiktDto.setSoknadsperiode(new PeriodeDto(periode.getFom(), periode.getTom()));
            },
            () -> {
                var mottatteOpplysninger = mottatteOpplysningerService.finnMottatteOpplysninger(behandling.getId());
                if (mottatteOpplysninger.isPresent()) {
                    var mottatteOpplysningerData = mottatteOpplysninger.get().getMottatteOpplysningerData();

                    var land = SoeknadslandDto.av(hentLand((mottatteOpplysningerData)));
                    behandlingOversiktDto.setLand(land);

                    var periode = hentPeriode(mottatteOpplysningerData);
                    if (periode != null) {
                        behandlingOversiktDto.setSoknadsperiode(new PeriodeDto(periode.getFom(), periode.getTom()));
                    }
                }
            });

        Behandlingsresultat behandlingsResultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(behandling.getId());

        behandlingsResultat.finnLovvalgsperiode().ifPresent(lovvalgsperiode -> {
            var periode = new PeriodeDto(lovvalgsperiode.getFom(), lovvalgsperiode.getTom());
            behandlingOversiktDto.setLovvalgsperiode(periode);
        });

        var periode = new PeriodeDto(
            behandlingsResultat.utledMedlemskapsperiodeFom(),
            behandlingsResultat.utledMedlemskapsperiodeTom());
        behandlingOversiktDto.setMedlemskapsperiode(periode);
    }

    private String hentNavn(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return UKJENT_NAVN;
        }
        var fagsak = behandlinger.get(0).getFagsak();
        var aktørId = fagsak.finnBrukersAktørID();
        if (aktørId != null) {
            return persondataFasade.hentSammensattNavn(persondataFasade.hentFolkeregisterident(aktørId));
        }
        var orgnr = fagsak.finnVirksomhetsOrgnr();
        if (orgnr != null) {
            return organisasjonOppslagService.hentOrganisasjon(orgnr).getNavn();
        }
        return UKJENT_NAVN;
    }
}
