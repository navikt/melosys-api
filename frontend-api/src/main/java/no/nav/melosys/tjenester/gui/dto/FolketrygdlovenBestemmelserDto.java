package no.nav.melosys.tjenester.gui.dto;

import java.util.Collection;

public class FolketrygdlovenBestemmelserDto {
    private final Collection<FolketrygdlovenbestemmelseMedVilkaarDto> støttedeBestemmelserMedVilkår;
    private final Collection<FolketrygdlovenbestemmelseMedVilkaarDto> ikkeStøttedeBestemmelserMedVilkår;

    public FolketrygdlovenBestemmelserDto(Collection<FolketrygdlovenbestemmelseMedVilkaarDto> støttedeBestemmelserMedVilkår, Collection<FolketrygdlovenbestemmelseMedVilkaarDto> ikkeStøttedeBestemmelserMedVilkår) {
        this.støttedeBestemmelserMedVilkår = støttedeBestemmelserMedVilkår;
        this.ikkeStøttedeBestemmelserMedVilkår = ikkeStøttedeBestemmelserMedVilkår;
    }

    public Collection<FolketrygdlovenbestemmelseMedVilkaarDto> getStøttedeBestemmelserMedVilkår() {
        return støttedeBestemmelserMedVilkår;
    }

    public Collection<FolketrygdlovenbestemmelseMedVilkaarDto> getIkkeStøttedeBestemmelserMedVilkår() {
        return ikkeStøttedeBestemmelserMedVilkår;
    }
}
