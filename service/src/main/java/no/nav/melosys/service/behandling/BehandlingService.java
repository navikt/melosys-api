package no.nav.melosys.service.behandling;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.DokumentasjonSvarfrist;
import no.nav.melosys.domain.kodeverk.behandlinger.*;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerKonverterer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.MottatteOpplysningerRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.ARBEID_FLERE_LAND;
import static no.nav.melosys.metrics.MetrikkerNavn.*;

@Service
public class BehandlingService {
    private static final Logger log = LoggerFactory.getLogger(BehandlingService.class);

    private final BehandlingRepository behandlingRepository;
    private final TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MottatteOpplysningerRepository mottatteOpplysningerRepository;
    private final OppgaveService oppgaveService;
    private final LovligeKombinasjonerService lovligeKombinasjonerService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UtledMottaksdato utledMottaksdato;
    private final Unleash unleash;
    private final Counter behandlingerAvsluttet = Metrics.counter(BEHANDLINGER_AVSLUTTET);
    private static final String FINNER_IKKE_BEHANDLING = "Finner ikke behandling med id ";

    static {
        Arrays.stream(Behandlingstema.values()).forEach(
            b -> Metrics.counter(BEHANDLINGSTEMAER_OPPRETTET, TAG_TEMA, b.getKode())
        );
        Arrays.stream(Behandlingstyper.values()).forEach(
            b -> Metrics.counter(BEHANDLINGSTYPER_OPPRETTET, TAG_TYPE, b.getKode())
        );
    }

    public BehandlingService(BehandlingRepository behandlingRepository,
                             TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository,
                             MottatteOpplysningerRepository mottatteOpplysningerRepository,
                             BehandlingsresultatService behandlingsresultatService,
                             @Lazy OppgaveService oppgaveService,
                             @Lazy LovligeKombinasjonerService lovligeKombinasjonerService,
                             ApplicationEventPublisher applicationEventPublisher,
                             UtledMottaksdato utledMottaksdato, Unleash unleash) {
        this.behandlingRepository = behandlingRepository;
        this.tidligereMedlemsperiodeRepository = tidligereMedlemsperiodeRepository;
        this.mottatteOpplysningerRepository = mottatteOpplysningerRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.lovligeKombinasjonerService = lovligeKombinasjonerService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.utledMottaksdato = utledMottaksdato;
        this.unleash = unleash;
    }

