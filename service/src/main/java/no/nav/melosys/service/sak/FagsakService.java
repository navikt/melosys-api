package no.nav.melosys.service.sak;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.Behandling.erBehandlingAvSedForespørsler;
import static no.nav.melosys.domain.Behandling.erBehandlingAvSøknad;
import static no.nav.melosys.metrics.MetrikkerNavn.SAKER_OPPRETTET;
import static no.nav.melosys.service.sak.SakstypeBehandlingstemaKobling.erGyldigBehandlingstemaForSakstype;

@Service
public class FagsakService {
    private static final Logger log = LoggerFactory.getLogger(FagsakService.class);
    private static final String FAGSAKID_PREFIX = "MEL-";

    private final FagsakRepository fagsakRepository;
    private final BehandlingService behandlingService;
    private final KontaktopplysningService kontaktopplysningService;
    private final OppgaveService oppgaveService;
    private final PersondataFasade persondataFasade;
    private final ProsessinstansService prosessinstansService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;
    private final Unleash unleash;

    private final Counter sakerOpprettet = Metrics.counter(SAKER_OPPRETTET);

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository,
                         BehandlingService behandlingService,
                         KontaktopplysningService kontaktopplysningService,
                         @Lazy OppgaveService oppgaveService,
                         PersondataFasade persondataFasade,
                         @Lazy ProsessinstansService prosessinstansService,
                         BehandlingsresultatService behandlingsresultatService,
                         MedlPeriodeService medlPeriodeService, Unleash unleash) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        this.kontaktopplysningService = kontaktopplysningService;
        this.oppgaveService = oppgaveService;
        this.persondataFasade = persondataFasade;
        this.prosessinstansService = prosessinstansService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
        this.unleash = unleash;
    }

    public Fagsak hentFagsak(String saksnummer) {
        return finnFagsakFraSaksnummer(saksnummer)
            .orElseThrow(() -> new IkkeFunnetException("Det finnes ingen fagsak med saksnummer: " + saksnummer));
    }

    public Optional<Fagsak> finnFagsakFraSaksnummer(String saksnummer) {
        return fagsakRepository.findBySaksnummer(saksnummer);
    }

    public Fagsak hentFagsakFraArkivsakID(Long arkivsakID) {
        return finnFagsakFraArkivsakID(arkivsakID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke fagsak for arkivsakID " + arkivsakID));
    }

    public Optional<Fagsak> finnFagsakFraArkivsakID(Long arkivsakID) {
        return fagsakRepository.findByGsakSaksnummer(arkivsakID);
    }

    public List<Fagsak> hentFagsakerMedAktør(Aktoersroller rolleType, String ident) {
        String aktørID = persondataFasade.hentAktørIdForIdent(ident);
        return fagsakRepository.findByRolleAndAktør(rolleType, aktørID);
    }

    @Transactional
    public void lagre(Fagsak sak) {
        if (sak.getSaksnummer() == null) {
            sak.setSaksnummer(hentNesteSaksnummer());
        }
        fagsakRepository.save(sak);
    }

    @Transactional
    public void bestillNySakOgBehandling(OpprettSakDto opprettSakDto) {
        validerOpprettSakDto(opprettSakDto);
        final Oppgave oppgave = validerOppgave(opprettSakDto.getOppgaveID());
        prosessinstansService.opprettProsessinstansNySak(
            oppgave.getJournalpostId(),
            opprettSakDto,
            erBehandlingAvSøknad(opprettSakDto.getBehandlingstema()) ? Behandlingstyper.SOEKNAD : Behandlingstyper.SED
        );
    }

    void validerOpprettSakDto(OpprettSakDto opprettSakDto) {
        final var sakstype = opprettSakDto.getSakstype();
        final var behandlingstema = opprettSakDto.getBehandlingstema();
        if (behandlingstema == null) {
            throw new FunksjonellException("Behandlingstema mangler for å opprette ny sak");
        } else if (!erBehandlingAvSøknad(behandlingstema)
            && !erBehandlingAvSedForespørsler(opprettSakDto.getBehandlingstema())) {
            throw new FunksjonellException("Kan ikke opprette ny sak med behandlingstema " + opprettSakDto.getBehandlingstema());
        } else if (!erGyldigBehandlingstemaForSakstype(sakstype, behandlingstema)) {
            throw new FunksjonellException("Behandlingstema " + behandlingstema + " er ikke gyldig for sakstype " + sakstype);
        } else if (behandlingstema == Behandlingstema.ARBEID_I_UTLANDET && !unleash.isEnabled("melosys.folketrygden.mvp")) {
            throw new FunksjonellException("Kan ikke opprette ny sak med behandlingstema " + behandlingstema);
        }

        boolean feilet = false;
        StringBuilder feilmeldingBuilder = new StringBuilder();

        if (erBehandlingAvSøknad(opprettSakDto.getBehandlingstema())) {
            final SøknadDto soknadDto = opprettSakDto.getSoknadDto();
            if (soknadDto == null) {
                throw new FunksjonellException("SoknadDto må ikke være null for å opprette en søknadbehandling.");
            }
            PeriodeDto periodeDto = soknadDto.getPeriode();
            if (periodeDto.getFom() == null) {
                feilet = true;
                feilmeldingBuilder.append("søknadsperiodes fra og med dato, ");
            }
            if (soknadDto.getLand() == null || soknadDto.getLand().isEmpty()) {
                feilet = true;
                feilmeldingBuilder.append("land, ");
            }
            if (feilet) {
                throw new FunksjonellException(feilmeldingBuilder.append("mangler for å opprette en søknadbehandling.").toString());
            }
            if (periodeDto.getTom() != null && periodeDto.getFom().isAfter(periodeDto.getTom())) {
                throw new FunksjonellException("Fra og med dato kan ikke være etter til og med dato.");
            }
        }
    }

    private Oppgave validerOppgave(String oppgaveID) {
        if (StringUtils.isEmpty(oppgaveID)) {
            throw new FunksjonellException("OppgaveID mangler.");
        }
        final Oppgave oppgave = oppgaveService.hentOppgaveMedOppgaveID(oppgaveID);
        if (!nySakKanOpprettesFraOppgavetype(oppgave.getOppgavetype())) {
            throw new FunksjonellException("Ny sak kan ikke opprettes på bakgrunn av oppgave med type: " + oppgave.getOppgavetype().getBeskrivelse());
        }
        if (StringUtils.isEmpty(oppgave.getJournalpostId())) {
            throw new FunksjonellException("Ny sak kan ikke opprettes fordi oppgave " + oppgaveID + " mangler journalpost med søknad.");
        }
        return oppgave;
    }

    private static boolean nySakKanOpprettesFraOppgavetype(Oppgavetyper oppgavetype) {
        return oppgavetype == Oppgavetyper.BEH_SAK_MK
            || oppgavetype == Oppgavetyper.BEH_SAK
            || oppgavetype == Oppgavetyper.BEH_SED;
    }

    // Sletter myndigheter som ikke ligger i oppgitt liste og legger til de som mangler.
    // Oppdaterer IKKE de som allerede finnes i database
    @Transactional
    public void oppdaterMyndigheter(String saksnummer, Collection<String> ider) {
        Fagsak fagsak = hentFagsak(saksnummer);
        fagsak.getAktører().removeIf(aktoer -> !ider.contains(aktoer.getInstitusjonId())
            && aktoer.getRolle() == Aktoersroller.MYNDIGHET);

        Collection<Aktoer> nyeMyndigheter = ider.stream()
            .map(id -> lagAktør(fagsak, Aktoersroller.MYNDIGHET, id))
            .collect(Collectors.toList());

        fagsak.getAktører().addAll(nyeMyndigheter);
        fagsakRepository.save(fagsak);
    }

    @Transactional
    public void leggTilAktør(String saksnummer, Aktoersroller aktørsrolle, String ID) {
        Fagsak fagsak = hentFagsak(saksnummer);

        Aktoer aktør = lagAktør(fagsak, aktørsrolle, ID);
        fagsak.getAktører().add(aktør);
        fagsakRepository.save(fagsak);
    }

    private static Aktoer lagAktør(Fagsak fagsak, Aktoersroller aktørsrolle, String ID) {
        Aktoer aktør = new Aktoer();
        aktør.setFagsak(fagsak);
        aktør.setRolle(aktørsrolle);
        switch (aktørsrolle) {
            case BRUKER:
                aktør.setAktørId(ID);
                break;
            case ARBEIDSGIVER:
            case REPRESENTANT:
                aktør.setOrgnr(ID);
                break;
            case MYNDIGHET:
                aktør.setInstitusjonId(ID);
                break;
            default:
                throw new IllegalStateException(aktørsrolle + " støttes ikke.");
        }
        return aktør;
    }

    /**
     * - Oppretter en ny fagsak med en ny behandling.
     * - Oppretter bruker, arbeidsgiver og representanter.
     * - Oppretter tom behandlingsresultat.
     */
    @Transactional
    public Fagsak nyFagsakOgBehandling(OpprettSakRequest opprettSakRequest) {
        Fagsak fagsak = new Fagsak();
        String saksnummer = hentNesteSaksnummer();
        fagsak.setSaksnummer(saksnummer);

        HashSet<Aktoer> aktører = new HashSet<>();

        Aktoer aktør = new Aktoer();
        aktør.setAktørId(opprettSakRequest.getAktørID());
        aktør.setUtenlandskPersonId(opprettSakRequest.getUtenlandskPersonId());
        aktør.setFagsak(fagsak);
        aktør.setRolle(Aktoersroller.BRUKER);
        aktører.add(aktør);

        String arbeidsgiver = opprettSakRequest.getArbeidsgiver();
        if (arbeidsgiver != null) {
            Aktoer aktørArbeidsgiver = new Aktoer();
            aktørArbeidsgiver.setOrgnr(arbeidsgiver);
            aktørArbeidsgiver.setFagsak(fagsak);
            aktørArbeidsgiver.setRolle(Aktoersroller.ARBEIDSGIVER);
            aktører.add(aktørArbeidsgiver);
        }

        Fullmektig fullmektig = opprettSakRequest.getFullmektig();
        if (fullmektig != null) {
            Aktoer aktørFullmektig = new Aktoer();
            aktørFullmektig.setOrgnr(fullmektig.getRepresentantID());
            aktørFullmektig.setFagsak(fagsak);
            aktørFullmektig.setRolle(Aktoersroller.REPRESENTANT);
            aktørFullmektig.setRepresenterer(fullmektig.getRepresenterer());
            aktører.add(aktørFullmektig);
        }

        Instant nå = Instant.now();

        fagsak.setType(opprettSakRequest.getSakstype());
        fagsak.setAktører(aktører);
        fagsak.setRegistrertDato(nå);
        fagsak.setEndretDato(nå);
        fagsak.setStatus(Saksstatuser.OPPRETTET);

        lagre(fagsak);

        List<Kontaktopplysning> kontaktopplysninger = opprettSakRequest.getKontaktopplysninger();
        if (CollectionUtils.isNotEmpty(kontaktopplysninger)) {
            kontaktopplysninger.forEach(opplysning -> kontaktopplysningService
                .lagEllerOppdaterKontaktopplysning(saksnummer, opplysning.getKontaktopplysningID().getOrgnr(),
                    opplysning.getKontaktOrgnr(), opplysning.getKontaktNavn(), opplysning.getKontaktTelefon()));
        }

        Behandlingstyper behandlingstype = opprettSakRequest.getBehandlingstype();
        Behandlingstema behandlingstema = opprettSakRequest.getBehandlingstema();
        String initierendeJournalpostId = opprettSakRequest.getInitierendeJournalpostId();
        String initierendeDokumentId = opprettSakRequest.getInitierendeDokumentId();
        Behandling behandling = behandlingService.nyBehandling(fagsak,
            Behandlingsstatus.OPPRETTET, behandlingstype, behandlingstema,
            initierendeJournalpostId, initierendeDokumentId);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        sakerOpprettet.increment();
        return fagsak;
    }

    private String hentNesteSaksnummer() {
        return FAGSAKID_PREFIX + fagsakRepository.hentNesteSekvensVerdi();
    }

    @Transactional
    public void avsluttSakSomBortfalt(Fagsak fagsak) {
        fagsak.getBehandlinger().forEach(behandling -> behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.getId(), Behandlingsresultattyper.HENLEGGELSE));

        fagsak.getBehandlinger().forEach(behandling -> {
            log.info("Setter behandling {} til {}", behandling.getId(), Behandlingsstatus.AVSLUTTET);
            behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        });
        log.info("Setter status på fagsak {} til {}", fagsak.getSaksnummer(), Saksstatuser.HENLAGT_BORTFALT);
        fagsak.setStatus(Saksstatuser.HENLAGT_BORTFALT);
        fagsakRepository.save(fagsak);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
    }

    //Brukes for å avslutte behandling (og dermed fagsak) fra frontend i manuelle sed-behandlinger
    @Transactional
    public void avsluttFagsakOgBehandlingValiderBehandlingstype(Fagsak fagsak, Behandling behandling) {
        Behandlingstema behandlingstema = behandling.getTema();
        if (!behandling.kanAvsluttesManuelt()) {
            throw new FunksjonellException("Behandlingstema " + behandlingstema + " kan ikke avsluttes manuelt");
        }

        Saksstatuser saksstatus = behandlingstema == Behandlingstema.IKKE_YRKESAKTIV
            ? Saksstatuser.LOVVALG_AVKLART : Saksstatuser.AVSLUTTET;
        avsluttFagsakOgBehandling(fagsak, saksstatus);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
    }

    public void avsluttFagsakOgBehandling(Fagsak fagsak, Saksstatuser saksstatus) {
        Behandling aktivBehandling = fagsak.hentAktivBehandling();
        if (aktivBehandling == null) {
            throw new FunksjonellException("Fagsak " + fagsak.getSaksnummer() + " har ingen aktiv behandling");
        } else {
            avsluttFagsakOgBehandling(fagsak, aktivBehandling, saksstatus);
        }
    }

    public void avsluttFagsakOgBehandling(Fagsak fagsak,
                                          Behandling behandling,
                                          Saksstatuser saksstatus) {
        if (!behandling.getFagsak().getSaksnummer().equals(fagsak.getSaksnummer())) {
            throw new FunksjonellException("Behandling " + behandling.getId()
                + " tilhører ikke fagsak " + fagsak.getSaksnummer());
        }
        oppdaterStatus(fagsak, saksstatus);
        behandlingService.avsluttBehandling(behandling.getId());
        log.info("Fagsak {} med behandling avsluttet", fagsak.getSaksnummer());
    }

    public void oppdaterStatus(Fagsak fagsak, Saksstatuser saksstatus) {
        fagsak.setStatus(saksstatus);
        fagsakRepository.save(fagsak);
    }

    @Transactional
    public long opprettNyVurderingBehandling(String saksnummer) {
        Fagsak fagsak = hentFagsak(saksnummer);
        Behandling behandling = fagsak.hentSistAktiveBehandling();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

        validerOpprettNyVurdering(behandling, behandlingsresultat);

        Behandlingstyper behandlingstype;
        if (behandling.erInaktiv()) {
            behandlingstype = Behandlingstyper.NY_VURDERING;
        } else {
            behandlingstype = behandling.getType();
        }

        Behandling replikertBehandling = behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingsstatus.OPPRETTET, behandlingstype);

        if (!behandling.erAvsluttet()) {
            behandlingService.avsluttBehandling(behandling.getId());
        }

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            replikertBehandling, replikertBehandling.getInitierendeJournalpostId(), fagsak.hentBrukerID(), SubjectHandler.getInstance().getUserID()
        );
        avsluttTidligereMedlPeriode(behandlingsresultat);
        return replikertBehandling.getId();
    }

    private void validerOpprettNyVurdering(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (behandling.erAktiv() && behandlingsresultat.erIkkeArtikkel16MedSendtAnmodningOmUnntak()) {
            throw new FunksjonellException("Kan ikke revurdere en aktiv behandling");
        } else if (behandling.erEndretPeriode()) {
            throw new FunksjonellException("Kan ikke revurdere en behandling av type " + Behandlingstyper.ENDRET_PERIODE.getBeskrivelse());
        }
    }

    private void avsluttTidligereMedlPeriode(Behandlingsresultat behandlingsresultat) {
        Collection<? extends PeriodeOmLovvalg> anmodningsperioder = behandlingsresultat.getAnmodningsperioder();
        Collection<? extends PeriodeOmLovvalg> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();

        Optional<Long> medlPeriodeID = Stream.concat(anmodningsperioder.stream(), lovvalgsperioder.stream())
            .map(PeriodeOmLovvalg::getMedlPeriodeID)
            .filter(Objects::nonNull)
            .findFirst();

        if (medlPeriodeID.isPresent()) {
            medlPeriodeService.avvisPeriode(medlPeriodeID.get());
        }
    }

}
