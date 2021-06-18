package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.*;
import no.nav.melosys.domain.dokument.felles.Periode;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

public class ArbeidsforholdKonvertering {
    private final ArbeidsforholdResponse arbeidsforholdResponse;

    public ArbeidsforholdKonvertering(ArbeidsforholdResponse arbeidsforholdResponse) {
        this.arbeidsforholdResponse = arbeidsforholdResponse;
    }

    public Saksopplysning createSaksopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(new ArbeidsforholdDokument(getArbeidsforhold()));
        return saksopplysning;
    }

    private List<Arbeidsforhold> getArbeidsforhold() {
        return arbeidsforholdResponse.getArbeidsforhold().stream().map(src -> {
            Arbeidsforhold dst = new Arbeidsforhold();
            dst.arbeidsforholdID = src.arbeidsforholdId;
            dst.arbeidsforholdIDnav = src.navArbeidsforholdId;
            dst.ansettelsesPeriode = getPeriode(src.ansettelsesperiode.periode);
            dst.arbeidsforholdstype = src.getType();
            dst.arbeidsavtaler = getArbeidsAvtaler(src.getArbeidsavtaler());
            dst.permisjonOgPermittering = getPermisjonPermitteringer(src.getPermisjonPermitteringer());
            dst.utenlandsopphold = getUtenlandsopphold(src.utenlandsopphold);
            dst.arbeidsgivertype = Aktoertype.valueOf(src.arbeidsgiver.type.toUpperCase());
            dst.arbeidsgiverID = null; // finnes ikke i rest api
            dst.arbeidstakerID = src.getArbeidstaker().aktoerId; // TODO: sjekk om riktig
            dst.opplysningspliktigtype = Aktoertype.valueOf(src.opplysningspliktig.type.toUpperCase());
            dst.opplysningspliktigID = null; // finnes ikke i rest api
            dst.arbeidsforholdInnrapportertEtterAOrdningen = src.innrapportertEtterAOrdningen;
            dst.opprettelsestidspunkt = getOffsetDateTime(src.registrert);
            dst.sistBekreftet = getOffsetDateTime(src.sistBekreftet);
            dst.antallTimerForTimeloennet = getAntallTimerForTimeloennet(src.antallTimerForTimeloennet);

            return dst;
        }).collect(Collectors.toList());
    }

    private List<AntallTimerIPerioden> getAntallTimerForTimeloennet(List<ArbeidsforholdResponse.AntallTimerForTimeloennet> antallTimerForTimeloennet) {
        return antallTimerForTimeloennet.stream().map(src -> {
            AntallTimerIPerioden dst = new AntallTimerIPerioden();
            dst.setAntallTimer(src.antallTimer);
            dst.setPeriode(getPeriode(src.periode));
            dst.setRapporteringsperiode(getRapporteringsperiode(src.rapporteringsperiode));
            return dst;
        }).collect(Collectors.toList());
    }

    private YearMonth getRapporteringsperiode(String rapporteringsperiode) {
        return YearMonth.parse(rapporteringsperiode);
    }

    private OffsetDateTime getOffsetDateTime(String date) {
        // OffsetDateTime is on format 2017-12-03T10:15:30+01:00
        // But what we get from example is without offset
        // Should we add offset in norway for this to be correct?
        // TODO: check if we can do this a better way
        return OffsetDateTime.parse(date + "+00:00");
    }

    private List<Utenlandsopphold> getUtenlandsopphold(List<ArbeidsforholdResponse.Utenlandsopphold> utenlandsopphold) {
        return utenlandsopphold.stream().map(src -> {
            Utenlandsopphold dst = new Utenlandsopphold();
            dst.setLand(src.landkode);
            dst.setPeriode(getPeriode(src.periode));
            return dst;
        }).collect(Collectors.toList());
    }

    private List<PermisjonOgPermittering> getPermisjonPermitteringer(List<ArbeidsforholdResponse.PermisjonPermitteringer> permisjonPermitteringer) {
        return permisjonPermitteringer.stream().map(src -> {
            PermisjonOgPermittering dst = new PermisjonOgPermittering();
            dst.setPermisjonsId(src.permisjonPermitteringId);
            dst.setPermisjonsPeriode(getPeriode(src.periode));
            return dst;
        }).collect(Collectors.toList());
    }

    private static Periode getPeriode(ArbeidsforholdResponse.Periode periode) {
        return new Periode(periode.getFom(), periode.getTom());
    }

    private List<Arbeidsavtale> getArbeidsAvtaler(List<ArbeidsforholdResponse.Arbeidsavtaler> arbeidsavtalerSrc) {
        return arbeidsavtalerSrc.stream().map(src -> {
            Arbeidsavtale dst = new Arbeidsavtale();
            dst.yrke = new Yrke(src.yrke);
            dst.beregnetAntallTimerPrUke = src.beregnetAntallTimerPrUke;
            // src.arbeidstidsordning har "ikkeSkift", hvordan sette det?
            dst.arbeidstidsordning = new Arbeidstidsordning(); // ?
            return dst;
        }).collect(Collectors.toList());
    }
}
