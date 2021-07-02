package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public class IkkeOmfattetBarn {
    private FamilieInfo info;
    private Medfolgende_barn_begrunnelser begrunnelse;

    public IkkeOmfattetBarn(FamilieInfo info, Medfolgende_barn_begrunnelser begrunnelse) {
        this.info = info;
        this.begrunnelse = begrunnelse;
    }

    public FamilieInfo getInfo() {
        return info;
    }

    public Medfolgende_barn_begrunnelser getBegrunnelse() {
        return begrunnelse;
    }
}
