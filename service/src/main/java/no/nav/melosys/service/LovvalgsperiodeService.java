package no.nav.melosys.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;

@Service
public final class LovvalgsperiodeService {

    private final BehandlingsresultatRepository behandlingsresultatRepo;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepo;

    public LovvalgsperiodeService(BehandlingsresultatRepository behandlingsresultatRepo, LovvalgsperiodeRepository lovvalgsperiodeRepo) {
        this.behandlingsresultatRepo = behandlingsresultatRepo;
        this.lovvalgsperiodeRepo = lovvalgsperiodeRepo;
    }

    public final Collection<Lovvalgsperiode> hentLovvalgsperioder(long behandlingsid) {
        return lovvalgsperiodeRepo.findByBehandlingsresultatId(behandlingsid);
    }

    public final Collection<Lovvalgsperiode> lagreLovvalgsperioder(long behandlingsid, Collection<Lovvalgsperiode> lovvalgsperioder) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepo.findOne(behandlingsid);
        if (behandlingsresultat == null) {
            return Collections.emptyList();
        }
        lovvalgsperiodeRepo.delete(lovvalgsperioder);
        List<Lovvalgsperiode> perioderMedBehandling = lovvalgsperioder.stream()
                .map(l -> kopierLovvalgsperiodeMedBehandlingsResultat(l, behandlingsresultat))
                .collect(Collectors.toList());
        return StreamSupport.stream(lovvalgsperiodeRepo.save(perioderMedBehandling).spliterator(), false)
                .collect(Collectors.toList());
    }

    private final Lovvalgsperiode kopierLovvalgsperiodeMedBehandlingsResultat(Lovvalgsperiode periode, Behandlingsresultat behandlingsresultat) {
        Gson gson = new Gson();
        Lovvalgsperiode klone = gson.fromJson(gson.toJson(periode), Lovvalgsperiode.class);
        klone.setBehandlingsresultat(behandlingsresultat);
        return klone;
    }

}