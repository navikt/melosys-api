package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;

import java.time.Instant;

public class MangelbrevArbeidsgiver extends Mangelbrev {

    private final String navnFullmektig;
    private final boolean brukerSkalHaKopi;

    private MangelbrevArbeidsgiver(MangelbrevBrevbestilling brevbestilling, Instant datoInnsendingsfrist) {
        super(brevbestilling, datoInnsendingsfrist);
        this.navnFullmektig = brevbestilling.getFullmektigNavn();
        this.brukerSkalHaKopi = brevbestilling.isBrukerSkalHaKopi();
    }

    public String getNavnFullmektig() {
        return navnFullmektig;
    }

    public boolean isBrukerSkalHaKopi() {
        return brukerSkalHaKopi;
    }

    public static MangelbrevArbeidsgiver av(MangelbrevBrevbestilling brevbestilling, Instant datoInnsendingsfrist) {
        return new MangelbrevArbeidsgiver(brevbestilling, datoInnsendingsfrist);
    }
}
