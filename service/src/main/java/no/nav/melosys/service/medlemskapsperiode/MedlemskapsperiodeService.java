package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Trygdedekninger.*;
import static no.nav.melosys.service.kontroll.PeriodeKontroller.feilIPeriode;

@Service
public class MedlemskapsperiodeService {

    private static final Collection<Trygdedekninger> GYLDIGE_TRYGDEDEKNINGER = Set.of(HELSEDEL, HELSEDEL_MED_SYKE_OG_FORELDREPENGER,
        PENSJONSDEL, HELSE_OG_PENSJONSDEL, HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER);

    private final MedlemskapsperiodeRepository medlemskapsperiodeRepository;
    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;

    public MedlemskapsperiodeService(MedlemskapsperiodeRepository medlemskapsperiodeRepository,
                                     MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository) {
        this.medlemskapsperiodeRepository = medlemskapsperiodeRepository;
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
    }

    @Transactional(readOnly = true)
    public Collection<Medlemskapsperiode> hentMedlemskapsperioder(long behandlingsresultatID) {
        return medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingsresultatID)
            .map(MedlemAvFolketrygden::getMedlemskapsperioder)
            .orElse(Collections.emptyList());
    }

    public MedlemAvFolketrygden hentMedlemAvFolketrygden(long behandlingsresultatID) {
        return medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingsresultatID)
            .orElseThrow(() -> new FunksjonellException("Bestemmelse er ikke opprettet for behandling " + behandlingsresultatID));
    }

    @Transactional
    public Medlemskapsperiode opprettMedlemskapsperiode(long behandlingsresultatID,
                                                        LocalDate fom,
                                                        LocalDate tom,
                                                        InnvilgelsesResultat innvilgelsesResultat,
                                                        Trygdedekninger trygdedekning) {
        final var medlemAvFolketrygden = hentMedlemAvFolketrygden(behandlingsresultatID);
        final var eksisterendeMedlemsperiode = medlemAvFolketrygden
            .getMedlemskapsperioder()
            .stream()
            .findFirst()
            .orElseThrow(() -> new FunksjonellException("Behandling " + behandlingsresultatID + " har ingen medlemskapsperiode"));

        final var nyMedlemskapsperiode = new Medlemskapsperiode();
        oppdaterMedlemskapsperiode(nyMedlemskapsperiode, fom, tom, innvilgelsesResultat, trygdedekning);
        nyMedlemskapsperiode.setMedlemAvFolketrygden(medlemAvFolketrygden);
        nyMedlemskapsperiode.setArbeidsland(eksisterendeMedlemsperiode.getArbeidsland());
        nyMedlemskapsperiode.setBestemmelse(eksisterendeMedlemsperiode.getBestemmelse());
        nyMedlemskapsperiode.setMedlemskapstype(eksisterendeMedlemsperiode.getMedlemskapstype());

        return medlemskapsperiodeRepository.save(nyMedlemskapsperiode);
    }

    @Transactional
    public Medlemskapsperiode oppdaterMedlemskapsperiode(long behandlingsresultatID,
                                                         long medlemskapsperiodeID,
                                                         LocalDate fom,
                                                         LocalDate tom,
                                                         InnvilgelsesResultat innvilgelsesResultat,
                                                         Trygdedekninger trygdedekning) {
        var medlemskapsperiode = hentMedlemAvFolketrygden(behandlingsresultatID)
            .getMedlemskapsperioder()
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
                                            Trygdedekninger trygdedekning) {
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
    public void slettMedlemskapsperiode(long behandlingsresultatID, long medlemskapsperiodeID) {
        MedlemAvFolketrygden medlemAvFolketrygden = hentMedlemAvFolketrygden(behandlingsresultatID);
        Collection<Medlemskapsperiode> medlemskapsperioder = medlemAvFolketrygden.getMedlemskapsperioder();

        if (medlemskapsperioder.size() == 1) {
            throw new FunksjonellException("Behandlingen må ha minst en medlemskapsperiode");
        }

        var medlemskapsperiode = medlemskapsperioder.stream()
            .filter(m -> m.getId() == medlemskapsperiodeID)
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen medlemskapsperiode med id " + medlemskapsperiodeID + " for behandling " + behandlingsresultatID));

        medlemAvFolketrygden.getMedlemskapsperioder().remove(medlemskapsperiode);
    }

    public Collection<Trygdedekninger> hentGyldigeTrygdedekninger() {
        return GYLDIGE_TRYGDEDEKNINGER;
    }

}
