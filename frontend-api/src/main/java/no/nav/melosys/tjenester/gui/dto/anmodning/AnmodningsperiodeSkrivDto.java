package no.nav.melosys.tjenester.gui.dto.anmodning;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.apache.commons.lang3.StringUtils;

public class AnmodningsperiodeSkrivDto {
    private static final LovvalgBestemmelsekonverterer konverterer = new LovvalgBestemmelsekonverterer();

    public final String id;
    @JsonUnwrapped(suffix = "Dato")
    public final PeriodeDto periode;
    public final String lovvalgBestemmelse;
    public final String tilleggBestemmelse;
    public final String lovvalgsland;
    public final String unntakFraBestemmelse;
    public final String unntakFraLovvalgsland;
    public final String trygdeDekning;
    public final String medlemskapsperiodeID;


    protected AnmodningsperiodeSkrivDto(String id,
                                        PeriodeDto periode,
                                        LovvalgBestemmelse lovvalgBestemmelse,
                                        LovvalgBestemmelse tilleggBestemmelse,
                                        Landkoder lovvalgsland,
                                        LovvalgBestemmelse unntakFraBestemmelse,
                                        Landkoder unntakFraLovvalgsland,
                                        Trygdedekninger trygdeDekning,
                                        String medlemskapsperiodeID) {
        this.id = id;
        this.periode = periode;
        this.lovvalgBestemmelse = lovvalgBestemmelse != null ? lovvalgBestemmelse.name() : null;
        this.tilleggBestemmelse = tilleggBestemmelse != null ? tilleggBestemmelse.name() : null;
        this.lovvalgsland = lovvalgsland != null ? lovvalgsland.name() : null;
        this.unntakFraBestemmelse = unntakFraBestemmelse != null ? unntakFraBestemmelse.name() : null;
        this.unntakFraLovvalgsland = unntakFraLovvalgsland != null ? unntakFraLovvalgsland.name() : null;
        this.trygdeDekning = trygdeDekning != null ? trygdeDekning.name() : null;
        this.medlemskapsperiodeID = medlemskapsperiodeID;
    }

    @JsonCreator
    @SuppressWarnings("unused")
    public AnmodningsperiodeSkrivDto(Map<String, String> json) {
        this(json.get("id"),
            new PeriodeDto(LocalDate.parse(json.get("fomDato")),
                StringUtils.isEmpty(json.get("tomDato")) ? null : LocalDate.parse(json.get("tomDato"))),
            konverterLovvalgsBestemmelse(json.get("lovvalgBestemmelse")),
            konverterLovvalgsBestemmelse(json.get("tilleggBestemmelse")),
            enumVerdiEllerNull(Landkoder.class, json.get("lovvalgsland")),
            konverterLovvalgsBestemmelse(json.get("unntakFraBestemmelse")),
            enumVerdiEllerNull(Landkoder.class, json.get("unntakFraLovvalgsland")),
            enumVerdiEllerNull(Trygdedekninger.class, json.get("trygdeDekning")),
            json.get("medlemskapsperiodeID"));
    }

    public static AnmodningsperiodeSkrivDto av(Anmodningsperiode anmodningsperiode) {
        return new AnmodningsperiodeSkrivDto(anmodningsperiode.getId().toString(),
            new PeriodeDto(anmodningsperiode.getFom(), anmodningsperiode.getTom()),
            anmodningsperiode.getBestemmelse(),
            anmodningsperiode.getTilleggsbestemmelse(),
            anmodningsperiode.getLovvalgsland(),
            anmodningsperiode.getUnntakFraBestemmelse(),
            anmodningsperiode.getUnntakFraLovvalgsland(),
            anmodningsperiode.getDekning(),
            anmodningsperiode.getMedlPeriodeID() != null ? anmodningsperiode.getMedlPeriodeID().toString() : null);
    }

    public final Anmodningsperiode til() {
        return new Anmodningsperiode(periode.getFom(), periode.getTom(),
            enumVerdiEllerNull(Landkoder.class, lovvalgsland),
            konverterer.convertToEntityAttribute(lovvalgBestemmelse),
            konverterer.convertToEntityAttribute(tilleggBestemmelse),
            enumVerdiEllerNull(Landkoder.class, unntakFraLovvalgsland),
            konverterer.convertToEntityAttribute(unntakFraBestemmelse),
            enumVerdiEllerNull(Trygdedekninger.class, trygdeDekning));
    }

    private static LovvalgBestemmelse konverterLovvalgsBestemmelse(String bestemmelsesnavn) {
        return konverterer.convertToEntityAttribute(bestemmelsesnavn);
    }

    private static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);
    }
}

