package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.*;
import no.nav.melosys.domain.dokument.felles.Periode;

import java.util.List;
import java.util.stream.Collectors;

public class ArbeidsforholdKonvertering {
    private final ArbeidsforholdResponse arbeidsforholdResponse;

    public ArbeidsforholdKonvertering(ArbeidsforholdResponse arbeidsforholdResponse) {
        this.arbeidsforholdResponse = arbeidsforholdResponse;
    }

    public Saksopplysning createSaksopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();

        List<Arbeidsforhold> arbeidsforholdList = arbeidsforholdResponse.getArbeidsforhold().stream().map(src -> {
            Arbeidsforhold dst = new Arbeidsforhold();
            dst.arbeidstakerID = src.getArbeidstaker().aktoerId; // TODO: sjekk om riktig
            dst.arbeidsforholdID = src.getNavArbeidsforholdId().toString();
            dst.arbeidsforholdstype = src.getType();
            dst.arbeidsavtaler = getArbeidsAvtaler(src.getArbeidsavtaler());
            dst.permisjonOgPermittering = getPermisjonPermitteringer(src.getPermisjonPermitteringer());
            return dst;
        }).collect(Collectors.toList());
        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument(arbeidsforholdList);

        saksopplysning.setDokument(arbeidsforholdDokument);
        return saksopplysning;
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
            return dst;
        }).collect(Collectors.toList());
    }
}
