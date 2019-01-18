package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.Lovvalgsperiode.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;

public final class LovvalgsperiodeDto {

    private static final LovvalgBestemmelsekonverterer konverterer = new LovvalgBestemmelsekonverterer();

    @JsonUnwrapped(suffix = "Dato")
    public final PeriodeDto periode;
    public final String lovvalgBestemmelse;
    public final String tilleggsbestemmelse;
    public final String lovvalgsland;
    public final String unntakFraBestemmelse;
    public final String unntakFraLovvalgsland;
    public final String innvilgelsesResultat;
    public final String trygdeDekning;
    public final String medlemskapstype;

    public LovvalgsperiodeDto(PeriodeDto periode,
            LovvalgBestemmelse lovvalgBestemmelse,
            LovvalgBestemmelse tilleggsbestemmelse,
            Landkoder lovvalgsland,
            LovvalgBestemmelse unntakFraBestemmelse,
            Landkoder unntakFraLovvalgsland,
            InnvilgelsesResultat innvilgelsesResultat,
            TrygdeDekning trygdeDekning,
            Medlemskapstype medlemskapstype) {
        this.periode = periode;
        this.lovvalgBestemmelse = lovvalgBestemmelse.name();
        this.tilleggsbestemmelse = tilleggsbestemmelse != null ? tilleggsbestemmelse.name() : null;
        this.lovvalgsland = lovvalgsland.name();
        this.unntakFraBestemmelse = unntakFraBestemmelse != null ? unntakFraBestemmelse.name() : null;
        this.unntakFraLovvalgsland = unntakFraLovvalgsland != null ? unntakFraLovvalgsland.name() : null;
        this.innvilgelsesResultat = innvilgelsesResultat.name();
        this.trygdeDekning = trygdeDekning != null ? trygdeDekning.name() : null;
        this.medlemskapstype = medlemskapstype.name();
    }

    @JsonCreator
    public LovvalgsperiodeDto(Map<String, String> json) {
        this(new PeriodeDto(LocalDate.parse(json.get("fomDato")),
                LocalDate.parse(json.get("tomDato"))),
                konverterLovvalgsBestemmelse(json.get("lovvalgBestemmelse")),
                konverterLovvalgsBestemmelse(json.get("tilleggsbestemmelse")),
                Landkoder.valueOf(json.get("lovvalgsland")),
                konverterLovvalgsBestemmelse(json.get("unntakFraBestemmelse")),
                enumVerdiEllerNull(Landkoder.class, json.get("unntakFraLovvalgsland")),
                InnvilgelsesResultat.valueOf(json.get("innvilgelsesResultat")),
                enumVerdiEllerNull(TrygdeDekning.class, json.get("trygdeDekning")),
                Medlemskapstype.valueOf(json.get("medlemskapstype")));
    }

    /**
     * Factory-metode for å lage en DTO fra det korresponderende domeneobjektet.
     * 
     * @param lovvalgsperiode
     *            ett domeneobjekt å konvertere.
     * @return en ny DTO-instanse.
     */
    public static LovvalgsperiodeDto av(Lovvalgsperiode lovvalgsperiode) {
        return new LovvalgsperiodeDto(new PeriodeDto(lovvalgsperiode.getFom(),
                lovvalgsperiode.getTom()),
                lovvalgsperiode.getBestemmelse(),
                lovvalgsperiode.getTilleggsbestemmelse(),
                lovvalgsperiode.getLovvalgsland(),
                lovvalgsperiode.getUnntakFraBestemmelse(),
                lovvalgsperiode.getUnntakFraLovvalgsland(),
                lovvalgsperiode.getInnvilgelsesresultat(),
                lovvalgsperiode.getDekning(), lovvalgsperiode.getMedlemskapstype());
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
        resultat.setLovvalgsland(Landkoder.valueOf(lovvalgsland));
        resultat.setBestemmelse(konverterer.convertToEntityAttribute(lovvalgBestemmelse));
        resultat.setUnntakFraBestemmelse(konverterer.convertToEntityAttribute(unntakFraBestemmelse));
        resultat.setTilleggsbestemmelse(konverterer.convertToEntityAttribute(tilleggsbestemmelse));
        resultat.setUnntakFraLovvalgsland(enumVerdiEllerNull(Landkoder.class, unntakFraLovvalgsland));
        resultat.setInnvilgelsesresultat(enumVerdiEllerNull(InnvilgelsesResultat.class, innvilgelsesResultat));
        resultat.setDekning(enumVerdiEllerNull(TrygdeDekning.class, trygdeDekning));
        resultat.setMedlemskapstype(enumVerdiEllerNull(Medlemskapstype.class, medlemskapstype));
        return resultat;
    }

    private static LovvalgBestemmelse konverterLovvalgsBestemmelse(String bestemmelsesnavn) {
        return konverterer.convertToEntityAttribute(bestemmelsesnavn);
    }

    static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);

    }

}
