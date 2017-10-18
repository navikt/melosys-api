package no.nav.melosys.domain.dokument.medlemskap;

import no.nav.melosys.domain.dokument.KodeverkEnum;

/**
 * Denne enumen er en hardkoding av kodeverket PeriodetypeMedl.
 */
public enum Periodetype implements KodeverkEnum<Periodetype> {
    PMMEDSKP("Periode med medlemskap"),
    PUMEDSKP("Periode uten medlemskap"),
    E500INFO("Utenlandsk id");

    private String navn;

    Periodetype(String navn) {
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return null;
    }
}
