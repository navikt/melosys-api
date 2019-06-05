package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;

final class UnntaksperiodeKontroller {

    private UnntaksperiodeKontroller() {
    }

    static Unntak_periode_begrunnelser gyldigPeriode(KontrollData kontrollData) {
        Periode periode = kontrollData.sedDokument.getLovvalgsperiode();
        return periode.getTom() != null && periode.getFom().isAfter(periode.getTom()) ?
            Unntak_periode_begrunnelser.FEIL_I_PERIODEN : null;
    }

    static Unntak_periode_begrunnelser periodeErÅpen(KontrollData kontrollData) {
        return kontrollData.sedDokument.getLovvalgsperiode().getTom() == null ?
            Unntak_periode_begrunnelser.FEIL_I_PERIODEN : null;
    }

    static Unntak_periode_begrunnelser periodeMaks24Mnd(KontrollData kontrollData) {
        Periode periode = kontrollData.sedDokument.getLovvalgsperiode();
        return periode.getTom() != null && ChronoUnit.MONTHS.between(periode.getFom(), periode.getTom()) <= 23L ?
            Unntak_periode_begrunnelser.PERIODEN_OVER_24_MD : null;
    }

    static Unntak_periode_begrunnelser periodeEldreEnn5År(KontrollData kontrollData) {
        Periode periode = kontrollData.sedDokument.getLovvalgsperiode();
        return periode.getFom().isBefore(LocalDate.now().minusYears(5L)) ?
            Unntak_periode_begrunnelser.PERIODE_FOR_GAMMEL : null;
    }

    static Unntak_periode_begrunnelser periodeOver1ÅrFremITid(KontrollData kontrollData) {
        Periode periode = kontrollData.sedDokument.getLovvalgsperiode();
        return periode.getFom().isAfter(LocalDate.now().plusYears(1L)) ?
            Unntak_periode_begrunnelser.PERIODE_LANGT_FREM_I_TID : null;
    }

    static Unntak_periode_begrunnelser lovvalgslandErNorge(KontrollData kontrollData) {
        Landkoder landkode = kontrollData.sedDokument.getLovvalgslandKode();
        return landkode.equals(Landkoder.NO) ?
            Unntak_periode_begrunnelser.LOVVALGSLAND_NORGE : null;
    }

    static Unntak_periode_begrunnelser overlappendeMedlemsperiode(KontrollData kontrollData) {

        LocalDate fom = kontrollData.sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.sedDokument.getLovvalgsperiode().getTom();
        MedlemskapDokument medlemskapDokument = kontrollData.medlemskapDokument;

        if (tom == null) {
            for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
                Periode periode = medlemsperiode.getPeriode();
                if (fom.isBefore(periode.getTom())) return Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER;
            }
        } else {
            for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
                Periode periode = medlemsperiode.getPeriode();
                if (fom.isBefore(periode.getTom()) && periode.getFom().isBefore(tom))
                    return Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER;
            }
        }
        return null;
    }

    static Unntak_periode_begrunnelser personDød(KontrollData kontrollData) {
        PersonDokument personDokument = kontrollData.personDokument;
        return personDokument.dødsdato != null ?
            Unntak_periode_begrunnelser.PERSON_DOD : null;
    }

    static Unntak_periode_begrunnelser personBosattINorge(KontrollData kontrollData) {
        Bostedsadresse bostedsadresse = kontrollData.personDokument.bostedsadresse;

        return bostedsadresse != null
            && bostedsadresse.getLand() != null
            && Land.NORGE.equals(bostedsadresse.getLand().getKode())
            ? Unntak_periode_begrunnelser.FEIL_I_PERIODEN : null;
    }

    static Unntak_periode_begrunnelser statsborgerskapIkkeMedlemsland(KontrollData kontrollData) {
        SedDokument sedDokument = kontrollData.sedDokument;
        return Arrays.stream(Landkoder.values())
            .noneMatch(landkode -> sedDokument.getStatsborgerskapKoder().contains(landkode.getKode()))
            ? Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND : null;
    }

    static Unntak_periode_begrunnelser utbetaltYtelserFraOffentligIPeriode(KontrollData kontrollData) {
        SedDokument sedDokument = kontrollData.sedDokument;
        InntektDokument inntektDokument = kontrollData.inntektDokument;
        return harUtbetalingerIPeriode(inntektDokument, sedDokument.getLovvalgsperiode().getFom(), sedDokument.getLovvalgsperiode().getTom())
            ? Unntak_periode_begrunnelser.MOTTAR_YTELSER : null;
    }

    private static boolean harUtbetalingerIPeriode(InntektDokument inntektDokument, LocalDate fom, LocalDate tom) {

        YearMonth fra = YearMonth.from(fom);
        YearMonth til = tom != null ? YearMonth.from(tom) : null;

        if(inntektDokument == null || inntektDokument.getArbeidsInntektMaanedListe().isEmpty()) {
            return true;
        }

        for (YtelseFraOffentlige ytelseFraOffentlige : hentYtelseFraOffentlige(inntektDokument)) {
            if (erUtbetaltIPeriode(ytelseFraOffentlige, fra, til)) {
                return false;
            }
        }

        return true;
    }

    private static boolean erUtbetaltIPeriode(YtelseFraOffentlige ytelseFraOffentlige, YearMonth fom, YearMonth tom) {
        YearMonth utbetaltIPeriode = ytelseFraOffentlige.utbetaltIPeriode;

        if (utbetaltIPeriode == null) {
            return false;
        }

        if (tom == null) {
            tom = fom.plusYears(2);
        }

        if (utbetaltIPeriode.isAfter(fom) && utbetaltIPeriode.isBefore(tom)) {
            return true;
        } else {
            return utbetaltIPeriode.equals(fom) || utbetaltIPeriode.equals(tom);
        }
    }

    private static Collection<YtelseFraOffentlige> hentYtelseFraOffentlige(InntektDokument inntektDokument) {
        return inntektDokument.getArbeidsInntektMaanedListe().stream()
            .map(ArbeidsInntektMaaned::getArbeidsInntektInformasjon)
            .filter(Objects::nonNull)
            .map(ArbeidsInntektInformasjon::getInntektListe)
            .flatMap(Collection::stream)
            .filter(YtelseFraOffentlige.class::isInstance)
            .map(YtelseFraOffentlige.class::cast)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
