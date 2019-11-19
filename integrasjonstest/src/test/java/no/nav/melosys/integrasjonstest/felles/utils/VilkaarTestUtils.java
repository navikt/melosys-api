package no.nav.melosys.integrasjonstest.felles.utils;

import java.util.ArrayList;

import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.service.vilkaar.VilkaarDto;

public final class VilkaarTestUtils {
    private VilkaarTestUtils() {}

    public static VilkaarDto lagVilkaarDto(Vilkaar vilkaar, boolean oppfylt, Kodeverk... vilkårbegrunnelser) {
        VilkaarDto vilkaarDto = new VilkaarDto();
        vilkaarDto.setOppfylt(oppfylt);
        vilkaarDto.setVilkaar(vilkaar.getKode());
        vilkaarDto.setBegrunnelseKoder(new ArrayList<>());
        for (Kodeverk begrunnelseKode : vilkårbegrunnelser) {
            vilkaarDto.getBegrunnelseKoder().add(begrunnelseKode.getKode());
        }
        return vilkaarDto;
    }
}
