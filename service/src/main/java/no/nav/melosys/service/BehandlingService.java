package no.nav.melosys.service;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.Period;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toList;
import static no.nav.melosys.metrics.MetrikkerNavn.*;

@Service
public class BehandlingService {
    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;

    private final Counter behandlingerAvsluttet = Metrics.counter(BEHANDLINGER_AVSLUTTET);
    private static final String FINNER_IKKE_BEHANDLING = "Finner ikke behandling med id ";

    static {
        Arrays.stream(Behandlingstyper.values()).forEach(
            b -> Metrics.counter(BEHANDLINGSTYPER_OPPRETTET, TAG_TYPE, b.getKode())
        );
    }

    @Autowired
    public BehandlingService(BehandlingRepository behandlingRepository,
                             BehandlingsresultatRepository behandlingsresultatRepository,
                             TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository,
                             BehandlingsresultatService behandlingsresultatService,
                             @Lazy OppgaveService oppgaveService) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.tidligereMedlemsperiodeRepository = tidligereMedlemsperiodeRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
    }

    /**
     * Knytt medlemsperioder fra MEDL til behandlingen.
     */
    @Transactional(rollbackFor = MelosysException.class)
    public void knyttMedlemsperioder(long behandlingID, List<Long> periodeIder) throws FunksjonellException {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Behandling " + behandlingID + " finnes ikke."));

        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Medlemsperioder kan ikke lagres på behandling med status " + behandling.getStatus());
        }
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = periodeIder.stream()
            .map(pid -> new TidligereMedlemsperiode(behandlingID, pid)).collect(toList());
        tidligereMedlemsperiodeRepository.deleteById_BehandlingId(behandlingID);
        tidligereMedlemsperiodeRepository.saveAll(tidligereMedlemsperioder);
    }

    public void lagre(Behandling behandling) {
        behandlingRepository.save(behandling);
    }

    /**
     * Oppdaterer status for en behandling med ID {@code behandlingID}.
     * Brukes til å markere om saksbehandler fortsatt venter på dokumentasjon eller om behandling kan gjenopptas,
     *  eller for å avslutte behandling ved behandlingstype VURDER_TRYGDETID
     */
    public void oppdaterStatus(long behandlingID, Behandlingsstatus status) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Behandling " + behandlingID + " finnes ikke."));

        if (behandling.getStatus() == Behandlingsstatus.VURDER_DOKUMENT && !erLovligNesteStatusEtterDokumentVurdering(status)) {
            throw new FunksjonellException("Må ikke sette behandlingsstatus til " + status);
        } else if (!behandling.erAktiv()) {
            throw new FunksjonellException("Behandlingen må være aktiv for å kunne endres. Status var: " + behandling.getStatus());
        }
        behandling.setStatus(status);
        if (behandling.erVenterForDokumentasjon()) {
            behandling.setDokumentasjonSvarfristDato(Instant.now().plus(Period.ofWeeks(2)));
        }

        behandlingRepository.save(behandling);

        if (status == Behandlingsstatus.AVSLUTTET) {
            oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
        }
    }

    private boolean erLovligNesteStatusEtterDokumentVurdering(Behandlingsstatus behandlingsstatus) {
        return (behandlingsstatus == Behandlingsstatus.UNDER_BEHANDLING)
            || (behandlingsstatus == Behandlingsstatus.AVVENT_DOK_PART)
            || (behandlingsstatus == Behandlingsstatus.AVVENT_DOK_UTL) 
            || (behandlingsstatus == Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
    }

    /**
     * - Oppretter en ny behandling.
     * - Oppretter tom behandlingsresultat.
     */
    @Transactional
    public Behandling nyBehandling(Fagsak fagsak, Behandlingsstatus behandlingsstatus, Behandlingstyper behandlingstype, String initierendeJournalpostId, String initierendeDokumentId) {
        Instant nå = Instant.now();

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(nå);
        behandling.setEndretDato(nå);

        behandling.setStatus(behandlingsstatus);
        behandling.setType(behandlingstype);
        behandling.setInitierendeJournalpostId(initierendeJournalpostId);
        behandling.setInitierendeDokumentId(initierendeDokumentId);
        behandlingRepository.save(behandling);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.UDEFINERT);
        behandlingsresultat.setType(Behandlingsresultattyper.IKKE_FASTSATT);
        behandlingsresultatRepository.save(behandlingsresultat);

        Metrics.counter(BEHANDLINGSTYPER_OPPRETTET, TAG_TYPE, behandlingstype.getKode()).increment();
        return behandling;
    }

    public List<Long> hentMedlemsperioder(long behandlingID) {
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = tidligereMedlemsperiodeRepository.findById_BehandlingId(behandlingID);
        return tidligereMedlemsperioder.stream()
            .map(TidligereMedlemsperiode::getId)
            .map(TidligereMedlemsperiodeId::getPeriodeId)
            .collect(toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Behandling replikerBehandlingOgBehandlingsresultat(Behandling tidligsteInaktiveBehandling, Behandlingsstatus behandlingsstatus, Behandlingstyper behandlingstype) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IkkeFunnetException {
        Behandling behandlingsreplika = replikerBehandling(tidligsteInaktiveBehandling, behandlingsstatus, behandlingstype);
        behandlingsresultatService.replikerBehandlingsresultat(tidligsteInaktiveBehandling, behandlingsreplika);
        return behandlingsreplika;
    }

    Behandling replikerBehandling(Behandling tidligsteInaktiveBehandling, Behandlingsstatus behandlingsstatus, Behandlingstyper behandlingstype) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Behandling behandlingsreplika = (Behandling) BeanUtils.cloneBean(tidligsteInaktiveBehandling);
        behandlingsreplika.setId(null);
        behandlingsreplika.setType(behandlingstype);
        behandlingsreplika.setStatus(behandlingsstatus);
        behandlingsreplika.setOpprinneligBehandling(tidligsteInaktiveBehandling);

        behandlingsreplika.setSaksopplysninger(new HashSet<>());
        for (Saksopplysning saksopplysning : tidligsteInaktiveBehandling.getSaksopplysninger()) {
            Saksopplysning saksopplysningsreplika = (Saksopplysning) BeanUtils.cloneBean(saksopplysning);
            saksopplysningsreplika.setBehandling(behandlingsreplika);
            saksopplysningsreplika.setId(null);
            behandlingsreplika.getSaksopplysninger().add(saksopplysningsreplika);
        }
        behandlingRepository.save(behandlingsreplika);
        return behandlingsreplika;
    }

    public void avsluttBehandling(long behandlingId) throws IkkeFunnetException {
        Behandling behandling = behandlingRepository.findById(behandlingId)
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLING + behandlingId));
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandlingRepository.save(behandling);
        behandlingerAvsluttet.increment();
    }

    public Behandling hentBehandling(long behandlingId) throws IkkeFunnetException {
        return Optional.ofNullable(behandlingRepository.findWithSaksopplysningerById(behandlingId))
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLING + behandlingId));
    }

    public Behandling hentBehandlingUtenSaksopplysninger(long behandlingId) throws IkkeFunnetException {
        return behandlingRepository.findById(behandlingId)
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLING + behandlingId));
    }

    public void endreBehandlingsstatusFraOpprettetTilUnderBehandling(Behandling aktivBehandling) {
        if (aktivBehandling.getStatus() == Behandlingsstatus.OPPRETTET) {
            aktivBehandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
            behandlingRepository.save(aktivBehandling);
        }
    }

    public boolean erBehandlingRedigerbarOgTilordnetSaksbehandler(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        if (!behandling.erRedigerbar()) {
            return false;
        }

        Oppgave oppgave = oppgaveService.hentOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());

        String tilordnetRessurs = oppgave.getTilordnetRessurs();
        return tilordnetRessurs != null && tilordnetRessurs.equalsIgnoreCase(saksbehandler);
    }
}