package no.nav.melosys.saksflyt.steg.ul;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.saksflyt.SaksflytTestUtils.lagUtenlandskMyndighet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UtpekLandDistribuerJournalpostTest {

    @Mock
    private DoksysFasade doksysFasade;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    private UtpekLandDistribuerJournalpost utpekLandDistribuerJournalpost;

    @Before
    public void settOpp() throws TekniskException {
        utpekLandDistribuerJournalpost = new UtpekLandDistribuerJournalpost(doksysFasade, utenlandskMyndighetService);
        when(utenlandskMyndighetService.hentUtenlandskMyndighet(any())).thenReturn(lagUtenlandskMyndighet());
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "12345");

        utpekLandDistribuerJournalpost.utfør(prosessinstans);

        ArgumentCaptor<StrukturertAdresse> captor = ArgumentCaptor.forClass(StrukturertAdresse.class);
        verify(doksysFasade).distribuerJournalpost(eq("12345"), captor.capture());

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);

        StrukturertAdresse strukturertAdresse = captor.getValue();
        assertThat(strukturertAdresse).isNotNull();
        assertThat(strukturertAdresse.gatenavn).isEqualTo("Svenskegatan 38");
        assertThat(strukturertAdresse.postnummer).isEqualTo("8080");
    }
}
