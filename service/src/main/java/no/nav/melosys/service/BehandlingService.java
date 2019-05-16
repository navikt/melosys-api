package no.nav.melosys.service;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toList;

@Service
public class BehandlingService {

    private final BehandlingRepository behandlingRepository;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;
    private BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public BehandlingService(BehandlingRepository behandlingRepository, BehandlingsresultatRepository behandlingsresultatRepository, TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository, BehandlingsresultatService behandlingsresultatService) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.tidligereMedlemsperiodeRepository = tidligereMedlemsperiodeRepository;
        this.behandlingsresultatService = behandlingsresultatService;
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

    /**
     * Oppdaterer status for en behandling med ID {@code behandlingID}.
     * Brukes til å markere om saksbehandler fortsatt venter på dokumentasjon eller om behandling kan gjenopptas.
     */
    public void oppdaterStatus(long behandlingID, Behandlingsstatus status) throws FunksjonellException {
        if (!erLovligNesteStatusEtterDokumentVurdering(status)) {
            throw new FunksjonellException("Må ikke sette behandlingsstatus til " + status);
        }

        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Behandling " + behandlingID + " finnes ikke."));
        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Behandlingen må være aktiv for å kunne endres. Status var: " + behandling.getStatus());
        }
        behandling.setStatus(status);
        behandlingRepository.save(behandling);
    }

    /**
     * - Oppretter en ny behandling.
     * - Oppretter tom behandlingsresultat.
     */
    @Transactional
    public Behandling nyBehandling(Fagsak fagsak, Behandlingsstatus behandlingsstatus, Behandlingstyper behandlingstype, String initierendeJournalpostId, String initierendeDokumentId) {
        Instant nå = Instant.now();

        Behandling behandling = new Behandling();
        fagsak.setBehandlinger(Collections.singletonList(behandling));
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

        return behandling;
    }

    public List<Long> hentMedlemsperioder(long behandlingID) {
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = tidligereMedlemsperiodeRepository.findById_BehandlingId(behandlingID);
        if (tidligereMedlemsperioder == null) {
            return new ArrayList<>();
        }
        return tidligereMedlemsperioder.stream()
            .map(TidligereMedlemsperiode::getId)
            .map(TidligereMedlemsperiodeId::getPeriodeId)
            .collect(toList());
    }

    public boolean erLovligNesteStatusEtterDokumentVurdering(Behandlingsstatus behandlingsstatus) {
        return (behandlingsstatus == Behandlingsstatus.UNDER_BEHANDLING)
            || (behandlingsstatus == Behandlingsstatus.AVVENT_DOK_PART)
            || (behandlingsstatus == Behandlingsstatus.AVVENT_DOK_UTL);
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
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandling med id " + behandlingId));

        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandlingRepository.save(behandling);
    }

    public Behandling hentBehandling(long behandlingId) throws IkkeFunnetException {
        return behandlingRepository.findById(behandlingId)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandling med id " + behandlingId));
    }
}