package no.nav.melosys.domain.familie;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public class IkkeOmfattetBarn {

    public final String fnrEllerUuid;
    public final Medfolgende_barn_begrunnelser begrunnelse;
    private final String begrunnnelseFritekst;
    public String sammensattNavn;

    public IkkeOmfattetBarn(String fnrEllerUuid, String begrunnelse, String begrunnnelseFritekst) {
        this.fnrEllerUuid = fnrEllerUuid;
        this.begrunnelse = begrunnelse == null ? null : Medfolgende_barn_begrunnelser.valueOf(begrunnelse);
        this.begrunnnelseFritekst = begrunnnelseFritekst;
    }

    String getBegrunnnelseFritekst() {
        return begrunnnelseFritekst;
    }
}
