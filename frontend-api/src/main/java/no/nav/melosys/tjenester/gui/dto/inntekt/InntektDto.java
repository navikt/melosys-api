package no.nav.melosys.tjenester.gui.dto.inntekt;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;

public class InntektDto {

    public List<ArbeidsInntektMaanedDto> arbeidsInntektMaanedListe = new ArrayList<>();

    public List<FrilansInntektMaanedDto> frilansInntektMaanedListe = new ArrayList<>();

    public InntektDto() {}

    public InntektDto(InntektDokument inntektDokument) {
        if (inntektDokument != null && !inntektDokument.arbeidsInntektMaanedListe.isEmpty()) {
            for (ArbeidsInntektMaaned arbeidsInntektMaaned : inntektDokument.arbeidsInntektMaanedListe) {
                if (arbeidsInntektMaaned.arbeidsInntektInformasjon != null) {
                    if (!arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe.isEmpty()) {
                        arbeidsInntektMaanedListe.add(new ArbeidsInntektMaanedDto(arbeidsInntektMaaned));
                    }
                    if (!arbeidsInntektMaaned.arbeidsInntektInformasjon.arbeidsforholdListe.isEmpty()) {
                        frilansInntektMaanedListe.add(new FrilansInntektMaanedDto(arbeidsInntektMaaned));
                    }
                }
            }
        }
    }
}
