package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import no.nav.melosys.domain.Medlemskapsperiode;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class Periode {

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    private final LocalDate fom;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    private final LocalDate tom;

    private final String bestemmelse;
    private final String innvilgelsesResultat;
    private final String medlemskapstype;
    private final String dekning;
    private final List<Trygdeavgift> trygdeavgift;

    public Periode(Medlemskapsperiode m) {
        this.fom = m.getFom();
        this.tom = m.getTom();
        this.bestemmelse = m.getBestemmelse().getKode();
        this.innvilgelsesResultat = m.getInnvilgelsesresultat().getKode();
        this.medlemskapstype = m.getMedlemskapstype().getKode();
        this.dekning = m.getDekning().getKode();
        this.trygdeavgift = m.getTrygdeavgift().stream().map(t -> new Trygdeavgift(
            t.getTrygdeavgiftsbeløpMd().toPlainString(),
            t.getTrygdesats().toPlainString(),
            t.getAvgiftskode(),
            t.getAvgiftForInntekt().name())
        ).toList();
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getBestemmelse() {
        return bestemmelse;
    }

    public String getInnvilgelsesResultat() {
        return innvilgelsesResultat;
    }

    public String getMedlemskapstype() {
        return medlemskapstype;
    }

    public String getDekning() {
        return dekning;
    }

    public List<Trygdeavgift> getTrygdeavgift() {
        return trygdeavgift;
    }
}
