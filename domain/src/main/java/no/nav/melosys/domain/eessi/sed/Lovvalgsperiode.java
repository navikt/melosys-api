package no.nav.melosys.domain.eessi.sed;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.Periode;

public class Lovvalgsperiode {

    private String lovvalgsland;
    private String unntakFraLovvalgsland;
    private Bestemmelse bestemmelse;
    private Bestemmelse tilleggsBestemmelse;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fom;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate tom;

    private String unntaksBegrunnelse;

    private Bestemmelse unntakFraBestemmelse;

    public Periode tilPeriode() {
        return new Periode(fom, tom);
    }

    public String getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(String lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }

    public String getUnntakFraLovvalgsland() {
        return unntakFraLovvalgsland;
    }

    public void setUnntakFraLovvalgsland(String unntakFraLovvalgsland) {
        this.unntakFraLovvalgsland = unntakFraLovvalgsland;
    }

    public Bestemmelse getBestemmelse() {
        return bestemmelse;
    }

    public void setBestemmelse(Bestemmelse bestemmelse) {
        this.bestemmelse = bestemmelse;
    }

    public Bestemmelse getTilleggsBestemmelse() {
        return tilleggsBestemmelse;
    }

    public void setTilleggsBestemmelse(Bestemmelse tilleggsBestemmelse) {
        this.tilleggsBestemmelse = tilleggsBestemmelse;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public String getUnntaksBegrunnelse() {
        return unntaksBegrunnelse;
    }

    public void setUnntaksBegrunnelse(String unntaksBegrunnelse) {
        this.unntaksBegrunnelse = unntaksBegrunnelse;
    }

    public Bestemmelse getUnntakFraBestemmelse() {
        return unntakFraBestemmelse;
    }

    public void setUnntakFraBestemmelse(Bestemmelse unntakFraBestemmelse) {
        this.unntakFraBestemmelse = unntakFraBestemmelse;
    }
}
