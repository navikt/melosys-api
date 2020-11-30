package no.nav.melosys.domain.familie;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public class IkkeOmfattetBarn {

    public final String fnr;
    public final Medfolgende_barn_begrunnelser begrunnelse;
    private final String begrunnnelseFritekst;
    public String sammensattNavn;

    public IkkeOmfattetBarn(String fnr, String begrunnelse, String begrunnnelseFritekst) {
        this.fnr = fnr;
        this.begrunnelse = begrunnelse == null ? null : Medfolgende_barn_begrunnelser.valueOf(begrunnelse);
        this.begrunnnelseFritekst = begrunnnelseFritekst;
    }

    String getBegrunnnelseFritekst() {
        return begrunnnelseFritekst;
    }
}
