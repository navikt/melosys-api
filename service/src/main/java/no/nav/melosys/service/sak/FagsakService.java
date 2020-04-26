package no.nav.melosys.service.sak;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.Behandling.erBehandlingAvSøknad;
import static no.nav.melosys.metrics.MetrikkerNavn.SAKER_OPPRETTET;

@Service
public class FagsakService {
    private static final Logger log = LoggerFactory.getLogger(FagsakService.class);
    private static final String FAGSAKID_PREFIX = "MEL-";

    private final FagsakRepository fagsakRepository;
    private final BehandlingService behandlingService;
    private final KontaktopplysningService kontaktopplysningService;
    private final OppgaveService oppgaveService;
    private final TpsFasade tpsFasade;
    private final ProsessinstansService prosessinstansService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    private final Counter sakerOpprettet = Metrics.counter(SAKER_OPPRETTET);

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository,
                         BehandlingService behandlingService,
                         KontaktopplysningService kontaktopplysningService,
                         @Lazy OppgaveService oppgaveService,
                         TpsFasade tpsFasade,
                         @Lazy ProsessinstansService prosessinstansService,
                         BehandlingsresultatService behandlingsresultatService,
                         MedlPeriodeService medlPeriodeService) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingService = behandlingService;
        this.kontaktopplysningService = kontaktopplysningService;
        this.oppgaveService = oppgaveService;
        this.tpsFasade = tpsFasade;
        this.prosessinstansService = prosessinstansService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void henleggFagsak(String saksnummer, String begrunnelseKodeString, String fritekst) throws TekniskException, FunksjonellException {
        Fagsak fagsak = hentFagsak(saksnummer);
        if (fagsak.getBehandlinger().isEmpty()) {
            throw new TekniskException("Fagsak med saksnummer " + saksnummer + " har ingen tilknyttede behandlinger.");
        }

        Henleggelsesgrunner begrunnelseKode;
        try {
            begrunnelseKode = Henleggelsesgrunner.valueOf(begrunnelseKodeString.toUpperCase());
        } catch (java.lang.IllegalArgumentException iae) {
            throw new TekniskException(begrunnelseKodeString.toUpperCase() + " er ingen gyldig henleggelsesgrunn");
        }

        Behandling sisteIkkeAvsluttedeBehandling = getSisteIkkeAvsluttedeBehandling(fagsak);
        prosessinstansService.opprettProsessinstansHenleggSak(sisteIkkeAvsluttedeBehandling, begrunnelseKode, fritekst);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(sisteIkkeAvsluttedeBehandling.getFagsak().getSaksnummer());
    }

    public Fagsak hentFagsak(String saksnummer) throws IkkeFunnetException {
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak == null) {
            throw new IkkeFunnetException("Det finnes ingen fagsak med saksnummer: " + saksnummer);
        }
        return fagsak;
    }

    public Fagsak hentFagsakFraGsakSaksnummer(Long gsakSaksnummer) throws IkkeFunnetException {
        return finnFagsakFraGsakSaksnummer(gsakSaksnummer)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke fagsak for gsakSaksnummer " + gsakSaksnummer));
    }

    public Optional<Fagsak> finnFagsakFraGsakSaksnummer(Long gsakSaksnummer) {
        return fagsakRepository.findByGsakSaksnummer(gsakSaksnummer);
    }

    public List<Fagsak> hentFagsakerMedAktør(Aktoersroller rolleType, String ident) throws IkkeFunnetException {
        String aktørID = tpsFasade.hentAktørIdForIdent(ident);
        return fagsakRepository.findByRolleAndAktør(rolleType, aktørID);
    }

    @Transactional
    public void lagre(Fagsak sak) {
        if (sak.getSaksnummer() == null) {
            sak.setSaksnummer(hentNesteSaksnummer());
        }
        fagsakRepository.save(sak);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void bestillNySakOgBehandling(OpprettSakDto opprettSakDto) throws FunksjonellException, TekniskException {
        validerOpprettSakDto(opprettSakDto);
        final Oppgave oppgave = validerOppgave(opprettSakDto.getOppgaveID());
        prosessinstansService.opprettProsessinstansNySak(oppgave.getJournalpostId(), opprettSakDto);
    }

    void validerOpprettSakDto(OpprettSakDto opprettSakDto) throws FunksjonellException {
        boolean feilet = false;
        StringBuilder feilmeldingBuilder = new StringBuilder();
        if (opprettSakDto.getBehandlingstema() == null) {
            feilet = true;
            feilmeldingBuilder.append("behandlingstema, ");
        }
        if (feilet) {
            throw new FunksjonellException(feilmeldingBuilder.append("mangler for å opprette en ny sak.").toString());
        }
        if (erBehandlingAvSøknad(opprettSakDto.getBehandlingstema().getKode())) {
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

    private Oppgave validerOppgave(String oppgaveID) throws FunksjonellException, TekniskException {
        if (StringUtils.isEmpty(oppgaveID)) {
            throw new FunksjonellException("OppgaveID mangler.");
        }
        final Oppgave oppgave = oppgaveService.hentOppgaveMedOppgaveID(oppgaveID);
        if (oppgave.getOppgavetype() != Oppgavetyper.BEH_SAK_MK && oppgave.getOppgavetype() != Oppgavetyper.BEH_SAK) {
            throw new FunksjonellException("Ny sak kan ikke opprettes på bakgrunn av oppgave med type: " + oppgave.getOppgavetype());
        }
        if (StringUtils.isEmpty(oppgave.getJournalpostId())) {
            throw new FunksjonellException("Ny sak kan ikke opprettes fordi oppgave " + oppgaveID + " mangler journalpost med søknad.");
        }
        return oppgave;
    }

    // Sletter myndigheter som ikke ligger i oppgitt liste og legger til de som mangler.
    // Oppdaterer IKKE de som allerede finnes i database
    @Transactional
    public void oppdaterMyndigheter(String saksnummer, Collection<String> ider) throws IkkeFunnetException {
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
    public void leggTilAktør(String saksnummer, Aktoersroller aktørsrolle, String ID) throws IkkeFunnetException {
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
    @Transactional(rollbackFor = MelosysException.class)
    public Fagsak nyFagsakOgBehandling(OpprettSakRequest opprettSakRequest) throws FunksjonellException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = hentNesteSaksnummer();
        fagsak.setSaksnummer(saksnummer);

        HashSet<Aktoer> aktører = new HashSet<>();

        Aktoer aktør = new Aktoer();
        aktør.setAktørId(opprettSakRequest.getAktørID());
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

        String representant = opprettSakRequest.getRepresentant();
        Representerer representantRepresenterer = opprettSakRequest.getRepresentantRepresenterer();
        if (representant != null) {
            Aktoer aktørRepresentant = new Aktoer();
            aktørRepresentant.setOrgnr(representant);
            aktørRepresentant.setFagsak(fagsak);
            aktørRepresentant.setRolle(Aktoersroller.REPRESENTANT);
            aktørRepresentant.setRepresenterer(representantRepresenterer);
            aktører.add(aktørRepresentant);
        }

        Instant nå = Instant.now();

        fagsak.setType(opprettSakRequest.getSakstype() == null ? Sakstyper.UKJENT : opprettSakRequest.getSakstype());
        fagsak.setAktører(aktører);
        fagsak.setRegistrertDato(nå);
        fagsak.setEndretDato(nå);
        fagsak.setStatus(Saksstatuser.OPPRETTET);

        lagre(fagsak);

        String representantKontaktperson = opprettSakRequest.getRepresentantKontaktperson();
        if (representantKontaktperson != null) {
            if (representant == null) {
                throw new FunksjonellException("Kontaktopplysninger kan ikke lagres uten orgnr.");
            } else {
                kontaktopplysningService.lagEllerOppdaterKontaktopplysning(saksnummer, representant, null, representantKontaktperson);
            }
        }

        Behandlingstyper behandlingstype = opprettSakRequest.getBehandlingstype();
        Behandlingstema behandlingstema = opprettSakRequest.getBehandlingstema();
        String initierendeJournalpostId = opprettSakRequest.getInitierendeJournalpostId();
        String initierendeDokumentId = opprettSakRequest.getInitierendeDokumentId();
        Behandling behandling = behandlingService.nyBehandling(fagsak, Behandlingsstatus.OPPRETTET, behandlingstype, behandlingstema, initierendeJournalpostId, initierendeDokumentId);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        sakerOpprettet.increment();
        return fagsak;
    }

    private String hentNesteSaksnummer() {
        return FAGSAKID_PREFIX + fagsakRepository.hentNesteSekvensVerdi();
    }

    private Behandling getSisteIkkeAvsluttedeBehandling(Fagsak fagsak) {
        return fagsak.getBehandlinger()
            .stream()
            .filter(behandling -> behandling.getStatus() != Behandlingsstatus.AVSLUTTET)
            .max(Comparator.comparing(RegistreringsInfo::getRegistrertDato))
            .orElseThrow(() -> new IllegalStateException("Sak " + fagsak.getSaksnummer() + " har ingen behandlinger eller bare avsluttede behandlinger."));
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void avsluttSakSomBortfalt(Fagsak fagsak) throws FunksjonellException, TekniskException {
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
    @Transactional(rollbackFor = MelosysException.class)
    public void avsluttFagsakOgBehandlingValiderBehandlingstype(Fagsak fagsak, Behandling behandling) throws FunksjonellException, TekniskException {
        Behandlingstema behandlingstema = behandling.getTema();
        if (behandlingstema != Behandlingstema.IKKE_YRKESAKTIV
            && behandlingstema != Behandlingstema.ØVRIGE_SED
            && behandlingstema != Behandlingstema.TRYGDETID) {
            throw new FunksjonellException("Behandlingstema " + behandlingstema + " kan ikke avsluttes manuelt");
        }

        Saksstatuser saksstatus = behandlingstema == Behandlingstema.IKKE_YRKESAKTIV ? Saksstatuser.LOVVALG_AVKLART : Saksstatuser.AVSLUTTET;
        avsluttFagsakOgBehandling(fagsak, saksstatus);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
    }

    public void avsluttFagsakOgBehandling(Fagsak fagsak, Saksstatuser saksstatus) throws FunksjonellException, TekniskException {
        avsluttFagsakOgBehandling(fagsak, fagsak.getAktivBehandling(), saksstatus);
    }

    public void avsluttFagsakOgBehandling(Fagsak fagsak, Behandling behandling, Saksstatuser saksstatus) throws FunksjonellException {

        if (!behandling.getFagsak().getSaksnummer().equals(fagsak.getSaksnummer())) {
            throw new FunksjonellException("Behandling " + behandling.getId() + " tilhører ikke fagsak " + fagsak.getSaksnummer());
        }
        oppdaterStatus(fagsak, saksstatus);
        behandlingService.avsluttBehandling(behandling.getId());
    }

    protected void oppdaterStatus(Fagsak fagsak, Saksstatuser saksstatus) {
        fagsak.setStatus(saksstatus);
        fagsakRepository.save(fagsak);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public long opprettNyVurderingBehandling(String saksnummer) throws FunksjonellException, TekniskException {
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

        oppdaterStatus(fagsak, Saksstatuser.OPPRETTET);
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            replikertBehandling, replikertBehandling.getInitierendeJournalpostId(), fagsak.hentBruker().getAktørId(), SubjectHandler.getInstance().getUserID()
        );
        avsluttTidligereMedlPeriode(behandlingsresultat);
        return replikertBehandling.getId();
    }

    private void validerOpprettNyVurdering(Behandling behandling, Behandlingsresultat behandlingsresultat) throws FunksjonellException {
        if (behandling.erAktiv() && !behandlingsresultat.erArtikkel16MedSendtAnmodningOmUnntak()) {
            throw new FunksjonellException("Kan ikke revurdere en aktiv behandling");
        } else if (behandling.erEndretPeriode()) {
            throw new FunksjonellException("Kan ikke revurdere en behandling av type " + Behandlingstyper.ENDRET_PERIODE.getBeskrivelse());
        }
    }

    private void avsluttTidligereMedlPeriode(Behandlingsresultat behandlingsresultat) throws IkkeFunnetException, SikkerhetsbegrensningException {
        Collection<? extends Medlemskapsperiode> anmodningsperioder = behandlingsresultat.getAnmodningsperioder();
        Collection<? extends Medlemskapsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();

        Optional<Long> medlPeriodeID = Stream.concat(anmodningsperioder.stream(), lovvalgsperioder.stream())
            .map(Medlemskapsperiode::getMedlPeriodeID)
            .filter(Objects::nonNull)
            .findFirst();

        if (medlPeriodeID.isPresent()) {
            medlPeriodeService.avvisPeriode(medlPeriodeID.get());
        }
    }

}
