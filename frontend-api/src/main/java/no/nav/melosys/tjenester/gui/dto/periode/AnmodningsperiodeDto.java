package no.nav.melosys.tjenester.gui.dto.periode;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

public final class AnmodningsperiodeDto {

    private static final LovvalgBestemmelsekonverterer konverterer = new LovvalgBestemmelsekonverterer();

    public final String id;
    @JsonUnwrapped(suffix = "Dato")
    public final PeriodeDto periode;
    public final String lovvalgBestemmelse;
    public final String tilleggBestemmelse;
    public final String lovvalgsland;
    public final String unntakFraBestemmelse;
    public final String unntakFraLovvalgsland;
    public final String medlemskapsperiodeID;

    private AnmodningsperiodeDto(String id,
                              PeriodeDto periode,
                              LovvalgBestemmelse lovvalgBestemmelse,
                              LovvalgBestemmelse tilleggBestemmelse,
                              Landkoder lovvalgsland,
                              LovvalgBestemmelse unntakFraBestemmelse,
                              Landkoder unntakFraLovvalgsland,
                              String medlemskapsperiodeID) {
        this.id = id;
        this.periode = periode;
        this.lovvalgBestemmelse = lovvalgBestemmelse != null ? lovvalgBestemmelse.name() : null;
        this.tilleggBestemmelse = tilleggBestemmelse != null ? tilleggBestemmelse.name() : null;
        this.lovvalgsland = lovvalgsland != null ? lovvalgsland.name() : null;
        this.unntakFraBestemmelse = unntakFraBestemmelse != null ? unntakFraBestemmelse.name() : null;
        this.unntakFraLovvalgsland = unntakFraLovvalgsland != null ? unntakFraLovvalgsland.name() : null;
        this.medlemskapsperiodeID = medlemskapsperiodeID;
    }

    @JsonCreator
    @SuppressWarnings("unused")
    public AnmodningsperiodeDto(Map<String, String> json) {
        this(json.get("id"),
            new PeriodeDto(LocalDate.parse(json.get("fomDato")), LocalDate.parse(json.get("tomDato"))),
            konverterLovvalgsBestemmelse(json.get("lovvalgBestemmelse")),
            konverterLovvalgsBestemmelse(json.get("tilleggBestemmelse")),
            enumVerdiEllerNull(Landkoder.class, json.get("lovvalgsland")),
            konverterLovvalgsBestemmelse(json.get("unntakFraBestemmelse")),
            enumVerdiEllerNull(Landkoder.class, json.get("unntakFraLovvalgsland")),
            json.get("medlemskapsperiodeID"));
    }

    public static AnmodningsperiodeDto av(Anmodningsperiode anmodningsperiode) {
        return new AnmodningsperiodeDto(anmodningsperiode.getId().toString(),
            new PeriodeDto(anmodningsperiode.getFom(), anmodningsperiode.getTom()),
            anmodningsperiode.getBestemmelse(),
            anmodningsperiode.getTilleggsbestemmelse(),
            anmodningsperiode.getLovvalgsland(),
            anmodningsperiode.getUnntakFraBestemmelse(),
            anmodningsperiode.getUnntakFraLovvalgsland(),
            anmodningsperiode.getMedlPeriodeID() != null ? anmodningsperiode.getMedlPeriodeID().toString() : null);
    }

    public final Anmodningsperiode til() {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(periode.getFom(), periode.getTom(),
            enumVerdiEllerNull(Landkoder.class, lovvalgsland),
            konverterer.convertToEntityAttribute(lovvalgBestemmelse),
            konverterer.convertToEntityAttribute(tilleggBestemmelse),
            enumVerdiEllerNull(Landkoder.class, unntakFraLovvalgsland),
            konverterer.convertToEntityAttribute(unntakFraBestemmelse),
            null);
        anmodningsperiode.setMedlPeriodeID(medlemskapsperiodeID != null ? Long.valueOf(medlemskapsperiodeID) : null);
        return anmodningsperiode;
    }

    private static LovvalgBestemmelse konverterLovvalgsBestemmelse(String bestemmelsesnavn) {
        return konverterer.convertToEntityAttribute(bestemmelsesnavn);
    }

    private static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);
    }
}

