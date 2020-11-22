package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.collect.MoreCollectors.onlyElement;
import static java.lang.String.format;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.FULL_DEKNING_EOSFO;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.FULL_DEKNING_FTRL;
import static no.nav.melosys.service.kontroll.PeriodeKontroller.feilIPeriode;

@Service
public class MedlemskapsperiodeService {

    private static final Collection<Trygdedekninger> GYLDIGE_TRYGDEDEKNINGER = Stream.of(Trygdedekninger.values())
        .filter(trygdedekning -> trygdedekning != FULL_DEKNING_EOSFO && trygdedekning != FULL_DEKNING_FTRL).collect(Collectors.toSet());

    private final MedlemskapsperiodeRepository medlemskapsperiodeRepository;
    private final BehandlingsresultatService behandlingsresultatService;

    public MedlemskapsperiodeService(MedlemskapsperiodeRepository medlemskapsperiodeRepository, BehandlingsresultatService behandlingsresultatService) {
        this.medlemskapsperiodeRepository = medlemskapsperiodeRepository;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Transactional(readOnly = true)
    public Collection<Medlemskapsperiode> hentMedlemskapsperioder(long behandlingsresultatID) {
        return medlemskapsperiodeRepository.findByBehandlingsresultatId(behandlingsresultatID);
    }

    @Transactional
    public Medlemskapsperiode opprettMedlemskapsperiode(long behandlingsresultatID,
                                                        LocalDate fom,
                                                        LocalDate tom,
                                                        InnvilgelsesResultat innvilgelsesResultat,
                                                        Trygdedekninger trygdedekning) throws FunksjonellException {
        var eksisterendeMedlemsperiode = hentMedlemskapsperioder(behandlingsresultatID)
            .stream()
            .findFirst()
            .orElseThrow(() -> new FunksjonellException("Behandling " + behandlingsresultatID + " har ingen medlemskapsperiode"));

        var nyMedlemskapsperiode = new Medlemskapsperiode();
        oppdaterMedlemskapsperiode(nyMedlemskapsperiode, fom, tom, innvilgelsesResultat, trygdedekning);
        nyMedlemskapsperiode.setBehandlingsresultat(eksisterendeMedlemsperiode.getBehandlingsresultat());
        nyMedlemskapsperiode.setArbeidsland(eksisterendeMedlemsperiode.getArbeidsland());
        nyMedlemskapsperiode.setBestemmelse(eksisterendeMedlemsperiode.getBestemmelse());
        nyMedlemskapsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);

        return medlemskapsperiodeRepository.save(nyMedlemskapsperiode);
    }

    @Transactional
    public Medlemskapsperiode oppdaterMedlemskapsperiode(long behandlingsresultatID,
                                                         long medlemskapsperiodeID,
                                                         LocalDate fom,
                                                         LocalDate tom,
                                                         InnvilgelsesResultat innvilgelsesResultat,
                                                         Trygdedekninger trygdedekning) throws FunksjonellException {
        var medlemskapsperiode = medlemskapsperiodeRepository.findByBehandlingsresultatId(behandlingsresultatID)
            .stream()
            .filter(m -> m.getId() == medlemskapsperiodeID)
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Behandling " + behandlingsresultatID + " har ingen medlemskapsperiode med id " + medlemskapsperiodeID));

        oppdaterMedlemskapsperiode(medlemskapsperiode, fom, tom, innvilgelsesResultat, trygdedekning);
        return medlemskapsperiodeRepository.save(medlemskapsperiode);
    }

    private void oppdaterMedlemskapsperiode(Medlemskapsperiode medlemskapsperiode,
                                            LocalDate fom,
                                            LocalDate tom,
                                            InnvilgelsesResultat innvilgelsesResultat,
                                            Trygdedekninger trygdedekning) throws FunksjonellException {
        if (fom == null || innvilgelsesResultat == null || trygdedekning == null) {
            throw new FunksjonellException("Fom-dato, innvilgelsesresultat og trygdedekning er påkrevd");
        } else if (!GYLDIGE_TRYGDEDEKNINGER.contains(trygdedekning)) {
            throw new FunksjonellException("Trygedekning " + trygdedekning + " støttes ikke for en medlemskapsperiode");
        } else if (feilIPeriode(fom, tom)) {
            throw new FunksjonellException("Tom-dato kan ikke være før fom-dato");
        }

        medlemskapsperiode.setTom(tom);
        medlemskapsperiode.setFom(fom);
        medlemskapsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        medlemskapsperiode.setTrygdedekning(trygdedekning);
    }

