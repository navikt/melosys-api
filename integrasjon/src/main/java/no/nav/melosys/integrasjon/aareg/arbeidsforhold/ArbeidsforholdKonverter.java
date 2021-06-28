package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.*;
import no.nav.melosys.domain.dokument.felles.Periode;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class ArbeidsforholdKonverter {
    private final ArbeidsforholdResponse arbeidsforholdResponse;
    private final KodeOppslag kodeOppslag;

    public ArbeidsforholdKonverter(ArbeidsforholdResponse arbeidsforholdResponse, KodeOppslag kodeOppslag) {
        this.arbeidsforholdResponse = arbeidsforholdResponse;
        this.kodeOppslag = kodeOppslag;
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
            dst.arbeidsgiverID = src.arbeidsgiver.organisasjonsnummer;
            dst.arbeidstakerID = src.getArbeidstaker().offentligIdent;
            dst.opplysningspliktigtype = Aktoertype.valueOf(src.opplysningspliktig.type.toUpperCase());
            dst.opplysningspliktigID = src.opplysningspliktig.organisasjonsnummer;
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
        return OffsetDateTime.parse(date + String.format("+%02d:00", getOffsetFraUTCForNorgeMedDTSavings()));
    }

    private int getOffsetFraUTCForNorgeMedDTSavings() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Oslo");
        Calendar cal = Calendar.getInstance(tz, Locale.forLanguageTag("nb_NO"));
        return cal.getTimeZone().getDSTSavings() / (1000 * 60 * 24);
    }

    private List<Utenlandsopphold> getUtenlandsopphold(List<ArbeidsforholdResponse.Utenlandsopphold> utenlandsopphold) {
        return utenlandsopphold.stream().map(src -> {
            Utenlandsopphold dst = new Utenlandsopphold();
            dst.setLand(src.landkode);
            dst.setPeriode(getPeriode(src.periode));
            dst.setRapporteringsperiode(YearMonth.parse(src.rapporteringsperiode));
            return dst;
        }).collect(Collectors.toList());
    }

    private List<PermisjonOgPermittering> getPermisjonPermitteringer(List<ArbeidsforholdResponse.PermisjonPermitteringer> permisjonPermitteringer) {
        return permisjonPermitteringer.stream().map(src -> {
            PermisjonOgPermittering dst = new PermisjonOgPermittering();
            dst.setPermisjonsId(src.permisjonPermitteringId);
            dst.setPermisjonsPeriode(getPeriode(src.periode));
            dst.setPermisjonsprosent(src.prosent);

            // Permisjon-/permitteringstype (kodeverk: PermisjonsOgPermitteringsBeskrivelse)
            // Finnes det en Enum med "PermisjonsOgPermitteringsBeskrivelse" ?
            dst.setPermisjonOgPermittering(getTermFraKode("PermisjonsOgPermitteringsBeskrivelse", src.type));
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
            // TODO: avklar om det finner en Enum klasse for Kodeverksoversikt navn
            dst.yrke.setTerm(getTermFraKode("Yrker", src.yrke));

            dst.beregnetAntallTimerPrUke = src.beregnetAntallTimerPrUke;
            dst.arbeidstidsordning = new Arbeidstidsordning();
            dst.arbeidstidsordning.setKode(src.arbeidstidsordning);

            // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Avl%C3%B8nningstyper
            dst.avloenningstype = ""; // Finnes ikke i nytt rest api

            dst.gyldighetsperiode = getPeriode(src.gyldighetsperiode);
            dst.beregnetAntallTimerPrUke = src.beregnetAntallTimerPrUke;
            dst.stillingsprosent = src.stillingsprosent;
            // Finner ikke denne i rest api men kan regnes ut
            // TODO:  Spør fag om det brukes til noe
            dst.beregnetStillingsprosent = src.calculateBergnetStillingsprosent();
            dst.sisteLoennsendringsdato = src.getSisteLoennsendringsDato();
            dst.endringsdatoStillingsprosent = src.getSistStillingsendringDato();
            dst.avtaltArbeidstimerPerUke = src.antallTimerPrUke;


            // Ser disse ikke er med i ny aareg rest api
            dst.maritimArbeidsavtale = false;
            dst.skipsregister = null;
            dst.skipstype = null;
            dst.fartsområde = null;

            return dst;
        }).collect(Collectors.toList());
    }

    private String getTermFraKode(String kodeverk, String kode) {
        return kodeOppslag.getTerm(kodeverk, kode);
    }
}
