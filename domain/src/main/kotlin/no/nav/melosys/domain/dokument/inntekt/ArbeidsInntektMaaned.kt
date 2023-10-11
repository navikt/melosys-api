package no.nav.melosys.domain.dokument.inntekt;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import no.nav.melosys.domain.dokument.DokumentView;

public class ArbeidsInntektMaaned {

    public YearMonth aarMaaned;

    @JsonView(DokumentView.Database.class)
    @NotNull
    public List<Avvik> avvikListe = new ArrayList<>();

    public ArbeidsInntektInformasjon arbeidsInntektInformasjon;

    public ArbeidsInntektInformasjon getArbeidsInntektInformasjon() {
        return arbeidsInntektInformasjon;
    }

}
