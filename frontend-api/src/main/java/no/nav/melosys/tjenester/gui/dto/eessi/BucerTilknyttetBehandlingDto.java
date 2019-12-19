package no.nav.melosys.tjenester.gui.dto.eessi;

import java.util.List;

public class BucerTilknyttetBehandlingDto {

    private final List<BucInformasjonDto> bucer;

    public BucerTilknyttetBehandlingDto(List<BucInformasjonDto> bucer) {
        this.bucer = bucer;
    }

    public List<BucInformasjonDto> getBucer() {
        return bucer;
    }
}
