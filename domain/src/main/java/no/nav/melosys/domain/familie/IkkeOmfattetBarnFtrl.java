package no.nav.melosys.domain.familie;

import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;

public class IkkeOmfattetBarnFtrl extends IkkeOmfattetFamilie {

    public IkkeOmfattetBarnFtrl(String uuid, String begrunnelse, String begrunnelseFritekst) {
        super(
            uuid,
            begrunnelse == null ? null : Medfolgende_barn_begrunnelser_ftrl.valueOf(begrunnelse),
            begrunnelseFritekst
        );
    }
}
