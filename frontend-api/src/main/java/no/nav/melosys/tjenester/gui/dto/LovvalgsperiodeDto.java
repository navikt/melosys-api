package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;

public final class LovvalgsperiodeDto {

    public static final class LovvalgBestemmelseILand {

        public final String lovvalgBestemmelse;
        public final String lovvalgsland;

        public LovvalgBestemmelseILand(LovvalgBestemmelse_883_2004 lovvalgBestemmelse, Landkoder land) {
            this.lovvalgBestemmelse = lovvalgBestemmelse.name();
            this.lovvalgsland = land.name();
        }

    }

    public static final class UnntakFraBestemmelseILand {

        public final String unntakFraBestemmelse;
        public final String unntakFraLovvalgsland;

        public UnntakFraBestemmelseILand(LovvalgBestemmelse_883_2004 unntakFraBestemmelse, Landkoder unntakFraLovvalgsland) {
            this.unntakFraBestemmelse = unntakFraBestemmelse != null ? unntakFraBestemmelse.name() : null;
            this.unntakFraLovvalgsland = unntakFraLovvalgsland != null ? unntakFraLovvalgsland.name() : null;
        }

    }

    @JsonUnwrapped(suffix = "Dato")
    public final PeriodeDto periode;
    @JsonUnwrapped
    public final LovvalgBestemmelseILand lovvalg;
    @JsonUnwrapped
    public final UnntakFraBestemmelseILand unntak;
    public final String innvilgelsesResultat;
    public final String trygdeDekning;
    public final String medlemskapstype;

    public LovvalgsperiodeDto(PeriodeDto periode,
            LovvalgBestemmelseILand lovvalg,
            UnntakFraBestemmelseILand unntak,
            InnvilgelsesResultat innvilgelsesResultat,
            TrygdeDekning trygdeDekning, Medlemskapstype medlemskapstype) {
        this.periode = periode;
        this.lovvalg = lovvalg;
        this.unntak = unntak;
        this.innvilgelsesResultat = innvilgelsesResultat.name();
        this.trygdeDekning = trygdeDekning != null ? trygdeDekning.name() : null;
        this.medlemskapstype = medlemskapstype.name();
    }

    @JsonCreator
    public LovvalgsperiodeDto(Map<String, String> json) {
        this(new PeriodeDto(LocalDate.parse(json.get("fomDato")),
                LocalDate.parse(json.get("tomDato"))),
                new LovvalgBestemmelseILand(LovvalgBestemmelse_883_2004.valueOf(json.get("lovvalgBestemmelse")),
                        Landkoder.valueOf(json.get("lovvalgsland"))),
                new UnntakFraBestemmelseILand(enumVerdiEllerNull(LovvalgBestemmelse_883_2004.class, json.get("unntakFraBestemmelse")),
                        enumVerdiEllerNull(Landkoder.class, json.get("unntakFraLovvalgsland"))),
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
                new LovvalgBestemmelseILand(lovvalgsperiode.getBestemmelse(),
                        lovvalgsperiode.getLovvalgsland()),
                new UnntakFraBestemmelseILand(lovvalgsperiode.getUnntakFraBestemmelse(), lovvalgsperiode.getUnntakFraLovvalgsland()),
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
        resultat.setLovvalgsland(Landkoder.valueOf(lovvalg.lovvalgsland));
        resultat.setBestemmelse(LovvalgBestemmelse_883_2004.valueOf(lovvalg.lovvalgBestemmelse));
        resultat.setInnvilgelsesresultat(enumVerdiEllerNull(InnvilgelsesResultat.class, innvilgelsesResultat));
        resultat.setDekning(enumVerdiEllerNull(TrygdeDekning.class, trygdeDekning));
        resultat.setMedlemskapstype(enumVerdiEllerNull(Medlemskapstype.class, medlemskapstype));
        return resultat;
    }

    static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);

    }

}
