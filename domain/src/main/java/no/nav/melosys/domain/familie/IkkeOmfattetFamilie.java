package no.nav.melosys.domain.familie;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public class IkkeOmfattetFamilie {
    public final String uuid;
    public final Kodeverk begrunnelse;
    public final String begrunnelseFritekst;

    public IkkeOmfattetFamilie(String uuid, Kodeverk begrunnelse, String begrunnelseFritekst) {
        this.uuid = uuid;
        this.begrunnelse = begrunnelse;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }
}
