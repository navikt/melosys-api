package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DistribuerJournalpostTest {

    @Mock
    private DoksysFasade mockDoksysFasade;

    private DistribuerJournalpost distribuerJournalpost;

    @BeforeEach
    void init() {
        distribuerJournalpost = new DistribuerJournalpost(mockDoksysFasade);
    }

    @Test
    void utførFeilerJournalpostIdMangler() {
        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(new Prosessinstans()));
    }

    @Test
    void utførDistribuerJournalpostUtenAdresse() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        String journalpostId = "12345";

        prosessinstans.setData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(eq(journalpostId));
    }

    @Test
    void utførDistribuerJournalpostMedAdresse() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        String journalpostId = "12345";

        prosessinstans.setData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);
        prosessinstans.setData(ProsessDataKey.DISTRIBUER_OVERSTYR_MOTTAKER, new StrukturertAdresse());

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(eq(journalpostId), any(StrukturertAdresse.class));
    }

}