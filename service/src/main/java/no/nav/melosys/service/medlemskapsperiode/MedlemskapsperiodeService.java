package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Trygdedekninger.*;
import static no.nav.melosys.service.kontroll.regler.PeriodeRegler.feilIPeriode;

@Service
public class MedlemskapsperiodeService {

    private static final Collection<Trygdedekninger> GYLDIGE_TRYGDEDEKNINGER = Set.of(FTRL_2_9_FØRSTE_LEDD_A_HELSE, FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
        FTRL_2_9_FØRSTE_LEDD_B_PENSJON, FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON, FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER);

    private final MedlemskapsperiodeRepository medlemskapsperiodeRepository;
    private final MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    public MedlemskapsperiodeService(MedlemskapsperiodeRepository medlemskapsperiodeRepository,
                                     MedlemAvFolketrygdenService medlemAvFolketrygdenService,
                                     TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService,
                                     BehandlingsresultatService behandlingsresultatService,
                                     MedlPeriodeService medlPeriodeService) {
        this.medlemskapsperiodeRepository = medlemskapsperiodeRepository;
        this.medlemAvFolketrygdenService = medlemAvFolketrygdenService;
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
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
        nyMedlemskapsperiode.setMedlemskapstype(Medlemskapstyper.PLIKTIG);
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
    public void erstattMedlemskapsperioder(List<Medlemskapsperiode> nyeInnvilgedeMedlemskapsperioder,
                                           long opprinneligBehandlingId,
                                           long nyBehandlingId) {
        var opprinneligeInnvilgedeMedlemskapsperioder =
            behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId)
                .finnMedlemskapsperioder().stream().filter(Medlemskapsperiode::erInnvilget).toList();

        for (Medlemskapsperiode medlemskapsperiode : opprinneligeInnvilgedeMedlemskapsperioder) {
            if (!finnesMedlIdIMedlemskapsperioder(nyeInnvilgedeMedlemskapsperioder, medlemskapsperiode.getMedlPeriodeID())) {
                medlPeriodeService.avvisPeriodeOpphørt(medlemskapsperiode.getMedlPeriodeID());
            }
        }
        for (Medlemskapsperiode medlemskapsperiode : nyeInnvilgedeMedlemskapsperioder) {
            opprettEllerOppdaterMedlPeriode(opprinneligeInnvilgedeMedlemskapsperioder, medlemskapsperiode, nyBehandlingId);
        }
    }

    private void opprettEllerOppdaterMedlPeriode(List<Medlemskapsperiode> opprinneligeInnvilgedeMedlemskapsperioder, Medlemskapsperiode medlemskapsperiode, long nyBehandlingId) {
        if (!finnesMedlIdIMedlemskapsperioder(opprinneligeInnvilgedeMedlemskapsperioder, medlemskapsperiode.getMedlPeriodeID())) {
            medlPeriodeService.opprettPeriodeEndelig(nyBehandlingId, medlemskapsperiode);
        } else {
            medlPeriodeService.oppdaterPeriodeEndelig(nyBehandlingId, medlemskapsperiode);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean finnesMedlIdIMedlemskapsperioder(List<Medlemskapsperiode> medlemskapsperioder,
                                                     Long medlPeriodeId) {
        return medlemskapsperioder.stream().anyMatch(periode ->
            Objects.equals(periode.getMedlPeriodeID(), medlPeriodeId)
        );
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
        return GYLDIGE_TRYGDEDEKNINGER;
    }

}
