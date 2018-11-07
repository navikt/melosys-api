package no.nav.melosys.service;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;

@Service
public class LovvalgsperiodeService {

    private final BehandlingsresultatRepository behandlingsresultatRepo;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepo;

    public LovvalgsperiodeService(BehandlingsresultatRepository behandlingsresultatRepo, LovvalgsperiodeRepository lovvalgsperiodeRepo) {
        this.behandlingsresultatRepo = behandlingsresultatRepo;
        this.lovvalgsperiodeRepo = lovvalgsperiodeRepo;
    }

    public Collection<Lovvalgsperiode> hentLovvalgsperioder(long behandlingsid) {
        return lovvalgsperiodeRepo.findByBehandlingsresultatId(behandlingsid);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<Lovvalgsperiode> lagreLovvalgsperioder(long behandlingsid, Collection<Lovvalgsperiode> lovvalgsperioder)
            throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepo.findOne(behandlingsid);
        if (behandlingsresultat == null) {
            throw new IkkeFunnetException(String.format("Behandling %s fins ikke.", behandlingsid));
        }
        lovvalgsperiodeRepo.deleteByBehandlingsresultat(behandlingsresultat);
        List<Lovvalgsperiode> perioderMedBehandling = lovvalgsperioder.stream()
                .map(l -> kopierLovvalgsperiodeMedBehandlingsResultat(l, behandlingsresultat))
                .collect(Collectors.toList());
        return StreamSupport.stream(lovvalgsperiodeRepo.save(perioderMedBehandling).spliterator(), false)
                .collect(Collectors.toList());
    }

    private final Lovvalgsperiode kopierLovvalgsperiodeMedBehandlingsResultat(Lovvalgsperiode periode, Behandlingsresultat behandlingsresultat) {
        Lovvalgsperiode kopi;
        try {
            kopi = (Lovvalgsperiode) BeanUtils.cloneBean(periode);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            throw new IllegalStateException(e);
        }
        kopi.setBehandlingsresultat(behandlingsresultat);
        return kopi;
    }

}