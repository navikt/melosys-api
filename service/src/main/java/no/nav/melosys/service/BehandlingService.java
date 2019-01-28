package no.nav.melosys.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehandlingService {

    private final BehandlingRepository behandlingRepository;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;

    @Autowired
    public BehandlingService(BehandlingRepository behandlingRepository, BehandlingsresultatRepository behandlingsresultatRepository, TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.tidligereMedlemsperiodeRepository = tidligereMedlemsperiodeRepository;
    }

    /**
     * Knytt medlemsperioder fra MEDL til behandlingen.
     */
    @Transactional
    public void knyttMedlemsperioder(long behandlingID, List<Long> periodeIder) throws FunksjonellException {
        Behandling behandling = behandlingRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Behandling " + behandlingID + " finnes ikke."));

        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Medlemsperioder kan ikke lagres på behandling med status " + behandling.getStatus());
        }
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = periodeIder.stream()
            .map(pid -> new TidligereMedlemsperiode(behandlingID, pid)).collect(Collectors.toList());
        tidligereMedlemsperiodeRepository.deleteById_BehandlingId(behandlingID);
        tidligereMedlemsperiodeRepository.saveAll(tidligereMedlemsperioder);
    }

    /**
     * Oppdaterer status for en behandling med ID {@code behandlingID}.
     * Brukes til å markere om saksbehandler fortsatt venter på dokumentasjon eller om behandling kan gjenopptas.
     */
    public void oppdaterStatus(long behandlingID, Behandlingsstatus status) throws FunksjonellException {
        if (!status.erLovligNesteStatusEtterDokumentVurdering()) {
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
    public Behandling nyBehandling(Fagsak fagsak, Behandlingsstatus behandlingsstatus, Behandlingstype behandlingstype) {
        Instant nå = Instant.now();

        Behandling behandling = new Behandling();
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(nå);
        behandling.setEndretDato(nå);

        behandling.setStatus(behandlingsstatus);
        behandling.setType(behandlingstype);
        behandlingRepository.save(behandling);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.UDEFINERT);
        behandlingsresultat.setType(BehandlingsresultatType.IKKE_FASTSATT);
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
            .collect(Collectors.toList());
    }
}
