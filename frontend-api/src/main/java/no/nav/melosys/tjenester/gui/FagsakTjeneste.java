package no.nav.melosys.tjenester.gui;

import java.util.*;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RegistreringsInfo;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.*;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import no.nav.security.token.support.core.api.Protected;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentPeriode;
import static no.nav.melosys.domain.util.BehandlingsgrunnlagUtils.hentSøknadsland;

@Protected
@RestController
@RequestMapping("/fagsaker")
@Api(tags = {"fagsaker"})
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class FagsakTjeneste {
    private static final Logger log = LoggerFactory.getLogger(FagsakTjeneste.class);
    private static final String UKJENT_SAMMENSATT_NAVN = "UKJENT";

    private final FagsakService fagsakService;
    private final OpprettNySakFraOppgave opprettNySakFraOppgave;
    private final Aksesskontroll aksesskontroll;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final HenleggFagsakService henleggFagsakService;
    private final PersondataFasade persondataFasade;
    private final SaksopplysningerService saksopplysningerService;
    private final UtpekingService utpekingService;
    private final VideresendSoknadService videresendSoknadService;

    @Autowired
    public FagsakTjeneste(FagsakService fagsakService, Aksesskontroll aksesskontroll, BehandlingsgrunnlagService behandlingsgrunnlagService,
                          HenleggFagsakService henleggFagsakService,
                          OpprettNySakFraOppgave opprettNySakFraOppgave, PersondataFasade persondataFasade,
                          SaksopplysningerService saksopplysningerService, UtpekingService utpekingService,
                          VideresendSoknadService videresendSoknadService) {
        this.fagsakService = fagsakService;
        this.aksesskontroll = aksesskontroll;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.henleggFagsakService = henleggFagsakService;
        this.opprettNySakFraOppgave = opprettNySakFraOppgave;
        this.persondataFasade = persondataFasade;
        this.saksopplysningerService = saksopplysningerService;
        this.utpekingService = utpekingService;
        this.videresendSoknadService = videresendSoknadService;
    }

    @GetMapping("{saksnr}")
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
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sok")
    @ApiOperation(
        value = "Søk etter saker på ident eller saksnummer",
        notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer."),
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
        }

        return Collections.emptyList();
    }

    @PostMapping("{saksnr}/henlegg")
    @ApiOperation(value = "Henlegger en fagsak")
    public ResponseEntity<Void> henleggFagsak(@PathVariable("saksnr") String saksnummer, @RequestBody HenleggelseDto henleggelseDto) {
        aksesskontroll.autoriserSakstilgang(saksnummer);
        henleggFagsakService.henleggFagsak(saksnummer, henleggelseDto.begrunnelseKode(), henleggelseDto.fritekst());
        return ResponseEntity.ok().build();
    }

    @PostMapping("{saksnr}/henlegg-videresend")
    @ApiOperation(value = "Videresender søknad for en gitt behandling")
    public ResponseEntity<Void> videresend(@PathVariable("saksnr") String saksnummer,
                                     @RequestBody VideresendDto videresendDto) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(fagsak);

        if (CollectionUtils.isEmpty(videresendDto.getVedlegg())) {
            throw new FunksjonellException("Kan ikke videresende søknad uten vedlegg!");
        }

        videresendSoknadService.videresend(saksnummer,
            videresendDto.getMottakerinstitusjon(),
            videresendDto.getFritekst(),
            videresendDto.getVedlegg().stream().map(
                v -> new DokumentReferanse(v.journalpostID(), v.dokumentID())).collect(
                Collectors.toUnmodifiableSet())
        );
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "{saksnr}/avsluttsaksombortfalt", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Henlegger en fagsak i Melosys som bortfalt, fordi den ikke skal behandles i Melosys")
    public ResponseEntity<Void> avsluttSakSomBortfalt(@PathVariable("saksnr") String saksnummer) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(fagsak);

        henleggFagsakService.henleggSomBortfalt(fagsak);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "{saksnr}/avslutt", consumes = MediaType.TEXT_PLAIN_VALUE, produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ApiOperation(value = "Brukes for å avslutte manuelle behandlinger. " +
        "Gyldige behandlingstyper er VURDER_TRYGDETID, ØVRIGE_SED og SOEKNAD_IKKE_YRKESAKTIVE", produces = MediaType.TEXT_PLAIN_VALUE, consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Void> avsluttSakManuelt(@PathVariable("saksnr") String saksnummer) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(fagsak);
        fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, fagsak.hentAktivBehandling());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{saksnummer}/utpek")
    @ApiOperation(value = "Utpeker lovvalgsland for gitt fagsak")
    public ResponseEntity<Void> utpekLovvalgsland(@PathVariable("saksnummer") String saksnummer,
                                            @RequestBody UtpekDto utpekDto) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        aksesskontroll.autoriserSakstilgang(fagsak);

        utpekingService.utpekLovvalgsland(
            fagsak,
            utpekDto.mottakerinstitusjoner(),
            utpekDto.fritekstSed(),
            utpekDto.fritekstBrev()
        );

        return ResponseEntity.noContent().build();
    }

    @ApiOperation(value = "Korrigerer eller omgjør et vedtak eller en anmodning til utenlandsk myndighet " +
        "for en sak ved å opprette en ny behandling basert på den siste endrede behandling")
    @PostMapping("/{saksnummer}/revurder")
    public ResponseEntity<RevurderingOpprettetDto> revurderSisteBehandling(@PathVariable("saksnummer") String saksnummer) {
        aksesskontroll.autoriserSakstilgang(saksnummer);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        return ResponseEntity.ok(new RevurderingOpprettetDto(behandlingID));
    }

    private FagsakDto tilFagsakDto(Fagsak fagsak) {
        FagsakDto fagsakDto = new FagsakDto();
        fagsakDto.setSaksnummer(fagsak.getSaksnummer());
        fagsakDto.setGsakSaksnummer(fagsak.getGsakSaksnummer());
        fagsakDto.setSakstype(fagsak.getType());
        fagsakDto.setSaksstatus(fagsak.getStatus());
        fagsakDto.setRegistrertDato(fagsak.getRegistrertDato());
        fagsakDto.setEndretDato(fagsak.getEndretDato());

        return fagsakDto;
    }

    private List<FagsakOppsummeringDto> tilFagsakOppsummeringDtoer(Iterable<Fagsak> saker) {
        List<FagsakOppsummeringDto> fagsakListe = new ArrayList<>();
        for (Fagsak fagsak : saker) {
            FagsakOppsummeringDto fagsakOppsummeringDto = new FagsakOppsummeringDto();
            fagsakOppsummeringDto.setSaksnummer(fagsak.getSaksnummer());
            fagsakOppsummeringDto.setSakstype(fagsak.getType());
            fagsakOppsummeringDto.setSaksstatus(fagsak.getStatus());
            fagsakOppsummeringDto.setOpprettetDato(fagsak.getRegistrertDato());

            List<Behandling> behandlinger = fagsak.getBehandlinger();

            List<BehandlingOversiktDto> behandlingOversiktDtoer = behandlinger.stream()
                .sorted(Comparator.comparing(RegistreringsInfo::getRegistrertDato).reversed())
                .map(this::tilBehandlingOversiktDto)
                .toList();

            fagsakOppsummeringDto.setSammensattNavn(hentSammensattNavn(behandlinger));
            fagsakOppsummeringDto.setBehandlingOversikter(behandlingOversiktDtoer);
            fagsakListe.add(fagsakOppsummeringDto);
        }
        return fagsakListe;
    }

    private BehandlingOversiktDto tilBehandlingOversiktDto(Behandling behandling) {
        BehandlingOversiktDto behandlingOversiktDto = new BehandlingOversiktDto();
        if (behandling != null) {
            behandlingOversiktDto.setBehandlingID(behandling.getId());
            behandlingOversiktDto.setBehandlingsstatus(behandling.getStatus());
            behandlingOversiktDto.setBehandlingstype(behandling.getType());
            behandlingOversiktDto.setBehandlingstema(behandling.getTema());
            behandlingOversiktDto.setOpprettetDato(behandling.getRegistrertDato());

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

    private String hentSammensattNavn(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return UKJENT_SAMMENSATT_NAVN;
        }
        final String fnr = persondataFasade.hentFolkeregisterident(behandlinger.get(0).getFagsak().hentAktørID());
        return persondataFasade.hentSammensattNavn(fnr);
    }
}
