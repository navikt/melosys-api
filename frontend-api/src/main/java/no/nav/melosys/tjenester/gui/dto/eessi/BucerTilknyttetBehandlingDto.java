package no.nav.melosys.tjenester.gui.dto.eessi;

import java.util.List;

import no.nav.melosys.domain.eessi.BucInformasjon;

public class BucerTilknyttetBehandlingDto {

    private final List<BucInformasjon> bucer;

    public BucerTilknyttetBehandlingDto(List<BucInformasjon> bucer) {
        this.bucer = bucer;
    }

    public List<BucInformasjon> getBucer() {
        return bucer;
    }
}
