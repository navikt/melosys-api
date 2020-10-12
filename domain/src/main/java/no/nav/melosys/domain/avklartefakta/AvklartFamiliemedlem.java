package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public class AvklartFamiliemedlem {

    public final String fnr;
    public final Medfolgende_barn_begrunnelser begrunnelse;

    public AvklartFamiliemedlem(String fnr, String begrunnelse) {
        this.fnr = fnr;
        this.begrunnelse = begrunnelse == null ? null : Medfolgende_barn_begrunnelser.valueOf(begrunnelse);
    }

    public boolean erOmfattet() {
        return begrunnelse == null;
    }
}
