package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;

import java.time.Instant;

public class MangelbrevBruker extends Mangelbrev {

    private MangelbrevBruker(MangelbrevBrevbestilling brevbestilling, Instant datoInnsendingsfrist) {
        super(brevbestilling, datoInnsendingsfrist);
    }

    public static MangelbrevBruker av(MangelbrevBrevbestilling brevbestilling, Instant datoInnsendingsfrist) {
        return new MangelbrevBruker(brevbestilling, datoInnsendingsfrist);
    }
}
