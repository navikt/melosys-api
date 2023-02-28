package no.nav.melosys.tjenester.gui.dto;

import java.util.Collection;

public class FolketrygdlovenStøttetIkkeStøttetBestemmelserDto {
    public Collection<FolketrygdlovenbestemmelseMedVilkaarDto> getStøttedeBestemmelserMedVilkår() {
        return støttedeBestemmelserMedVilkår;
    }

    public Collection<FolketrygdlovenbestemmelseMedVilkaarDto> getIkkeStøttedeBestemmelserMedVilkår() {
        return ikkeStøttedeBestemmelserMedVilkår;
    }

    public FolketrygdlovenStøttetIkkeStøttetBestemmelserDto(Collection<FolketrygdlovenbestemmelseMedVilkaarDto> støttedeBestemmelserMedVilkår, Collection<FolketrygdlovenbestemmelseMedVilkaarDto> ikkeStøttedeBestemmelserMedVilkår) {
        this.støttedeBestemmelserMedVilkår = støttedeBestemmelserMedVilkår;
        this.ikkeStøttedeBestemmelserMedVilkår = ikkeStøttedeBestemmelserMedVilkår;
    }

    private final Collection<FolketrygdlovenbestemmelseMedVilkaarDto> støttedeBestemmelserMedVilkår;
    private final Collection<FolketrygdlovenbestemmelseMedVilkaarDto> ikkeStøttedeBestemmelserMedVilkår;

}
