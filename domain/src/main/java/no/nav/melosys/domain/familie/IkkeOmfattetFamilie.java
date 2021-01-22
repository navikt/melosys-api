package no.nav.melosys.domain.familie;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public class IkkeOmfattetFamilie {
    private final String uuid;
    private final Kodeverk begrunnelse;
    private final String begrunnelseFritekst;

    public IkkeOmfattetFamilie(String uuid, Kodeverk begrunnelse, String begrunnelseFritekst) {
        this.uuid = uuid;
        this.begrunnelse = begrunnelse;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public String getUuid() {
        return uuid;
    }

    public Kodeverk getBegrunnelse() {
        return begrunnelse;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }
}
