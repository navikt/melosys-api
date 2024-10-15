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
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;
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

    public Lovvalgsperiode hentLovvalgsperiode(long behandlingsid) {
        Collection<Lovvalgsperiode> lovvalgsperioder = hentLovvalgsperioder(behandlingsid);
        if (lovvalgsperioder.size() != 1) {
            if (lovvalgsperioder.size() > 1) {
                throw new FunksjonellException("Fant %s lovvalgsperioder. Forventer kun én lovvalgsperiode"
                    .formatted(lovvalgsperioder.size()));
            } else {
                throw new FunksjonellException("Fant ingen lovvalgsperiode. Forventer én lovvalgsperiode");
            }
        }
        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();
        if (lovvalgsperiode.harUgyldigTilstand()) {
            throw new FunksjonellException("Lovvalgsperioden har en ugyldig kombinasjon av resultat og lovvalgsland");
        }
        return lovvalgsperiode;
    }

    @Transactional
    public Lovvalgsperiode oppdaterLovvalgsperiode(long lovvalgsperiodeId, Lovvalgsperiode lovvalgsperiode) {
        var lagretLovvalgsperiode = lovvalgsperiodeRepo.findById(lovvalgsperiodeId)
            .orElseThrow(() -> new FunksjonellException(String.format("Lovvalgsperioden %s finnes ikke", lovvalgsperiodeId)));

        lagretLovvalgsperiode.setFom(lovvalgsperiode.getFom());
        lagretLovvalgsperiode.setTom(lovvalgsperiode.getTom());
        lagretLovvalgsperiode.setLovvalgsland(lovvalgsperiode.getLovvalgsland());
        lagretLovvalgsperiode.setBestemmelse(lovvalgsperiode.getBestemmelse());
        lagretLovvalgsperiode.setTilleggsbestemmelse(lovvalgsperiode.getTilleggsbestemmelse());
        lagretLovvalgsperiode.setInnvilgelsesresultat(lovvalgsperiode.getInnvilgelsesresultat());
        lagretLovvalgsperiode.setDekning(lovvalgsperiode.getDekning());
        lagretLovvalgsperiode.setMedlemskapstype(lovvalgsperiode.getMedlemskapstype());
        lagretLovvalgsperiode.setMedlPeriodeID(lovvalgsperiode.getMedlPeriodeID());

        return lovvalgsperiodeRepo.save(lagretLovvalgsperiode);
    }

    @Transactional
    public void slettLovvalgsperiode(long lovvalgsperiodeId) {
        lovvalgsperiodeRepo.deleteById(lovvalgsperiodeId);
    }

    @Transactional
    public Collection<Lovvalgsperiode> lagreLovvalgsperioder(long behandlingsid, Collection<Lovvalgsperiode> lovvalgsperioder) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepo.findById(behandlingsid)
            .orElseThrow(() -> new IllegalStateException(String.format("Behandling %s fins ikke.", behandlingsid)));

        lovvalgsperiodeRepo.deleteByBehandlingsresultatId(behandlingsresultat.getId());

        List<Lovvalgsperiode> lovvalgsperiodeKopi = lovvalgsperioder
            .stream()
            .map(periode -> kopierLovvalgsperiodeMedBehandlingsResultat(periode, behandlingsresultat))
            .toList();

        return lovvalgsperiodeRepo.saveAllAndFlush(lovvalgsperiodeKopi);
    }

    public Collection<Lovvalgsperiode> hentTidligereLovvalgsperioder(Behandling behandling) {
        Set<Long> utvalgtePeriodeIDer = tidligereMedlemsperiodeRepository.findById_BehandlingId(behandling.getId()).stream()
            .map(utvalgtPeriode -> utvalgtPeriode.getId().getPeriodeId())
            .collect(Collectors.toSet());

        if (utvalgtePeriodeIDer.isEmpty()) {
            return Collections.emptySet();
        }

        MedlemskapDokument medlemskapdokument = behandling.hentMedlemskapDokument();
        Set<Medlemsperiode> perioder = medlemskapdokument.getMedlemsperiode().stream()
            .filter(periode -> utvalgtePeriodeIDer.contains(periode.getId()))
            .collect(Collectors.toSet());

        List<Lovvalgsperiode> tidligereLovvalgsperioder = new ArrayList<>();
        for (Medlemsperiode periode : perioder) {
            Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
            lovvalgsperiode.setFom(periode.getPeriode().getFom());
            lovvalgsperiode.setTom(periode.getPeriode().getTom());
            lovvalgsperiode.setMedlPeriodeID(periode.getId());
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

    public Lovvalgsperiode hentOpprinneligLovvalgsperiode(long behandlingId) {
        Behandling behandling = behandlingRepository.findById(behandlingId)
            .orElseThrow(() -> new IkkeFunnetException("Fant ingen behandling for " + behandlingId));

        Behandling opprinneligBehandling = Optional.ofNullable(behandling.getOpprinneligBehandling())
            .orElseThrow(() -> new IkkeFunnetException("Fant ingen opprinnelig behandling for " + behandlingId));

        List<Lovvalgsperiode> lovvalgsperiodeList = lovvalgsperiodeRepo.findByBehandlingsresultatId(opprinneligBehandling.getId());
        return lovvalgsperiodeList.stream()
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Fant ingen opprinnelig lovvalgsperiode for " + behandlingId));
    }

    public Optional<Lovvalgsperiode> finnOpprinneligLovvalgsperiode(long behandlingId) {
        return behandlingRepository.findById(behandlingId).map(Behandling::getOpprinneligBehandling)
            .flatMap(behandling -> lovvalgsperiodeRepo.findByBehandlingsresultatId(behandling.getId()).stream().findFirst());
    }

    public boolean harSelvstendigNæringsdrivendeLovvalgsbestemmelse(long behandlingId) {
        var lovvalgBestemmelse = hentLovvalgsperiode(behandlingId).getBestemmelse();
        return lovvalgBestemmelse.equals(Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2) ||
            lovvalgBestemmelse.equals(Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4);
    }

    private Lovvalgsperiode kopierLovvalgsperiodeMedBehandlingsResultat(Lovvalgsperiode periode, Behandlingsresultat behandlingsresultat) {
        Lovvalgsperiode kopi;
        try {
            kopi = (Lovvalgsperiode) BeanUtils.cloneBean(periode);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException e) {
            throw new IllegalStateException(e);
        }
        kopi.setBehandlingsresultat(behandlingsresultat);
        return kopi;
    }
}
