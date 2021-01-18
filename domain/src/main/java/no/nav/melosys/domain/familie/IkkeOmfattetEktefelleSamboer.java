package no.nav.melosys.domain.familie;

import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;

public class IkkeOmfattetEktefelleSamboer extends IkkeOmfattetFamilie {

    public IkkeOmfattetEktefelleSamboer(String uuid, String begrunnelse, String begrunnelseFritekst) {
        super(
            uuid,
            begrunnelse == null ? null : Medfolgende_ektefelle_samboer_begrunnelser_ftrl.valueOf(begrunnelse),
            begrunnelseFritekst
        );
    }
}
