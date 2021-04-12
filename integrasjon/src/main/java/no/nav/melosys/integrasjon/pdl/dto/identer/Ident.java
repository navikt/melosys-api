package no.nav.melosys.integrasjon.pdl.dto.identer;

import static no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.AKTORID;
import static no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.FOLKEREGISTERIDENT;

public record Ident(String ident, IdentGruppe gruppe) {
    public boolean erAktørID() {
        return gruppe == AKTORID;
    }

    public boolean erFolkeregisterIdent() {
        return gruppe == FOLKEREGISTERIDENT;
    }
}
