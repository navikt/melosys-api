package no.nav.melosys.tjenester.gui.medlemskapsperiode.dto;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;

import java.util.Collection;

public class FolketrygdlovenBestemmelserDto {
    private final Collection<BestemmelseMedVilkårOgBegrunnelserDto> støttedeBestemmelser;
    private final Collection<Folketrygdloven_kap2_bestemmelser> ikkeStøttedeBestemmelser;

    public FolketrygdlovenBestemmelserDto(Collection<BestemmelseMedVilkårOgBegrunnelserDto> støttedeBestemmelser, Collection<Folketrygdloven_kap2_bestemmelser> ikkeStøttedeBestemmelser) {
        this.støttedeBestemmelser = støttedeBestemmelser;
        this.ikkeStøttedeBestemmelser = ikkeStøttedeBestemmelser;
    }

    public Collection<BestemmelseMedVilkårOgBegrunnelserDto> getStøttedeBestemmelser() {
        return støttedeBestemmelser;
    }

    public Collection<Folketrygdloven_kap2_bestemmelser> getIkkeStøttedeBestemmelser() {
        return ikkeStøttedeBestemmelser;
    }
}
