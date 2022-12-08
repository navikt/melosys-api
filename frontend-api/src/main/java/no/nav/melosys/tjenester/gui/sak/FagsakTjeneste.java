package no.nav.melosys.tjenester.gui.sak;

import java.util.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.util.MottatteOpplysningerUtils.hentPeriode;
import static no.nav.melosys.domain.util.MottatteOpplysningerUtils.hentSøknadsland;

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = {"fagsaker"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class FagsakTjeneste {
    private static final Logger log = LoggerFactory.getLogger(FagsakTjeneste.class);
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
    private final Unleash unleash;

    public FagsakTjeneste(FagsakService fagsakService, Aksesskontroll aksesskontroll, MottatteOpplysningerService mottatteOpplysningerService,
                          OpprettSak opprettSak, EndreSakService endreSakService,
                          BehandlingsresultatService behandlingsresultatService, PersondataFasade persondataFasade,
                          Unleash unleash,
                          SaksopplysningerService saksopplysningerService, OrganisasjonOppslagService organisasjonOppslagService,
                          OpprettBehandlingForSak opprettBehandlingForSak) {
        this.fagsakService = fagsakService;
        this.aksesskontroll = aksesskontroll;
        this.mottatteOpplysningerService = mottatteOpplysningerService;
        this.opprettSak = opprettSak;
        this.endreSakService = endreSakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.persondataFasade = persondataFasade;
        this.saksopplysningerService = saksopplysningerService;
        this.organisasjonOppslagService = organisasjonOppslagService;
        this.unleash = unleash;
        this.opprettBehandlingForSak = opprettBehandlingForSak;

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

        if (opprettSakDto.getOppgaveID() == null && unleash.isEnabled("melosys.ny_opprett_sak")) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto);
        } else {
            opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto);
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{saksnr}/behandlinger")
    @ApiOperation(value = "Oppretter en ny behandling for sak.")
    public ResponseEntity<Void> opprettNyBehandlingForSak(@PathVariable("saksnr") String saksnummer, @RequestBody OpprettSakDto opprettSakDto) {
        if (!unleash.isEnabled("melosys.ny_opprett_sak")) {
            throw new FunksjonellException("Beklager, denne funksjonen er ikke støttet enda. Toggle melosys.ny_opprett_sak er disabled.");
        }
        if (opprettSakDto.getBrukerID() != null) {
            aksesskontroll.autoriserFolkeregisterIdent(opprettSakDto.getBrukerID());
        }

        opprettBehandlingForSak.opprettBehandling(saksnummer, opprettSakDto);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{saksnr}/endre")
    @ApiOperation(value = "Endre en sak.")
    public ResponseEntity<Void> endreFagsak(@PathVariable("saksnr") String saksnummer, @RequestBody EndreSakDto endreDto) {
        log.debug("Saksbehandler {} ber om å endre fagsak {} med sakstype {}, sakstema {}",
            SubjectHandler.getInstance().getUserID(), saksnummer, endreDto.getSakstype(), endreDto.getSakstema());
        aksesskontroll.autoriserSakstilgang(saksnummer);
        endreSakService.endre(saksnummer, endreDto.getSakstype(), endreDto.getSakstema(), endreDto.getBehandlingstema(),
            endreDto.getBehandlingstype(), endreDto.getBehandlingsstatus(), endreDto.getBehandlingsfrist());
        return ResponseEntity.noContent().build();
    }

    @Deprecated(since = "melosys.behandle_alle_saker")
    @GetMapping("{saksnr}/mulige-sakstyper")
    @ApiOperation(value = "Hent mulige nye sakstype for en behandling")
    public ResponseEntity<Collection<Sakstyper>> hentMuligeSakstyper(@PathVariable("saksnr") String saksnummer) {
        log.debug("Saksbehandler {} ber om å hente mulige nye sakstema for fagsak {}.", SubjectHandler.getInstance().getUserID(), saksnummer);
        aksesskontroll.autoriserSakstilgang(saksnummer);

        return ResponseEntity.ok(fagsakService.hentMuligeSakstyper());
    }

    @Deprecated(since = "melosys.behandle_alle_saker")
    @GetMapping("{saksnr}/mulige-sakstemaer")
    @ApiOperation(value = "Hent mulige nye sakstema for en behandling")
    public ResponseEntity<Collection<Sakstemaer>> hentMuligeSakstemaer(@PathVariable("saksnr") String saksnummer,
                                                                       @RequestParam("sakstype") Sakstyper sakstype) {
        log.debug("Saksbehandler {} ber om å hente mulige nye sakstema for fagsak {}.",
            SubjectHandler.getInstance().getUserID(), saksnummer);
        aksesskontroll.autoriserSakstilgang(saksnummer);

        return ResponseEntity.ok(fagsakService.hentMuligeSakstemaer());
    }

    @PostMapping("/sok")
    @ApiOperation(
        value = "Søk etter saker på ident eller saksnummer eller orgnr",
        notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer. Saker knyttet til en organisasjon søkes via organisasjonsnummer."),
        response = FagsakOppsummeringDto.class,
        responseContainer = "List")
    @Transactional
    public List<FagsakOppsummeringDto> hentFagsaker(@RequestBody FagsakSokDto fagsakSokDto) {

        if (StringUtils.isNotEmpty(fagsakSokDto.ident())) {
            aksesskontroll.autoriserFolkeregisterIdent(fagsakSokDto.ident());
            return tilFagsakOppsummeringDtoer(fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, fagsakSokDto.ident()));
        } else if (StringUtils.isNotEmpty(fagsakSokDto.saksnummer())) {
            Optional<Fagsak> fagsak = fagsakService.finnFagsakFraSaksnummer(fagsakSokDto.saksnummer());
            if (fagsak.isPresent()) {
                aksesskontroll.autoriserSakstilgang(fagsak.get());
                return tilFagsakOppsummeringDtoer(Collections.singletonList(fagsak.get()));
            }
        } else if (StringUtils.isNotEmpty(fagsakSokDto.orgnr())) {
            return tilFagsakOppsummeringDtoer(fagsakService.hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, fagsakSokDto.orgnr()));
        }

        return Collections.emptyList();
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker
     */
    @Deprecated
    @ApiOperation(value = "Korrigerer eller omgjør et vedtak eller en anmodning til utenlandsk myndighet " +
        "for en sak ved å opprette en ny behandling basert på den siste endrede behandling")
    @PostMapping("/{saksnummer}/revurder")
    public ResponseEntity<RevurderingOpprettetDto> revurderSisteBehandling(@PathVariable("saksnummer") String saksnummer) {
        aksesskontroll.autoriserSakstilgang(saksnummer);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        return ResponseEntity.ok(new RevurderingOpprettetDto(behandlingID));
    }

    @PutMapping("/{saksnummer}/ferdigbehandle")
    @ApiOperation("Avslutt behandling med Ferdigbehandlet som resultat og oppdatere saksstatus")
    public ResponseEntity<Void> ferdigbehandleSak(@PathVariable("saksnummer") String saksnummer) {
        log.info("Saksbehandler {} ber om å avslutte aktiv behandling og oppdatere saksstatus på {}", SubjectHandler.getInstance().getUserID(), saksnummer);
        aksesskontroll.autoriserSakstilgang(saksnummer);

        fagsakService.ferdigbehandleSak(saksnummer);

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
        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
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

                        var land = SoeknadslandDto.av(hentSøknadsland((mottatteOpplysningerData)));
                        behandlingOversiktDto.setLand(land);

                        var periode = hentPeriode(mottatteOpplysningerData);
                        behandlingOversiktDto.setSoknadsperiode(new PeriodeDto(periode.getFom(), periode.getTom()));
                    }
                });

            Behandlingsresultat behandlingsResultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

            behandlingsResultat.finnLovvalgsperiode().ifPresent(lovvalgsperiode -> {
                var periode = new PeriodeDto(lovvalgsperiode.getFom(), lovvalgsperiode.getTom());
                behandlingOversiktDto.setLovvalgsperiode(periode);
            });
        } else {
            if (behandling.erBehandlingAvSøknadGammel()) {
                mottatteOpplysningerService.finnMottatteOpplysninger(behandling.getId())
                    .map(MottatteOpplysninger::getMottatteOpplysningerData).ifPresent(grunnlagData -> {
                        SoeknadslandDto land = SoeknadslandDto.av(hentSøknadsland((grunnlagData)));
                        behandlingOversiktDto.setLand(land);
                        Periode periode = hentPeriode(grunnlagData);
                        if (periode != null) {
                            behandlingOversiktDto.setSoknadsperiode(new PeriodeDto(periode.getFom(), periode.getTom()));
                        }
                    });
            } else {
                saksopplysningerService.finnSedOpplysninger(behandling.getId()).ifPresent(sedDokument -> {
                    SoeknadslandDto land = SoeknadslandDto.av(sedDokument.getLovvalgslandKode());
                    behandlingOversiktDto.setLand(land);
                    behandlingOversiktDto.setSoknadsperiode(new PeriodeDto(
                        sedDokument.getLovvalgsperiode().getFom(), sedDokument.getLovvalgsperiode().getTom())
                    );
                });
            }
        }
    }

    private String hentNavn(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return UKJENT_NAVN;
        }
        var fagsak = behandlinger.get(0).getFagsak();
        var aktørId = fagsak.finnBrukersAktørID();
        if (aktørId.isPresent()) {
            return persondataFasade.hentSammensattNavn(persondataFasade.hentFolkeregisterident(aktørId.get()));
        }
        var orgnr = fagsak.finnVirksomhetsOrgnr();
        if (orgnr.isPresent()) {
            return organisasjonOppslagService.hentOrganisasjon(orgnr.get()).getNavn();
        }
        return UKJENT_NAVN;
    }
}
