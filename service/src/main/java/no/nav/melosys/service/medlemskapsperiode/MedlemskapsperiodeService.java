package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.*;

import io.getunleash.Unleash;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Trygdedekninger.*;
import static no.nav.melosys.service.kontroll.regler.PeriodeRegler.feilIPeriode;

@Service
public class MedlemskapsperiodeService {

    public static final Collection<Trygdedekninger> GYLDIGE_TRYGDEDEKNINGER_2_7 = Set.of(
        FULL_DEKNING_FTRL,
        FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
        FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
    );

    public static final Collection<Trygdedekninger> GYLDIGE_TRYGDEDEKNINGER_2_8
        = Set.of(
        FTRL_2_9_FØRSTE_LEDD_A_HELSE,
        FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
        FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
        FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
        FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER);

    private final MedlemskapsperiodeRepository medlemskapsperiodeRepository;
    private final MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    private final MedlPeriodeService medlPeriodeService;
    private final Unleash unleash;

    public MedlemskapsperiodeService(MedlemskapsperiodeRepository medlemskapsperiodeRepository,
                                     MedlemAvFolketrygdenService medlemAvFolketrygdenService,
                                     TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService,
                                     MedlPeriodeService medlPeriodeService,
                                     Unleash unleash) {
        this.medlemskapsperiodeRepository = medlemskapsperiodeRepository;
        this.medlemAvFolketrygdenService = medlemAvFolketrygdenService;
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.medlPeriodeService = medlPeriodeService;
        this.unleash = unleash;
    }

