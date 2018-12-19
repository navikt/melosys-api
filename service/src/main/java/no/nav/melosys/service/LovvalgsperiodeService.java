package no.nav.melosys.service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;

import static no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.tilLovvalgBestemmelse;

@Service
public class LovvalgsperiodeService {

    private final BehandlingsresultatRepository behandlingsresultatRepo;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepo;
    private final TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;

    public LovvalgsperiodeService(BehandlingsresultatRepository behandlingsresultatRepo, LovvalgsperiodeRepository lovvalgsperiodeRepo, TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository) {
        this.behandlingsresultatRepo = behandlingsresultatRepo;
        this.lovvalgsperiodeRepo = lovvalgsperiodeRepo;
        this.tidligereMedlemsperiodeRepository = tidligereMedlemsperiodeRepository;
    }

    public Collection<Lovvalgsperiode> hentLovvalgsperioder(long behandlingsid) {
        return lovvalgsperiodeRepo.findByBehandlingsresultatId(behandlingsid);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<Lovvalgsperiode> lagreLovvalgsperioder(long behandlingsid, Collection<Lovvalgsperiode> lovvalgsperioder) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepo.findOne(behandlingsid);
        if (behandlingsresultat == null) {
            throw new IllegalStateException(String.format("Behandling %s fins ikke.", behandlingsid));
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
            if (EnumUtils.isValidEnum(GrunnlagMedl.class, periode.getGrunnlagstype())) {
                GrunnlagMedl grunnlagMedlKode = GrunnlagMedl.valueOf(periode.getGrunnlagstype());
                lovvalgsperiode.setBestemmelse(tilLovvalgBestemmelse(grunnlagMedlKode));
            }
            else {
                lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ANNET);
            }
            tidligereLovvalgsperioder.add(lovvalgsperiode);
        }
        return tidligereLovvalgsperioder;
    }
}