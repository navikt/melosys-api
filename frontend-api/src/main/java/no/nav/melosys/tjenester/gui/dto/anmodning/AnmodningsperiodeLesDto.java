package no.nav.melosys.tjenester.gui.dto.anmodning;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public final class AnmodningsperiodeLesDto extends AnmodningsperiodeSkrivDto {
    public final boolean sendtUtland;

    private AnmodningsperiodeLesDto(String id,
                                    PeriodeDto periodeDto,
                                    LovvalgBestemmelse bestemmelse,
                                    LovvalgBestemmelse tilleggsbestemmelse,
                                    Land_iso2 lovvalgsland,
                                    LovvalgBestemmelse unntakFraBestemmelse,
                                    Land_iso2 unntakFraLovvalgsland,
                                    Trygdedekninger trygdedekning,
                                    String medlemskapsperiodeID,
                                    boolean sendtUtland) {
        super(id, periodeDto, bestemmelse, tilleggsbestemmelse, lovvalgsland, unntakFraBestemmelse,
            unntakFraLovvalgsland, trygdedekning, medlemskapsperiodeID);
        this.sendtUtland = sendtUtland;
    }

    @JsonCreator
    @SuppressWarnings("unused")
    public AnmodningsperiodeLesDto(Map<String, String> json) {
        super(json);
        this.sendtUtland = json.containsKey("sendtUtland") && Boolean.valueOf(json.get("sendtUtland"));
    }

    public static AnmodningsperiodeLesDto av(Anmodningsperiode anmodningsperiode) {
        return new AnmodningsperiodeLesDto(anmodningsperiode.getId().toString(),
            new PeriodeDto(anmodningsperiode.getFom(), anmodningsperiode.getTom()),
            anmodningsperiode.getBestemmelse(),
            anmodningsperiode.getTilleggsbestemmelse(),
            anmodningsperiode.getLovvalgsland(),
            anmodningsperiode.getUnntakFraBestemmelse(),
            anmodningsperiode.getUnntakFraLovvalgsland(),
            anmodningsperiode.getDekning(),
            anmodningsperiode.getMedlPeriodeID() != null ? anmodningsperiode.getMedlPeriodeID().toString() : null,
            anmodningsperiode.erSendtUtland());
    }
}
