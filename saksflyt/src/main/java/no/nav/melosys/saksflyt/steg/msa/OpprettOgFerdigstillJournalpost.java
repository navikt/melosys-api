package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

@Component
public class OpprettOgFerdigstillJournalpost implements StegBehandler {

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.MSA_OPPRETT_OG_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        throw new NotImplementedException("Støtter ikke opprettelse av journalpost");

    }
}
