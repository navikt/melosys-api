package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public class IkkeOmfattetBarn {
    private FamiliemedlemInfo info;
    private Medfolgende_barn_begrunnelser begrunnelse;

    public IkkeOmfattetBarn(FamiliemedlemInfo info, Medfolgende_barn_begrunnelser begrunnelse) {
        this.info = info;
        this.begrunnelse = begrunnelse;
    }

    public FamiliemedlemInfo getInfo() {
        return info;
    }

    public Medfolgende_barn_begrunnelser getBegrunnelse() {
        return begrunnelse;
    }
}
