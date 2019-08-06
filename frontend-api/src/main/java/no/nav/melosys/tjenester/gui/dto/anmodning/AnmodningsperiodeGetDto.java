package no.nav.melosys.tjenester.gui.dto.anmodning;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public final class AnmodningsperiodeGetDto extends AnmodningsperiodeDto {
    public final String medlemskapsperiodeID;
    public final boolean sendtUtland;

    private AnmodningsperiodeGetDto(String id,
                                    PeriodeDto periodeDto,
                                    LovvalgBestemmelse bestemmelse,
                                    LovvalgBestemmelse tilleggsbestemmelse,
                                    Landkoder lovvalgsland,
                                    LovvalgBestemmelse unntakFraBestemmelse,
                                    Landkoder unntakFraLovvalgsland,
                                    String medlemskapsperiodeID,
                                    boolean erSendt) {
        super(id, periodeDto, bestemmelse, tilleggsbestemmelse, lovvalgsland, unntakFraBestemmelse, unntakFraLovvalgsland);
        this.medlemskapsperiodeID = medlemskapsperiodeID;
        this.sendtUtland = erSendt;
    }

    public static AnmodningsperiodeGetDto av(Anmodningsperiode anmodningsperiode) {
        return new AnmodningsperiodeGetDto(anmodningsperiode.getId().toString(),
            new PeriodeDto(anmodningsperiode.getFom(), anmodningsperiode.getTom()),
            anmodningsperiode.getBestemmelse(),
            anmodningsperiode.getTilleggsbestemmelse(),
            anmodningsperiode.getLovvalgsland(),
            anmodningsperiode.getUnntakFraBestemmelse(),
            anmodningsperiode.getUnntakFraLovvalgsland(),
            anmodningsperiode.getMedlPeriodeID() != null ? anmodningsperiode.getMedlPeriodeID().toString() : null,
            anmodningsperiode.erSendt());
    }
}
