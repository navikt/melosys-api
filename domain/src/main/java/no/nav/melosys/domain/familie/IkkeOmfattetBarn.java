package no.nav.melosys.domain.familie;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

import static no.nav.melosys.domain.familie.AvklarteMedfolgendeBarn.UUID_V4_PATTERN;

public class IkkeOmfattetBarn {

    public String fnr;
    public String uuid;
    public final Medfolgende_barn_begrunnelser begrunnelse;
    private final String begrunnnelseFritekst;
    public String sammensattNavn;

    public IkkeOmfattetBarn(String ident, String begrunnelse, String begrunnnelseFritekst) {
        if (UUID_V4_PATTERN.matcher(ident).matches()) {
            uuid = ident;
        } else {
            fnr = ident;
        }
        this.begrunnelse = begrunnelse == null ? null : Medfolgende_barn_begrunnelser.valueOf(begrunnelse);
        this.begrunnnelseFritekst = begrunnnelseFritekst;
    }

    String getBegrunnnelseFritekst() {
        return begrunnnelseFritekst;
    }
}