    @Transactional
    public void slettMedlemskapsperiode(long behandlingsresultatID, long medlemskapsperiodeID) throws FunksjonellException {
        Collection<Medlemskapsperiode> medlemskapsperioder = hentMedlemskapsperioder(behandlingsresultatID);

        if (medlemskapsperioder.size() == 1) {
            throw new FunksjonellException("Behandlingen må ha minst en medlemskapsperiode");
        }

        var medlemskapsperiode = medlemskapsperioder.stream()
            .filter(m -> m.getId() == medlemskapsperiodeID)
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen medlemskapsperiode med id " + medlemskapsperiodeID + " for behandling " + behandlingsresultatID));

        medlemskapsperiodeRepository.delete(medlemskapsperiode);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public Collection<Medlemskapsperiode> utledMedlemskapsperioderFraSøknad(long behandlingID, Folketrygdloven_kap2_bestemmelser bestemmelse) throws FunksjonellException {
        if (!støtterBestemmelse(bestemmelse)) {
            throw new FunksjonellException("Støtter ikke perioder med bestemmelse " + bestemmelse);
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        validerSakstype(behandlingsresultat.getBehandling().getFagsak());
        validerVilkår(behandlingsresultat, bestemmelse);
        medlemskapsperiodeRepository.deleteByBehandlingsresultat(behandlingsresultat);

        Behandling behandling = behandlingsresultat.getBehandling();
        Behandlingsgrunnlag behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        SoeknadFtrl søknad = (SoeknadFtrl) behandlingsgrunnlag.getBehandlingsgrunnlagdata();

        var medlemskapsperioder = UtledMedlemskapsperioder.lagMedlemskapsperioder(
            new UtledMedlemskapsperioderRequest(
                søknad.periode,
                søknad.getTrygdedekning(),
                bestemmelse,
                behandlingsgrunnlag.getMottaksdato(),
                søknad.soeknadsland.landkoder.stream().collect(onlyElement())
            )
        );

        medlemskapsperioder.forEach(m -> m.setBehandlingsresultat(behandlingsresultat));
        return medlemskapsperiodeRepository.saveAll(medlemskapsperioder);
    }

    private void validerVilkår(Behandlingsresultat behandlingsresultat, Folketrygdloven_kap2_bestemmelser bestemmelse) throws FunksjonellException {
        var vilkårForBestemmelse = hentVilkårForBestemmelse(bestemmelse);
        if (!behandlingsresultat.oppfyllerVilkår(vilkårForBestemmelse)) {
            throw new FunksjonellException(format("Vilkår %s er påkrevd for bestemmelse %s", vilkårForBestemmelse, bestemmelse));
        }
    }

    private void validerSakstype(Fagsak fagsak) throws FunksjonellException {
        if (fagsak.getType() != Sakstyper.FTRL) {
            throw new FunksjonellException("Kan ikke opprette medlemskapsperioder for sakstype " + fagsak.getType());
        }
    }

    public Collection<Trygdedekninger> hentGyldigeTrygdedekninger() {
        return GYLDIGE_TRYGDEDEKNINGER;
    }

    public Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> hentBestemmelserMedVilkaar() {
        return Map.of(
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            Set.of(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID),
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
            Set.of(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE)
        );
    }

    private Collection<Vilkaar> hentVilkårForBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse) throws FunksjonellException {
        return Optional.ofNullable(hentBestemmelserMedVilkaar().get(bestemmelse))
            .orElseThrow(() -> new FunksjonellException("Finner ikke vilkår for bestemmelse " + bestemmelse));
    }

    private boolean støtterBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse) {
        return hentBestemmelserMedVilkaar().containsKey(bestemmelse);
    }
}
