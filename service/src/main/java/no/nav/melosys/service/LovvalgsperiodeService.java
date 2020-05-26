package no.nav.melosys.service;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.tilLovvalgBestemmelse;

@Service
public class LovvalgsperiodeService {

    private final BehandlingsresultatRepository behandlingsresultatRepo;
    private final BehandlingRepository behandlingRepository;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepo;
    private final TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;

    public LovvalgsperiodeService(BehandlingsresultatRepository behandlingsresultatRepo, LovvalgsperiodeRepository lovvalgsperiodeRepo, TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository, BehandlingRepository behandlingRepository) {
        this.behandlingsresultatRepo = behandlingsresultatRepo;
        this.lovvalgsperiodeRepo = lovvalgsperiodeRepo;
        this.tidligereMedlemsperiodeRepository = tidligereMedlemsperiodeRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public Collection<Lovvalgsperiode> hentLovvalgsperioder(long behandlingsid) {
        return lovvalgsperiodeRepo.findByBehandlingsresultatId(behandlingsid);
    }

    public Lovvalgsperiode hentValidertLovvalgsperiode(long behandlingsid) throws FunksjonellException {
        Collection<Lovvalgsperiode> lovvalgsperioder = hentLovvalgsperioder(behandlingsid);
        if (lovvalgsperioder.size() != 1) {
            throw new FunksjonellException("Forventer minst én og kun én lovvalgsperiode!");
        }
        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();
        if (lovvalgsperiode.harUgyldigTilstand()) {
            throw new FunksjonellException("Lovvalgsperioden har en ugyldig kombinasjon av resultat og lovvalgsland");
        }
        if (lovvalgsperiode.getTom() == null) {
            throw new FunksjonellException("Lovvalgsperioden mangler sluttdato");
        }
        return lovvalgsperiode;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<Lovvalgsperiode> lagreLovvalgsperioder(long behandlingsid, Collection<Lovvalgsperiode> lovvalgsperioder) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepo.findById(behandlingsid)
            .orElseThrow(() -> new IllegalStateException(String.format("Behandling %s fins ikke.", behandlingsid)));

        lovvalgsperiodeRepo.deleteByBehandlingsresultat(behandlingsresultat);
        lovvalgsperiodeRepo.flush();
        List<Lovvalgsperiode> perioderMedBehandling = lovvalgsperioder.stream()
            .map(l -> kopierLovvalgsperiodeMedBehandlingsResultat(l, behandlingsresultat))
            .collect(Collectors.toList());
        return new ArrayList<>(lovvalgsperiodeRepo.saveAll(perioderMedBehandling));
    }

    private Lovvalgsperiode kopierLovvalgsperiodeMedBehandlingsResultat(Lovvalgsperiode periode, Behandlingsresultat behandlingsresultat) {
        Lovvalgsperiode kopi;
        try {
            kopi = (Lovvalgsperiode) BeanUtils.cloneBean(periode);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            throw new IllegalStateException(e);
        }
        kopi.setBehandlingsresultat(behandlingsresultat);
        return kopi;
    }

    public Collection<Lovvalgsperiode> hentTidligereLovvalgsperioder(Behandling behandling) throws TekniskException {
        Set<Long> utvalgtePeriodeIDer = tidligereMedlemsperiodeRepository.findById_BehandlingId(behandling.getId()).stream()
            .map(utvalgtPeriode -> utvalgtPeriode.getId().getPeriodeId())
            .collect(Collectors.toSet());

        MedlemskapDokument medlemskapdokument = SaksopplysningerUtils.hentMedlemskapDokument(behandling);
        Set<Medlemsperiode> perioder = medlemskapdokument.getMedlemsperiode().stream()
            .filter(periode -> utvalgtePeriodeIDer.contains(periode.id))
            .collect(Collectors.toSet());

        List<Lovvalgsperiode> tidligereLovvalgsperioder = new ArrayList<>();
        for (Medlemsperiode periode : perioder) {
            Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
            lovvalgsperiode.setFom(periode.periode.getFom());
            lovvalgsperiode.setTom(periode.periode.getTom());
            lovvalgsperiode.setMedlPeriodeID(periode.id);
            if (periode.getGrunnlagstype() != null
                && EnumUtils.isValidEnum(GrunnlagMedl.class, periode.getGrunnlagstype().toUpperCase())) {
                GrunnlagMedl grunnlagMedlKode = GrunnlagMedl.valueOf(periode.getGrunnlagstype().toUpperCase());
                lovvalgsperiode.setBestemmelse(tilLovvalgBestemmelse(grunnlagMedlKode));
            } else {
                lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET);
            }
            tidligereLovvalgsperioder.add(lovvalgsperiode);
        }
        return tidligereLovvalgsperioder;
    }

    public Lovvalgsperiode hentOpprinneligLovvalgsperiode(long behandlingId) throws IkkeFunnetException {
        Behandling behandling = behandlingRepository.findById(behandlingId)
            .orElseThrow(() -> new IkkeFunnetException("Fant ingen behandling for " + behandlingId));

        Behandling opprinneligBehandling = Optional.ofNullable(behandling.getOpprinneligBehandling())
            .orElseThrow(() -> new IkkeFunnetException("Fant ingen opprinnelig behandling for " + behandlingId));

        List<Lovvalgsperiode> lovvalgsperiodeList = lovvalgsperiodeRepo.findByBehandlingsresultatId(opprinneligBehandling.getId());
        return lovvalgsperiodeList.stream()
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Fant ingen opprinnelig lovvalgsperiode for " + behandlingId));
    }

    Optional<Lovvalgsperiode> finnOpprinneligLovvalgsperiode(long behandlingId) {
        return behandlingRepository.findById(behandlingId).map(Behandling::getOpprinneligBehandling)
            .flatMap(behandling -> lovvalgsperiodeRepo.findByBehandlingsresultatId(behandling.getId()).stream().findFirst());
    }
}