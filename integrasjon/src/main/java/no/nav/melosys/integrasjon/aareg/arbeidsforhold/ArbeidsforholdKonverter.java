package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.*;
import no.nav.melosys.domain.dokument.felles.Periode;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

public class ArbeidsforholdKonverter {
    private final ArbeidsforholdResponse arbeidsforholdResponse;

    public ArbeidsforholdKonverter(ArbeidsforholdResponse arbeidsforholdResponse) {
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
        // OffsetDateTime is on format 2017-12-03T10:15:30+01:00
        // TODO: Legg til norsk offset, og må vel ta med "daylight savings"...
        return OffsetDateTime.parse(date + "+01:00");
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

            // Ser ut til vi kan bruke type her. Eks. "permisjonMedForeldrepenger"
            // xml eksemple har "Permisjon"
            // TODO: hent fra KodeverkService for å få dette riktig
            dst.setPermisjonOgPermittering(src.type);
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
            // TODO: Bruke KodeverkService for å finne yrke
            dst.yrke.setTerm(""); // Ser det er "" om det ikke finnes fra xml mapping

            dst.beregnetAntallTimerPrUke = src.beregnetAntallTimerPrUke;
            dst.arbeidstidsordning = new Arbeidstidsordning();
            // TODO: Du kan nok stille spørsmål til #team-registre og/eller Anders Bryhni.
            dst.arbeidstidsordning.setKode(src.arbeidstidsordning); // Jeg tror dette blir rikig

            dst.avloenningstype = ""; // soap api gir f.eks "Fastlønn" eller tom string "";
            dst.gyldighetsperiode = getPeriode(src.gyldighetsperiode);
            dst.beregnetAntallTimerPrUke = src.beregnetAntallTimerPrUke;
            dst.stillingsprosent = src.stillingsprosent;
            // Finner ikke denne i rest api men kan regnes ut -
            // TODO:  Spør fag om det brukes til noe
            dst.beregnetStillingsprosent = src.calculateBergnetStillingsprosent();
            dst.sisteLoennsendringsdato = src.getSisteLoennsendringsDato();
            dst.endringsdatoStillingsprosent = src.getSistStillingsendringDato();
            dst.avtaltArbeidstimerPerUke = src.antallTimerPrUke;


            // Ser disse ikke er med xml i tester
            dst.maritimArbeidsavtale = false;
            dst.skipsregister = null;
            dst.skipstype = null;
            dst.fartsområde = null;

            return dst;
        }).collect(Collectors.toList());
    }
}
