package no.nav.melosys.domain.familie;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public class IkkeOmfattetBarn {

    public String uuid;
    public final Medfolgende_barn_begrunnelser begrunnelse;
    private final String begrunnnelseFritekst;
    public String sammensattNavn;

    public IkkeOmfattetBarn(String uuid, String begrunnelse, String begrunnnelseFritekst) {
        this.uuid = uuid;
        this.begrunnelse = begrunnelse == null ? null : Medfolgende_barn_begrunnelser.valueOf(begrunnelse);
        this.begrunnnelseFritekst = begrunnnelseFritekst;
    }

    String getBegrunnnelseFritekst() {
        return begrunnnelseFritekst;
    }
}
