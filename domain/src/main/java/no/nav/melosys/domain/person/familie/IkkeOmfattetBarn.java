package no.nav.melosys.domain.person.familie;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public class IkkeOmfattetBarn {

    public final String uuid;
    public final Medfolgende_barn_begrunnelser begrunnelse;
    private final String begrunnelseFritekst;
    public String sammensattNavn;

    public IkkeOmfattetBarn(String uuid, String begrunnelse, String begrunnelseFritekst) {
        this.uuid = uuid;
        this.begrunnelse = begrunnelse == null ? null : Medfolgende_barn_begrunnelser.valueOf(begrunnelse);
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }
}