    public Behandling hentBehandling(long behandlingId) {
        return behandlingRepository.findById(behandlingId)
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLING + behandlingId));
    }

    public Behandling hentBehandlingMedSaksopplysninger(long behandlingId) {
        return Optional.ofNullable(behandlingRepository.findWithSaksopplysningerById(behandlingId))
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLING + behandlingId));
    }

    @Transactional
    public Behandling nyBehandling(Fagsak fagsak,
                                   Behandlingsstatus behandlingsstatus,
                                   Behandlingstyper behandlingstype,
                                   Behandlingstema behandlingstema,
                                   String initierendeJournalpostId,
                                   String initierendeDokumentId,
                                   LocalDate mottaksdato,
                                   Behandlingsaarsaktyper årsaktype,
                                   String årsakFritekst) {
        Instant nå = Instant.now();
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(nå);
        behandling.setEndretDato(nå);
        behandling.setStatus(behandlingsstatus);
        behandling.setType(behandlingstype);
        behandling.setTema(behandlingstema);
        if (unleash.isEnabled("melosys.ny_opprett_sak") && unleash.isEnabled("melosys.behandle_alle_saker")) {
            if (årsaktype == null || mottaksdato == null) {
                throw new FunksjonellException("Mangler mottaksdato eller behandlingsårsaktype");
            }
            Behandlingsaarsak behandlingsårsak = new Behandlingsaarsak(årsaktype, årsakFritekst, mottaksdato);
            behandling.setBehandlingsårsak(behandlingsårsak);
        }
        behandling.setInitierendeJournalpostId(initierendeJournalpostId);
        behandling.setInitierendeDokumentId(initierendeDokumentId);
        behandling.setBehandlingsfrist(
            unleash.isEnabled("melosys.behandle_alle_saker")
                ? Behandling.utledBehandlingsfrist(behandling, utledMottaksdato.getMottaksdato(behandling))
                : Behandling.utledFristForBehandlingtema(behandlingstema));
        behandlingRepository.save(behandling);

        behandlingsresultatService.lagreNyttBehandlingsresultat(behandling);

        Metrics.counter(BEHANDLINGSTEMAER_OPPRETTET, TAG_TEMA, behandlingstema.getKode()).increment();
        Metrics.counter(BEHANDLINGSTYPER_OPPRETTET, TAG_TYPE, behandlingstype.getKode()).increment();
        return behandling;
    }

    @Transactional
    public void endreBehandling(long behandlingID, Behandlingstyper type, Behandlingstema tema, Behandlingsstatus status, LocalDate mottaksdato) {
        var behandling = hentBehandling(behandlingID);
        boolean behandlingErLåst = behandling.kanIkkeEndres();

        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Behandlingen må være aktiv for å kunne endres");
        }
        if (status != null && status != behandling.getStatus()) {
            validerNyStatusMulig(behandling, status);
            endreStatus(behandling, status);
        }
        if (type != null && type != behandling.getType() && !behandlingErLåst) {
            validerNyTypeMulig(behandling, type);
            endreType(behandling, type);
        }
        if (tema != null && tema != behandling.getTema() && !behandlingErLåst && validerNyTemaMulig(behandling, tema)) {
            endreTema(behandling, tema);
        }
        if (mottaksdato != null && !mottaksdato.equals(behandling.getMottatteOpplysninger().getMottaksdato())) {
            endreMottaksdato(behandling, mottaksdato);
        }
    }

    @Transactional
    public void knyttMedlemsperioder(long behandlingID, List<Long> periodeIder) {
        Behandling behandling = hentBehandling(behandlingID);

        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Medlemsperioder kan ikke lagres på behandling med status " + behandling.getStatus());
        }
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = periodeIder.stream()
            .map(pid -> new TidligereMedlemsperiode(behandlingID, pid)).toList();
        tidligereMedlemsperiodeRepository.deleteById_BehandlingId(behandlingID);
        tidligereMedlemsperiodeRepository.saveAll(tidligereMedlemsperioder);
    }

    public void lagre(Behandling behandling) {
        behandlingRepository.save(behandling);
    }

    public void endreStatus(long behandlingID, Behandlingsstatus status) {
        Behandling behandling = hentBehandling(behandlingID);
        endreStatus(behandling, status);
    }

    public void endreStatus(Behandling behandling, Behandlingsstatus status) {
        if (behandling.getStatus() == status) {
            return;
        }

        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Behandlingen må være aktiv for å kunne endres. Status var: " + behandling.getStatus());
        }

        log.info("Oppdaterer status for behandling {} fra {} til {}", behandling.getId(), behandling.getStatus(), status);
        behandling.setStatus(status);

        behandling.setDokumentasjonSvarfristDato(behandling.erVenterForDokumentasjon() ?
            DokumentasjonSvarfrist.beregnFristFraDagensDato() : null);

        behandlingRepository.save(behandling);

        if (List.of(AVVENT_FAGLIG_AVKLARING, AVVENT_DOK_PART, AVVENT_DOK_UTL).contains(status)) {
            oppgaveService.oppdaterOppgaveMedSaksnummer(
                behandling.getFagsak().getSaksnummer(),
                OppgaveOppdatering.builder().beskrivelse(status.getBeskrivelse()).build()
            );
        } else if (status == Behandlingsstatus.AVSLUTTET) {
            oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
        }

        applicationEventPublisher.publishEvent(new BehandlingEndretStatusEvent(status, behandling));
    }

    public void endreType(Behandling behandling, Behandlingstyper type) {
        log.info("Endrer behandlingstypen for behandling {} fra {} til {}", behandling.getId(), behandling.getType(), type);
        behandling.setType(type);
        behandlingRepository.save(behandling);
        if (!unleash.isEnabled("melosys.behandle_alle_saker")) {
            tilbakestillMottatteOpplysninger(behandling);
            applicationEventPublisher.publishEvent(new BehandlingEndretAvSaksbehandlerEvent(behandling.getId(), behandling));
        }
    }

    public void endreTema(Behandling behandling, Behandlingstema tema) {
        log.info("Endrer behandlingstema for behandling {} fra {} til {}", behandling.getId(), behandling.getTema(), tema);
        behandling.setTema(tema);
        behandlingRepository.save(behandling);
        if (!unleash.isEnabled("melosys.behandle_alle_saker")) {
            tilbakestillMottatteOpplysninger(behandling);
            applicationEventPublisher.publishEvent(new BehandlingEndretAvSaksbehandlerEvent(behandling.getId(), behandling));
        }
    }

    public void endreMottaksdato(Behandling behandling, LocalDate mottaksdato) {
        log.info("Endrer mottaksdato for behandling {} fra {} til {}",
            behandling.getId(), behandling.getMottatteOpplysninger().getMottaksdato(), mottaksdato);
        behandling.getMottatteOpplysninger().setMottaksdato(mottaksdato);
        behandling.setBehandlingsfrist(unleash.isEnabled("melosys.behandle_alle_saker")
            ? Behandling.utledBehandlingsfrist(behandling, mottaksdato)
            : Behandling.utledFristForBehandlingtema(behandling.getTema()));
        behandlingRepository.save(behandling);
        if (!unleash.isEnabled("melosys.behandle_alle_saker")) {
            applicationEventPublisher.publishEvent(new BehandlingEndretAvSaksbehandlerEvent(behandling.getId(), behandling));
        }
    }

    public List<Long> hentMedlemsperioder(long behandlingID) {
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = tidligereMedlemsperiodeRepository.findById_BehandlingId(behandlingID);
        return tidligereMedlemsperioder.stream()
            .map(TidligereMedlemsperiode::getId)
            .map(TidligereMedlemsperiodeId::getPeriodeId)
            .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Behandling replikerBehandlingMedNyttBehandlingsresultat(Behandling tidligsteInaktiveBehandling, Behandlingstyper behandlingstype) {
        Behandling behandlingsreplika;
        try {
            behandlingsreplika = replikerBehandlingUtenMottatteOpplysningerSaksopplysningerOgResultat(tidligsteInaktiveBehandling, behandlingstype);
            behandlingsresultatService.lagreNyttBehandlingsresultat(behandlingsreplika);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
            IllegalAccessException e) {
            throw new TekniskException(String.format("Klarte ikke replikere behandling %s for fagsak %s",
                tidligsteInaktiveBehandling.getId(), tidligsteInaktiveBehandling.getFagsak().getSaksnummer()), e);
        }
        return behandlingsreplika;
    }

    Behandling replikerBehandlingUtenMottatteOpplysningerSaksopplysningerOgResultat(Behandling tidligsteInaktiveBehandling, Behandlingstyper behandlingstype)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Behandling behandlingsreplika = (Behandling) BeanUtils.cloneBean(tidligsteInaktiveBehandling);

        Instant nå = Instant.now();
        behandlingsreplika.setRegistrertDato(nå);
        behandlingsreplika.setEndretDato(nå);
        behandlingsreplika.setId(null);
        behandlingsreplika.setType(behandlingstype);
        behandlingsreplika.setStatus(OPPRETTET);
        behandlingsreplika.setOpprinneligBehandling(tidligsteInaktiveBehandling);
        behandlingsreplika.setMottatteOpplysninger(null);
        behandlingsreplika.setBehandlingsnotater(Collections.emptySet());
        behandlingsreplika.setBehandlingsfrist(unleash.isEnabled("melosys.behandle_alle_saker")
            ? Behandling.utledBehandlingsfrist(behandlingsreplika, utledMottaksdato.getMottaksdato(behandlingsreplika))
            : Behandling.utledFristForBehandlingtema(tidligsteInaktiveBehandling.getTema()));
        behandlingsreplika.setSaksopplysninger(new HashSet<>());
        behandlingRepository.save(behandlingsreplika);

        return behandlingsreplika;
    }

    @Transactional(rollbackFor = Exception.class)
    public Behandling replikerBehandlingOgBehandlingsresultat(Behandling tidligsteInaktiveBehandling,
                                                              Behandlingstyper behandlingstype) {
        Behandling behandlingsreplika;
        try {
            behandlingsreplika = replikerBehandling(tidligsteInaktiveBehandling, behandlingstype);
            behandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingsreplika);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
            IllegalAccessException e) {
            throw new TekniskException(String.format("Klarte ikke replikere behandling %s for fagsak %s",
                tidligsteInaktiveBehandling.getId(), tidligsteInaktiveBehandling.getFagsak().getSaksnummer()), e);
        }

        return behandlingsreplika;
    }

    Behandling replikerBehandling(Behandling tidligsteInaktiveBehandling, Behandlingstyper behandlingstype)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Behandling behandlingsreplika = (Behandling) BeanUtils.cloneBean(tidligsteInaktiveBehandling);

        Instant nå = Instant.now();
        behandlingsreplika.setRegistrertDato(nå);
        behandlingsreplika.setEndretDato(nå);
        behandlingsreplika.setId(null);
        behandlingsreplika.setType(behandlingstype);
        behandlingsreplika.setStatus(OPPRETTET);
        behandlingsreplika.setOpprinneligBehandling(tidligsteInaktiveBehandling);
        behandlingsreplika.setMottatteOpplysninger(replikerMottatteOpplysninger(behandlingsreplika, tidligsteInaktiveBehandling.getMottatteOpplysninger()));
        behandlingsreplika.setBehandlingsnotater(Collections.emptySet());
        behandlingsreplika.setBehandlingsfrist(unleash.isEnabled("melosys.behandle_alle_saker")
            ? Behandling.utledBehandlingsfrist(behandlingsreplika, utledMottaksdato.getMottaksdato(behandlingsreplika))
            : Behandling.utledFristForBehandlingtema(tidligsteInaktiveBehandling.getTema())
        );

        behandlingsreplika.setSaksopplysninger(new HashSet<>());
        for (Saksopplysning saksopplysning : tidligsteInaktiveBehandling.getSaksopplysninger()) {
            Saksopplysning saksopplysningsreplika = (Saksopplysning) BeanUtils.cloneBean(saksopplysning);
            saksopplysningsreplika.setBehandling(behandlingsreplika);
            saksopplysningsreplika.setKilder(replikerKilder(saksopplysningsreplika, saksopplysning.getKilder()));
            saksopplysningsreplika.setId(null);
            behandlingsreplika.getSaksopplysninger().add(saksopplysningsreplika);
        }
        behandlingRepository.save(behandlingsreplika);
        return behandlingsreplika;
    }

    private Set<SaksopplysningKilde> replikerKilder(Saksopplysning saksopplysningreplika, Set<SaksopplysningKilde> kilder)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Set<SaksopplysningKilde> kildereplikas = new HashSet<>();
        for (var kilde : kilder) {
            var kildereplika = (SaksopplysningKilde) BeanUtils.cloneBean(kilde);
            kildereplika.setId(null);
            kildereplika.setSaksopplysning(saksopplysningreplika);
            kildereplikas.add(kildereplika);
        }

        return kildereplikas;
    }

    private MottatteOpplysninger replikerMottatteOpplysninger(Behandling behandlingsreplika, MottatteOpplysninger opprinneligMottatteOpplysninger)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (opprinneligMottatteOpplysninger == null) {
            return null;
        }

        MottatteOpplysninger replikertMottatteOpplysninger = (MottatteOpplysninger) BeanUtils.cloneBean(opprinneligMottatteOpplysninger);
        replikertMottatteOpplysninger.setMottatteOpplysningerdata(opprinneligMottatteOpplysninger.getMottatteOpplysningerData());
        replikertMottatteOpplysninger.setId(null);
        replikertMottatteOpplysninger.setBehandling(behandlingsreplika);
        return replikertMottatteOpplysninger;
    }

    public void avsluttBehandling(long behandlingId) {
        Behandling behandling = hentBehandling(behandlingId);

        avsluttBehandling(behandling);
    }

    private void avsluttBehandling(Behandling behandling) {
        if (behandling.erAvsluttet()) {
            throw new FunksjonellException("Behandling " + behandling.getId() + " er allerede avsluttet!");
        }

        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandlingRepository.save(behandling);
        behandlingerAvsluttet.increment();
        applicationEventPublisher.publishEvent(new BehandlingEndretStatusEvent(AVSLUTTET, behandling));
    }

    public void avsluttNyVurdering(long behandlingId, Behandlingsresultattyper nyBehandlingsResultatType) {
        Behandling behandling = hentBehandling(behandlingId);
        avsluttNyVurdering(behandling, nyBehandlingsResultatType);
    }

    @Transactional(readOnly = true)
    public Collection<Behandling> hentBehandlingerMedstatus(Behandlingsstatus behandlingsstatus) {
        return behandlingRepository.findAllByStatus(behandlingsstatus);
    }

    public void endreBehandlingsstatusFraOpprettetTilUnderBehandling(Behandling aktivBehandling) {
        if (aktivBehandling.getStatus() == Behandlingsstatus.OPPRETTET) {
            aktivBehandling.setStatus(UNDER_BEHANDLING);
            behandlingRepository.save(aktivBehandling);
        }
    }

    public void oppdaterStatusOgSvarfrist(Behandling behandling, Behandlingsstatus behandlingsstatus, Instant svarfristDato) {
        behandling.setStatus(behandlingsstatus);
        behandling.setDokumentasjonSvarfristDato(svarfristDato);
        behandlingRepository.save(behandling);
    }

    private void tilbakestillMottatteOpplysninger(Behandling behandling) {
        behandlingsresultatService.tømBehandlingsresultat(behandling.getId());
        if (behandling.getTema() != ARBEID_FLERE_LAND && behandling.getMottatteOpplysninger() != null) {
            behandling.getMottatteOpplysninger().getMottatteOpplysningerData().soeknadsland.erUkjenteEllerAlleEosLand = false;
            MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(behandling.getMottatteOpplysninger());
            mottatteOpplysningerRepository.saveAndFlush(behandling.getMottatteOpplysninger());
        }
    }

    public Set<Behandlingsstatus> hentMuligeStatuser(long behandlingID) {
        var behandling = hentBehandling(behandlingID);
        if (!unleash.isEnabled("melosys.behandle_alle_saker")) {
            return MuligeManuelleBehandlingsendringer.hentMuligeStatuser(behandling);
        }

        if (behandling.erInaktiv()) return Collections.emptySet();
        return lovligeKombinasjonerService.hentMuligeBehandlingStatuser();
    }

    @Transactional
    public Set<Behandlingstema> hentMuligeBehandlingstema(long behandlingID) {
        var behandling = hentBehandling(behandlingID);
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

        return MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandling, behandlingsresultat, unleash.isEnabled("melosys.behandle_alle_saker"));
    }

    public Set<Behandlingstyper> hentMuligeTyper(long behandlingID) {
        if (unleash.isEnabled("melosys.api.endretype")) {
            var behandling = hentBehandling(behandlingID);

            return MuligeManuelleBehandlingsendringer.hentMuligeTyper(behandling);
        }
        return Collections.emptySet();
    }

    private void avsluttNyVurdering(Behandling behandling, Behandlingsresultattyper nyBehandlingsResultatType) {
        if (!behandling.erNyVurdering()) {
            throw new FunksjonellException("Behandling " + behandling.getId() + " er ikke typen NY_VURDERING!");
        }
        avsluttBehandling(behandling);

        behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.getId(), nyBehandlingsResultatType);
    }

    private void validerNyStatusMulig(Behandling behandling, Behandlingsstatus status) {
        if (!unleash.isEnabled("melosys.behandle_alle_saker")) {
            MuligeManuelleBehandlingsendringer.validerNyStatusMulig(behandling, status);
        } else {
            lovligeKombinasjonerService.validerNyStatusMulig(behandling, status);
        }
    }

    @Deprecated(since = "melosys.behandle_alle_saker")
    private void validerNyTypeMulig(Behandling behandling, Behandlingstyper type) {
        if (!unleash.isEnabled("melosys.behandle_alle_saker")) {
            MuligeManuelleBehandlingsendringer.validerNyTypeMulig(behandling, type);
        }
    }

    @Deprecated(since = "melosys.behandle_alle_saker")
    private boolean validerNyTemaMulig(Behandling behandling, Behandlingstema tema) {
        if (!unleash.isEnabled("melosys.behandle_alle_saker")) {
            var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
            MuligeManuelleBehandlingsendringer.validerNyttTemaMulig(behandling, behandlingsresultat, tema, unleash.isEnabled("melosys.behandle_alle_saker"));
            return behandlingsresultat.erIkkeArtikkel16MedSendtAnmodningOmUnntak();
        }
        return true;
    }
}
