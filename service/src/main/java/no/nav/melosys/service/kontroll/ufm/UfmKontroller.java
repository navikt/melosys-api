package no.nav.melosys.service.kontroll.ufm;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.*;

final class UfmKontroller {
    private static final String STATSLØS = "XS";
    private static final Set<Landkoder> NORDISK_ELLER_AVTALELAND = Set.of(
        Landkoder.SE,
        Landkoder.DK,
        Landkoder.FI,
        Landkoder.IS,
        Landkoder.AT,
        Landkoder.NL,
        Landkoder.LU
    );

    private UfmKontroller() {
    }

    static Kontroll_begrunnelser feilIPeriode(UfmKontrollData kontrollData) {
        return PeriodeKontroller.feilIPeriode(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
            Kontroll_begrunnelser.FEIL_I_PERIODEN : null;
    }

    static Kontroll_begrunnelser periodeErÅpen(UfmKontrollData kontrollData) {
        return PeriodeKontroller.periodeErÅpen(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
            Kontroll_begrunnelser.INGEN_SLUTTDATO : null;
    }

    static Kontroll_begrunnelser periodeOver24Mnd(UfmKontrollData kontrollData) {
        return PeriodeKontroller.periodeOver24Mnd(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
            Kontroll_begrunnelser.PERIODEN_OVER_24_MD : null;
    }

    static Kontroll_begrunnelser periodeOver5År(UfmKontrollData kontrollData) {
        return PeriodeKontroller.periodeOver5År(
            kontrollData.getSedDokument().getLovvalgsperiode().getFom(), kontrollData.getSedDokument().getLovvalgsperiode().getTom()) ?
                Kontroll_begrunnelser.PERIODEN_OVER_5_AR : null;
    }

    static Kontroll_begrunnelser periodeStarterFørFørsteJuni2012(UfmKontrollData kontrollData) {
        return PeriodeKontroller.datoErFørFørsteJuni2012(kontrollData.getSedDokument().getLovvalgsperiode().getFom())
            ? Kontroll_begrunnelser.PERIODE_FOR_GAMMEL : null;
    }

    static Kontroll_begrunnelser periodeOver1ÅrFremITid(UfmKontrollData kontrollData) {
        return PeriodeKontroller.datoOver1ÅrFremITid(kontrollData.getSedDokument().getLovvalgsperiode().getFom()) ?
            Kontroll_begrunnelser.PERIODE_LANGT_FREM_I_TID : null;
    }

    static Kontroll_begrunnelser utbetaltYtelserFraOffentligIPeriode(UfmKontrollData kontrollData) {
        LocalDate fom = kontrollData.getSedDokument().getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.getSedDokument().getLovvalgsperiode().getTom();
        return YtelseKontroller.utbetaltYtelserFraOffentligIPeriode(kontrollData.getInntektDokument(), fom, tom) ?
            Kontroll_begrunnelser.MOTTAR_YTELSER : null;
    }

    static Kontroll_begrunnelser utbetaltBarnetrygdytelser(UfmKontrollData kontrollData) {
        if (kontrollData.getUtbetalingDokument() == null) {
            return null;
        }

        return YtelseKontroller.utbetaltBarnetrygdytelser(kontrollData.getUtbetalingDokument()) ?
            Kontroll_begrunnelser.MOTTAR_YTELSER : null;
    }

    static Kontroll_begrunnelser lovvalgslandErNorge(UfmKontrollData kontrollData) {
        return lovvalgslandErNorge(kontrollData.getSedDokument().getLovvalgslandKode()) ?
            Kontroll_begrunnelser.LOVVALGSLAND_NORGE : null;
    }

    static boolean lovvalgslandErNorge(Landkoder landkode) {
        return landkode.equals(Landkoder.NO);
    }

    static Kontroll_begrunnelser overlappendeMedlemsperiode(UfmKontrollData kontrollData) {
        return OverlappendeMedlemskapsperioderKontroller.harOverlappendeMedlemsperiodeIkkeAvvistIPeriode(
            kontrollData.getMedlemskapDokument(), kontrollData.getSedDokument().getLovvalgsperiode()) ?
            Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER : null;
    }

    static Kontroll_begrunnelser statsborgerskapIkkeMedlemsland(UfmKontrollData kontrollData) {
        return avsenderErNordiskEllerAvtaleland(kontrollData.getSedDokument().getAvsenderLandkode())
            || erStatsløs(kontrollData.getSedDokument().getStatsborgerskapKoder())
            || statsborgerskapErMedlemsland(kontrollData.getSedDokument().getStatsborgerskapKoder()) ?
            null : Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND;
    }

    static Kontroll_begrunnelser personDød(UfmKontrollData kontrollData) {
        return PersonKontroller.erPersonDød(kontrollData.getPersonDokument()) ?
            Kontroll_begrunnelser.PERSON_DOD : null;
    }

    static Kontroll_begrunnelser personBosattINorge(UfmKontrollData kontrollData) {
        return PersonKontroller.personBosattINorge(kontrollData.getPersonDokument()) ?
            Kontroll_begrunnelser.BOSATT_I_NORGE : null;
    }

    static Kontroll_begrunnelser arbeidssted(UfmKontrollData kontrollData) {
        return ArbeidsstedKontroller.arbeidstedSvalbardOgJanMayen(kontrollData.getSedDokument()) ?
            Kontroll_begrunnelser.ARBEIDSSTED_UTENFOR_EOS : null;
    }

    static boolean statsborgerskapErMedlemsland(Collection<String> statsborgerskapLandkoder) {
        return !statsborgerskapLandkoder.isEmpty() && Arrays.stream(Landkoder.values())
            .anyMatch(landkode -> statsborgerskapLandkoder.contains(landkode.getKode()));
    }

    static boolean avsenderErNordiskEllerAvtaleland(Landkoder avsenderLandkode) {
        return avsenderLandkode != null && NORDISK_ELLER_AVTALELAND.contains(avsenderLandkode);
    }

    static boolean erStatsløs(Collection<String> statsborgerskapLandkoder) {
        return !statsborgerskapLandkoder.isEmpty() && statsborgerskapLandkoder.contains(STATSLØS);
    }
}
