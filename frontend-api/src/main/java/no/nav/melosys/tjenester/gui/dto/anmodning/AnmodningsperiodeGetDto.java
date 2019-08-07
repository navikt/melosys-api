package no.nav.melosys.tjenester.gui.dto.anmodning;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public final class AnmodningsperiodeGetDto extends AnmodningsperiodeDto {
    public final String trygdeDekning;
    public final String medlemskapsperiodeID;
    public final boolean anmodningSaksflytSendt;

    private AnmodningsperiodeGetDto(String id,
                                    PeriodeDto periodeDto,
                                    LovvalgBestemmelse bestemmelse,
                                    LovvalgBestemmelse tilleggsbestemmelse,
                                    Landkoder lovvalgsland,
                                    LovvalgBestemmelse unntakFraBestemmelse,
                                    Landkoder unntakFraLovvalgsland,
                                    Trygdedekninger trygdedekning,
                                    String medlemskapsperiodeID,
                                    boolean erSendt) {
        super(id, periodeDto, bestemmelse, tilleggsbestemmelse, lovvalgsland, unntakFraBestemmelse, unntakFraLovvalgsland);
        this.trygdeDekning = trygdedekning != null ? trygdedekning.getKode() : null;
        this.medlemskapsperiodeID = medlemskapsperiodeID;
        this.anmodningSaksflytSendt = erSendt;
    }

    @JsonCreator
    @SuppressWarnings("unused")
    public AnmodningsperiodeGetDto(Map<String, String> json) {
        super(json);
        this.trygdeDekning = json.get("trygdeDekning");
        this.medlemskapsperiodeID = json.get("medlemskapsperiodeID");
        this.anmodningSaksflytSendt = json.containsKey("anmodningSaksflytSendt") && Boolean.valueOf(json.get("anmodningSaksflytSendt"));
    }

    public static AnmodningsperiodeGetDto av(Anmodningsperiode anmodningsperiode) {
        return new AnmodningsperiodeGetDto(anmodningsperiode.getId().toString(),
            new PeriodeDto(anmodningsperiode.getFom(), anmodningsperiode.getTom()),
            anmodningsperiode.getBestemmelse(),
            anmodningsperiode.getTilleggsbestemmelse(),
            anmodningsperiode.getLovvalgsland(),
            anmodningsperiode.getUnntakFraBestemmelse(),
            anmodningsperiode.getUnntakFraLovvalgsland(),
            anmodningsperiode.getDekning(),
            anmodningsperiode.getMedlPeriodeID() != null ? anmodningsperiode.getMedlPeriodeID().toString() : null,
            anmodningsperiode.erSendt());
    }
}
