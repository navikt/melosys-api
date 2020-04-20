package no.nav.melosys.tjenester.gui.dto.utpeking;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public class UtpekingsperiodeDto {
    private static final LovvalgBestemmelsekonverterer konverterer = new LovvalgBestemmelsekonverterer();

    @JsonUnwrapped(suffix = "Dato")
    public PeriodeDto periode;
    private String lovvalgsbestemmelse;
    private String tilleggsbestemmelse;
    private String lovvalgsland;

    public UtpekingsperiodeDto(PeriodeDto periode, String lovvalgsbestemmelse, String tilleggsbestemmelse, String lovvalgsland) {
        this.periode = periode;
        this.lovvalgsbestemmelse = lovvalgsbestemmelse;
        this.tilleggsbestemmelse = tilleggsbestemmelse;
        this.lovvalgsland = lovvalgsland;
    }

    public PeriodeDto getPeriode() {
        return periode;
    }

    public String getLovvalgsbestemmelse() {
        return lovvalgsbestemmelse;
    }

    public void setLovvalgsbestemmelse(String lovvalgsbestemmelse) {
        this.lovvalgsbestemmelse = lovvalgsbestemmelse;
    }

    public String getTilleggsbestemmelse() {
        return tilleggsbestemmelse;
    }

    public void setTilleggsbestemmelse(String tilleggsbestemmelse) {
        this.tilleggsbestemmelse = tilleggsbestemmelse;
    }

    public String getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(String lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }

    public static UtpekingsperiodeDto av(Utpekingsperiode utpekingsperiode) {
        return new UtpekingsperiodeDto(
            new PeriodeDto(utpekingsperiode.getFom(), utpekingsperiode.getTom()),
            utpekingsperiode.getLovvalgsbestemmelse().name(),
            utpekingsperiode.getTilleggsbestemmelse() != null ? utpekingsperiode.getTilleggsbestemmelse().name() : null,
            utpekingsperiode.getLovvalgsland().name()
        );
    }

    public final Utpekingsperiode tilDomene() {
        return new Utpekingsperiode(
            periode.getFom(),
            periode.getTom(),
            enumVerdiEllerNull(Landkoder.class, lovvalgsland),
            konverterer.convertToEntityAttribute(lovvalgsbestemmelse),
            konverterer.convertToEntityAttribute(tilleggsbestemmelse));
    }

    private static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);
    }
}
