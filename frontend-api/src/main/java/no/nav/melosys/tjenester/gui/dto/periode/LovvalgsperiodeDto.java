package no.nav.melosys.tjenester.gui.dto.periode;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import org.apache.commons.lang3.StringUtils;

public final class LovvalgsperiodeDto {

    private static final LovvalgBestemmelsekonverterer konverterer = new LovvalgBestemmelsekonverterer();

    @JsonUnwrapped(suffix = "Dato")
    public final PeriodeDto periode;
    public final String lovvalgsbestemmelse;
    public final String tilleggBestemmelse;
    public final String lovvalgsland;
    public final String innvilgelsesResultat;
    public final String trygdeDekning;
    public final String medlemskapstype;
    public final String medlemskapsperiodeID;

    public LovvalgsperiodeDto(PeriodeDto periode,
            LovvalgBestemmelse lovvalgsbestemmelse,
            LovvalgBestemmelse tilleggBestemmelse,
            Landkoder lovvalgsland,
            InnvilgelsesResultat innvilgelsesResultat,
            Trygdedekninger trygdeDekning,
            Medlemskapstyper medlemskapstype,
            String medlemskapsperiodeID) {
        this.periode = periode;
        this.lovvalgsbestemmelse = lovvalgsbestemmelse != null ? lovvalgsbestemmelse.name() : null;
        this.tilleggBestemmelse = tilleggBestemmelse != null ? tilleggBestemmelse.name() : null;
        this.lovvalgsland = lovvalgsland != null ? lovvalgsland.name() : null;
        this.innvilgelsesResultat = innvilgelsesResultat.name();
        this.trygdeDekning = trygdeDekning != null ? trygdeDekning.name() : null;
        this.medlemskapstype = medlemskapstype != null ? medlemskapstype.name() : null;
        this.medlemskapsperiodeID = medlemskapsperiodeID;
    }

    @JsonCreator
    LovvalgsperiodeDto(Map<String, String> json) {
        this(new PeriodeDto(LocalDate.parse(json.get("fomDato")),
                StringUtils.isEmpty(json.get("tomDato")) ? null : LocalDate.parse(json.get("tomDato"))),
                konverterLovvalgsBestemmelse(json.get("lovvalgsbestemmelse")),
                konverterLovvalgsBestemmelse(json.get("tilleggBestemmelse")),
                enumVerdiEllerNull(Landkoder.class, json.get("lovvalgsland")),
                InnvilgelsesResultat.valueOf(json.get("innvilgelsesResultat")),
                enumVerdiEllerNull(Trygdedekninger.class, json.get("trygdeDekning")),
                enumVerdiEllerNull(Medlemskapstyper.class, json.get("medlemskapstype")),
                json.get("medlemskapsperiodeID"));
    }

    /**
     * Factory-metode for å lage en DTO fra det korresponderende domeneobjektet.
     *
     * @param lovvalgsperiode ett domeneobjekt å konvertere.
     * @return en ny DTO-instanse.
     */
    public static LovvalgsperiodeDto av(Lovvalgsperiode lovvalgsperiode) {
        return new LovvalgsperiodeDto(new PeriodeDto(
            lovvalgsperiode.getFom(),
            lovvalgsperiode.getTom()),
            lovvalgsperiode.getBestemmelse(),
            lovvalgsperiode.getTilleggsbestemmelse(),
            lovvalgsperiode.getLovvalgsland(),
            lovvalgsperiode.getInnvilgelsesresultat(),
            lovvalgsperiode.getDekning(),
            lovvalgsperiode.getMedlemskapstype(),
            lovvalgsperiode.getMedlPeriodeID() != null ? lovvalgsperiode.getMedlPeriodeID().toString() : null);
    }

    /**
     * Konverter denne instansen til ett korresponderende domeneobjekt.
     *
     * @return ett domeneobjekt initialisert fra denne instansen.
     */
    public final Lovvalgsperiode til() {
        Lovvalgsperiode resultat = new Lovvalgsperiode();
        resultat.setFom(periode.getFom());
        resultat.setTom(periode.getTom());
        resultat.setLovvalgsland(enumVerdiEllerNull(Landkoder.class, lovvalgsland));
        resultat.setBestemmelse(konverterer.convertToEntityAttribute(lovvalgsbestemmelse));
        resultat.setTilleggsbestemmelse(konverterer.convertToEntityAttribute(tilleggBestemmelse));
        resultat.setInnvilgelsesresultat(enumVerdiEllerNull(InnvilgelsesResultat.class, innvilgelsesResultat));
        resultat.setDekning(enumVerdiEllerNull(Trygdedekninger.class, trygdeDekning));
        resultat.setMedlemskapstype(enumVerdiEllerNull(Medlemskapstyper.class, medlemskapstype));
        resultat.setMedlPeriodeID(medlemskapsperiodeID != null ? Long.valueOf(medlemskapsperiodeID) : null);
        return resultat;
    }

    private static LovvalgBestemmelse konverterLovvalgsBestemmelse(String bestemmelsesnavn) {
        return konverterer.convertToEntityAttribute(bestemmelsesnavn);
    }

    static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);
    }
}
