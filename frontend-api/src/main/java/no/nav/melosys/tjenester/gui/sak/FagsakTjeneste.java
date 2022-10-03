package no.nav.melosys.tjenester.gui.sak;

import java.util.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
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

import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentPeriode;
import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentSøknadsland;

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = {"fagsaker"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class FagsakTjeneste {
    private static final Logger log = LoggerFactory.getLogger(FagsakTjeneste.class);
    private static final String UKJENT_NAVN = "UKJENT";

    private final FagsakService fagsakService;
    private final OpprettNySakFraOppgave opprettNySakFraOppgave;
    private final EndreSakService endreSakService;
    private final Aksesskontroll aksesskontroll;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final PersondataFasade persondataFasade;
    private final SaksopplysningerService saksopplysningerService;
    private final OrganisasjonOppslagService organisasjonOppslagService;

    public FagsakTjeneste(FagsakService fagsakService, Aksesskontroll aksesskontroll, BehandlingsgrunnlagService behandlingsgrunnlagService,
                          OpprettNySakFraOppgave opprettNySakFraOppgave, EndreSakService endreSakService,
                          BehandlingsresultatService behandlingsresultatService, PersondataFasade persondataFasade,
                          SaksopplysningerService saksopplysningerService, OrganisasjonOppslagService organisasjonOppslagService) {
        this.fagsakService = fagsakService;
        this.aksesskontroll = aksesskontroll;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.opprettNySakFraOppgave = opprettNySakFraOppgave;
        this.endreSakService = endreSakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.persondataFasade = persondataFasade;
        this.saksopplysningerService = saksopplysningerService;
        this.organisasjonOppslagService = organisasjonOppslagService;
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

    @PostMapping("/opprett")
    @ApiOperation(value = "Oppretter en sak med tilhørende behandling.")
    public ResponseEntity<Void> opprettFagsak(@RequestBody OpprettSakDto opprettSakDto) {
        if (opprettSakDto.getBrukerID() == null) {
            throw new FunksjonellException("BrukerID trengs for å opprette en sak.");
        }
        aksesskontroll.autoriserFolkeregisterIdent(opprettSakDto.getBrukerID());
        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{saksnr}/endre")
    @ApiOperation(value = "Endre en sak.")
    public ResponseEntity<Void> endreFagsak(@PathVariable("saksnr") String saksnummer, @RequestBody EndreSakDto endreSakDto) {
        log.debug("Saksbehandler {} ber om å endre fagsak {} med sakstype {}, sakstema {}",
            SubjectHandler.getInstance().getUserID(), saksnummer, endreSakDto.sakstype(), endreSakDto.sakstema());
        aksesskontroll.autoriserSakstilgang(saksnummer);
        endreSakService.endre(saksnummer, endreSakDto.sakstype(), endreSakDto.sakstema());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{saksnr}/mulige-sakstyper")
    @ApiOperation(value = "Hent mulige nye sakstype for en behandling")
    public ResponseEntity<Collection<Sakstyper>> hentMuligeSakstyper(@PathVariable("saksnr") String saksnummer) {
        log.debug("Saksbehandler {} ber om å hente mulige nye sakstema for fagsak {}.", SubjectHandler.getInstance().getUserID(), saksnummer);
        aksesskontroll.autoriserSakstilgang(saksnummer);

        return ResponseEntity.ok(fagsakService.hentMuligeSakstyper(saksnummer));
    }

    @GetMapping("{saksnr}/mulige-sakstemaer")
    @ApiOperation(value = "Hent mulige nye sakstema for en behandling")
    public ResponseEntity<Collection<Sakstemaer>> hentMuligeSakstemaer(@PathVariable("saksnr") String saksnummer,
                                                                       @RequestParam("sakstype") Sakstyper sakstype) {
        log.debug("Saksbehandler {} ber om å hente mulige nye sakstema for fagsak {}.",
                  SubjectHandler.getInstance().getUserID(), saksnummer);
        aksesskontroll.autoriserSakstilgang(saksnummer);

        return ResponseEntity.ok(fagsakService.hentMuligeSakstemaer(saksnummer, sakstype));
    }

    @PostMapping("/sok")
    @ApiOperation(
        value = "Søk etter saker på ident eller saksnummer eller orgnr",
        notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer. Saker knyttet til en organisasjon søkes via organisasjonsnummer."),
        response = FagsakOppsummeringDto.class,
        responseContainer = "List")
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
        if (behandling.erBehandlingAvSøknad()) {
            behandlingsgrunnlagService.finnBehandlingsgrunnlag(behandling.getId())
                .map(Behandlingsgrunnlag::getBehandlingsgrunnlagdata).ifPresent(grunnlagData -> {
                    SoeknadslandDto land = SoeknadslandDto.av(hentSøknadsland((grunnlagData)));
                    behandlingOversiktDto.setLand(land);
                    Periode periode = hentPeriode(grunnlagData);
                    if (periode != null) {
                        behandlingOversiktDto.setPeriode(new PeriodeDto(periode.getFom(), periode.getTom()));
                    }
                });
        } else {
            saksopplysningerService.finnSedOpplysninger(behandling.getId()).ifPresent(sedDokument -> {
                SoeknadslandDto land = SoeknadslandDto.av(sedDokument.getLovvalgslandKode());
                behandlingOversiktDto.setLand(land);
                behandlingOversiktDto.setPeriode(new PeriodeDto(
                    sedDokument.getLovvalgsperiode().getFom(), sedDokument.getLovvalgsperiode().getTom())
                );
            });
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