    @Transactional(readOnly = true)
    public Collection<Medlemskapsperiode> hentMedlemskapsperioder(long behandlingsresultatID) {
        return medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)
            .map(medlemAvFolketrygden -> medlemAvFolketrygden.getMedlemskapsperioder().stream().toList())
            .orElse(Collections.emptyList());
    }

    @Transactional
    public Medlemskapsperiode opprettMedlemskapsperiode(long behandlingsresultatID,
                                                        LocalDate fom,
                                                        LocalDate tom,
                                                        InnvilgelsesResultat innvilgelsesResultat,
                                                        Trygdedekninger trygdedekning) {
        final var medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID);
        var søknad = (SøknadNorgeEllerUtenforEØS) medlemAvFolketrygden.getBehandlingsresultat().getBehandling().getMottatteOpplysninger().getMottatteOpplysningerData();
        var nyMedlemskapsperiode = new Medlemskapsperiode();

        oppdaterMedlemskapsperiode(nyMedlemskapsperiode, fom, tom, innvilgelsesResultat, trygdedekning);
        nyMedlemskapsperiode.setArbeidsland(søknad.hentArbeidsland());
        nyMedlemskapsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        medlemAvFolketrygden.addMedlemskapsperiode(nyMedlemskapsperiode);


        Medlemskapsperiode medlemskapsperiode = medlemskapsperiodeRepository.save(nyMedlemskapsperiode);
        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden);
        return medlemskapsperiode;
    }

    @Transactional
    public Medlemskapsperiode oppdaterMedlemskapsperiode(long behandlingsresultatID,
                                                         long medlemskapsperiodeID,
                                                         LocalDate fom,
                                                         LocalDate tom,
                                                         InnvilgelsesResultat innvilgelsesResultat,
                                                         Trygdedekninger trygdedekning) {
        var medlemskapsperiode = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)
            .getMedlemskapsperioder()
            .stream()
            .filter(m -> m.getId() == medlemskapsperiodeID)
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Behandling " + behandlingsresultatID + " har ingen medlemskapsperiode med id " + medlemskapsperiodeID));

        oppdaterMedlemskapsperiode(medlemskapsperiode, fom, tom, innvilgelsesResultat, trygdedekning);

        MedlemAvFolketrygden medlemAvFolketrygden = medlemskapsperiode.getMedlemAvFolketrygden();
        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden);

        return medlemskapsperiodeRepository.save(medlemskapsperiode);
    }

    // TODO: MELOSYS-6148
    private void fjernTrygdeavgiftsperioderOmDeFinnes(MedlemAvFolketrygden medlemAvFolketrygden) {
        if (medlemAvFolketrygden.getFastsattTrygdeavgift() != null) {
            trygdeavgiftsgrunnlagService.fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden.getFastsattTrygdeavgift());
        }
    }

    private void oppdaterMedlemskapsperiode(Medlemskapsperiode medlemskapsperiode,
                                            LocalDate fom,
                                            LocalDate tom,
                                            InnvilgelsesResultat innvilgelsesResultat,
                                            Trygdedekninger trygdedekning) {
        if (fom == null || innvilgelsesResultat == null || trygdedekning == null) {
            throw new FunksjonellException("Fom-dato, innvilgelsesresultat og trygdedekning er påkrevd");
        } else if (!GYLDIGE_TRYGDEDEKNINGER_2_8.contains(trygdedekning)) {
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
    public void erstattMedlemskapsperioder(long behandlingID, long opprinneligBehandlingID, List<Medlemskapsperiode> nyeMedlemskapsperioder) {
        var opprinneligeMedlemskapsperioder = hentMedlemskapsperioder(opprinneligBehandlingID);
        var opprinneligeInnvilgedeMedlemskapsperioder = opprinneligeMedlemskapsperioder.stream().filter(Medlemskapsperiode::erInnvilget).toList();
        var nyeInnvilgedeMedlemskapsperioder = nyeMedlemskapsperioder.stream().filter(Medlemskapsperiode::erInnvilget).toList();

        opphørOpprinneligeInnvilgedePerioderSomIkkeVidereføres(opprinneligeInnvilgedeMedlemskapsperioder, nyeInnvilgedeMedlemskapsperioder);
        opprettEllerOppdaterInnvilgedePerioder(behandlingID, nyeInnvilgedeMedlemskapsperioder);

        var opprinneligeOpphørtePerioder = opprinneligeMedlemskapsperioder.stream().filter(Medlemskapsperiode::erOpphørt).toList();
        var nyeOpphørtePerioder = nyeMedlemskapsperioder.stream().filter(Medlemskapsperiode::erOpphørt).toList();
        feilregistrerOpprinneligeOpphørtePerioderSomIkkeVidereføres(opprinneligeOpphørtePerioder, nyeOpphørtePerioder);
        opprettEllerOppdaterOpphørtePerioder(behandlingID, nyeOpphørtePerioder);
    }

    private void opphørOpprinneligeInnvilgedePerioderSomIkkeVidereføres(List<Medlemskapsperiode> opprinneligeInnvilgedeMedlemskapsperioder, List<Medlemskapsperiode> nyeInnvilgedeMedlemskapsperioder) {
        opprinneligeInnvilgedeMedlemskapsperioder.stream()
            .filter(medlemskapsperiode -> !eksistererMedlemskapsperiodeMedID(nyeInnvilgedeMedlemskapsperioder, medlemskapsperiode.getMedlPeriodeID()))
            .mapToLong(Medlemskapsperiode::getMedlPeriodeID)
            .forEach(medlPeriodeService::avvisPeriodeOpphørt);
    }

    private void feilregistrerOpprinneligeOpphørtePerioderSomIkkeVidereføres(List<Medlemskapsperiode> opprinneligeOpphørtePerioder, List<Medlemskapsperiode> nyeOpphørtePerioder) {
        opprinneligeOpphørtePerioder.stream()
            .filter(medlemskapsperiode -> !eksistererMedlemskapsperiodeMedID(nyeOpphørtePerioder, medlemskapsperiode.getMedlPeriodeID()))
            .mapToLong(Medlemskapsperiode::getMedlPeriodeID)
            .forEach(medlPeriodeService::avvisPeriodeFeilregistrert);
    }

    private void opprettEllerOppdaterInnvilgedePerioder(long behandlingID, List<Medlemskapsperiode> nyeInnvilgedeMedlemskapsperioder) {
        nyeInnvilgedeMedlemskapsperioder.forEach(medlemskapsperiode -> opprettEllerOppdaterMedlPeriode(behandlingID, medlemskapsperiode));
    }

    private void opprettEllerOppdaterOpphørtePerioder(long behandlingID, List<Medlemskapsperiode> nyeOpphørteMedlemskapsperioder) {
        nyeOpphørteMedlemskapsperioder.forEach(medlemskapsperiode -> opprettEllerOppdaterOpphørtMedlPeriode(behandlingID, medlemskapsperiode));
    }

    private boolean eksistererMedlemskapsperiodeMedID(List<Medlemskapsperiode> medlemskapsperioder,
                                                      Long medlPeriodeID) {
        return medlemskapsperioder.stream().anyMatch(periode ->
            Objects.equals(periode.getMedlPeriodeID(), medlPeriodeID)
        );
    }

    public void opprettEllerOppdaterMedlPeriode(long behandlingID, Medlemskapsperiode medlemskapsperiode) {
        if (medlemskapsperiode.getMedlPeriodeID() == null) {
            medlPeriodeService.opprettPeriodeEndelig(behandlingID, medlemskapsperiode);
        } else {
            medlPeriodeService.oppdaterPeriodeEndelig(behandlingID, medlemskapsperiode);
        }
    }

    private void opprettEllerOppdaterOpphørtMedlPeriode(long behandlingID, Medlemskapsperiode medlemskapsperiode) {
        if (medlemskapsperiode.getMedlPeriodeID() == null) {
            medlPeriodeService.opprettOpphørtPeriode(behandlingID, medlemskapsperiode);
        } else {
            medlPeriodeService.oppdaterOpphørtPeriode(behandlingID, medlemskapsperiode);
        }
    }

    @Transactional
    public void slettMedlemskapsperiode(long behandlingsresultatID, long medlemskapsperiodeID) {
        MedlemAvFolketrygden medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID);
        Collection<Medlemskapsperiode> medlemskapsperioder = medlemAvFolketrygden.getMedlemskapsperioder();

        var medlemskapsperiode = medlemskapsperioder.stream()
            .filter(m -> m.getId() == medlemskapsperiodeID)
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Finner ingen medlemskapsperiode med id " + medlemskapsperiodeID + " for behandling " + behandlingsresultatID));

        medlemAvFolketrygden.removeMedlemskapsperioder(medlemskapsperiode);
        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden);
    }

    public Collection<Trygdedekninger> hentGyldigeTrygdedekninger() {
        if (unleash.isEnabled(ToggleName.MELOSYS_FOLKETRYGDEN_2_7)) {
            return CollectionUtils.union(GYLDIGE_TRYGDEDEKNINGER_2_7, GYLDIGE_TRYGDEDEKNINGER_2_8);
        }
        return GYLDIGE_TRYGDEDEKNINGER_2_8;
    }

}
