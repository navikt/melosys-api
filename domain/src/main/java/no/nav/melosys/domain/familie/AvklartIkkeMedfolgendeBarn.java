package no.nav.melosys.domain.familie;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public class AvklartIkkeMedfolgendeBarn {

    public final String fnr;
    public final Medfolgende_barn_begrunnelser begrunnelse;
    private final String begrunnnelseFritekst;

    public AvklartIkkeMedfolgendeBarn(String fnr, String begrunnelse, String begrunnnelseFritekst) {
        this.fnr = fnr;
        this.begrunnelse = begrunnelse == null ? null : Medfolgende_barn_begrunnelser.valueOf(begrunnelse);
        this.begrunnnelseFritekst = begrunnnelseFritekst;
    }

    String getBegrunnnelseFritekst() {
        return begrunnnelseFritekst;
    }
}
