package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;

import java.time.Instant;

public class MangelbrevArbeidsgiver extends Mangelbrev {

    private final String navnFullmektig;

    private MangelbrevArbeidsgiver(MangelbrevBrevbestilling brevbestilling, Instant datoInnsendingsfrist) {
        super(brevbestilling, datoInnsendingsfrist);
        this.navnFullmektig = brevbestilling.getFullmektigNavn();
    }

    public String getNavnFullmektig() {
        return navnFullmektig;
    }

    public static MangelbrevArbeidsgiver av(MangelbrevBrevbestilling brevbestilling, Instant datoInnsendingsfrist) {
        return new MangelbrevArbeidsgiver(brevbestilling, datoInnsendingsfrist);
    }
}
