package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public class IkkeOmfattetBarn {
    private final String navn;
    private final Medfolgende_barn_begrunnelser begrunnelse;

    public IkkeOmfattetBarn(String navn, Medfolgende_barn_begrunnelser begrunnelse) {
        this.navn = navn;
        this.begrunnelse = begrunnelse;
    }

    public String getNavn() {
        return navn;
    }

    public Medfolgende_barn_begrunnelser getBegrunnelse() {
        return begrunnelse;
    }
}
