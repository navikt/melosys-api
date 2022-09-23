package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;

import java.time.Instant;

public class MangelbrevBruker extends Mangelbrev {

    private MangelbrevBruker(MangelbrevBrevbestilling brevbestilling, Instant datoInnsendingsfrist, boolean toggleEnabled) {
        super(brevbestilling, datoInnsendingsfrist, toggleEnabled);
    }

    public static MangelbrevBruker av(MangelbrevBrevbestilling brevbestilling, Instant datoInnsendingsfrist, boolean toggleEnabled) {
        return new MangelbrevBruker(brevbestilling, datoInnsendingsfrist, toggleEnabled);
    }
}
