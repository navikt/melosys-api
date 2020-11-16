package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.service.kontroll.PeriodeKontroller.feilIPeriode;

@Service
public class MedlemskapsperiodeService {

    private static final Collection<Trygdedekninger> GYLDIGE_TRYGDEDEKNINGER = Stream.of(Trygdedekninger.values())
        .filter(trygdedekning -> trygdedekning != Trygdedekninger.FULL_DEKNING_EOSFO).collect(Collectors.toSet());

    private final MedlemskapsperiodeRepository medlemskapsperiodeRepository;

    public MedlemskapsperiodeService(MedlemskapsperiodeRepository medlemskapsperiodeRepository) {
        this.medlemskapsperiodeRepository = medlemskapsperiodeRepository;
    }

    @Transactional(readOnly = true)
    public Collection<Medlemskapsperiode> hentMedlemskapsperioder(long behandlingsresultatID) {
        return medlemskapsperiodeRepository.findByBehandlingsresultatId(behandlingsresultatID);
    }

    @Transactional
    public Medlemskapsperiode opprettMedlemskapsperiode(long behandlingID,
                                                        LocalDate fom,
                                                        LocalDate tom,
                                                        InnvilgelsesResultat innvilgelsesResultat,
                                                        Trygdedekninger trygdedekning) throws FunksjonellException {
        var eksisterendeMedlemsperiode = hentMedlemskapsperioder(behandlingID)
            .stream()
            .findFirst()
            .orElseThrow(() -> new FunksjonellException("Behandling " + behandlingID + " har ingen medlemskapsperiode"));

        var nyMedlemskapsperiode = new Medlemskapsperiode();
        oppdaterMedlemskapsperiode(nyMedlemskapsperiode, fom, tom, innvilgelsesResultat, trygdedekning);
        nyMedlemskapsperiode.setBehandlingsresultat(eksisterendeMedlemsperiode.getBehandlingsresultat());
        nyMedlemskapsperiode.setArbeidsland(eksisterendeMedlemsperiode.getArbeidsland());
        nyMedlemskapsperiode.setBestemmelse(eksisterendeMedlemsperiode.getBestemmelse());
        nyMedlemskapsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);

        return medlemskapsperiodeRepository.save(nyMedlemskapsperiode);
    }

    @Transactional
    public Medlemskapsperiode oppdaterMedlemskapsperiode(long behandlingID,
                                                         long medlemskapsperiodeID,
                                                         LocalDate fom,
                                                         LocalDate tom,
                                                         InnvilgelsesResultat innvilgelsesResultat,
                                                         Trygdedekninger trygdedekning) throws FunksjonellException {
        var medlemskapsperiode = medlemskapsperiodeRepository.findByBehandlingsresultatId(behandlingID)
            .stream()
            .filter(m -> m.getId() == medlemskapsperiodeID)
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Behandling " + behandlingID + " har ingen medlemskapsperiode med id " + medlemskapsperiodeID));

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
    public void slettMedlemskapsperiode(long behandlingID, long medlemskapsperiodeID) throws FunksjonellException {
        Collection<Medlemskapsperiode> medlemskapsperioder = hentMedlemskapsperioder(behandlingID);

        if (medlemskapsperioder.size() == 1) {
            throw new FunksjonellException("Behandlingen må ha minst en medlemskapsperiode");
        }

        var medlemskapsperiode = medlemskapsperioder.stream()
            .filter(m -> m.getId() == medlemskapsperiodeID)
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen medlemskapsperiode med id " + medlemskapsperiodeID + " for behandling " + behandlingID));

        medlemskapsperiodeRepository.delete(medlemskapsperiode);
    }
}
