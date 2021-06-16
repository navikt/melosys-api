package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.*;

import java.util.ArrayList;
import java.util.List;

public class ArbeidsforholdKonvertering {
    private final ArbeidsforholdResponse arbeidsforholdResponse;

    public ArbeidsforholdKonvertering(ArbeidsforholdResponse arbeidsforholdResponse) {
        this.arbeidsforholdResponse = arbeidsforholdResponse;
    }

    public Saksopplysning createSaksopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();

        List<Arbeidsforhold> arbeidsforholdList = new ArrayList<>();

        for(ArbeidsforholdResponse.Arbeidsforhold src : arbeidsforholdResponse.getArbeidsforhold()) {
            Arbeidsforhold dst = new Arbeidsforhold();
            dst.arbeidsforholdID = src.getNavArbeidsforholdId().toString();
            dst.arbeidsforholdstype = src.getType();
            dst.arbeidsavtaler = getArbeidsAvtaler(src.getArbeidsavtaler());
            arbeidsforholdList.add(dst);
        }

        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument(arbeidsforholdList);

        saksopplysning.setDokument(arbeidsforholdDokument);
        return saksopplysning;
    }

    private List<Arbeidsavtale> getArbeidsAvtaler(List<ArbeidsforholdResponse.Arbeidsavtaler> arbeidsavtalerSrc) {
        List<Arbeidsavtale> arbeidsavtaler = new ArrayList<>();
        for(var src : arbeidsavtalerSrc) {
            Arbeidsavtale dst = new Arbeidsavtale();
            dst.yrke = new Yrke(src.yrke);
            dst.beregnetAntallTimerPrUke = src.beregnetAntallTimerPrUke;
            arbeidsavtaler.add(dst);
        }

        return arbeidsavtaler;
    }
}
