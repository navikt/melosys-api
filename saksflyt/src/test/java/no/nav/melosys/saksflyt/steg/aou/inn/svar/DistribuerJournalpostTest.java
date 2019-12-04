package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import java.util.Collections;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DistribuerJournalpostTest {

    @Mock
    private DoksysFasade doksysFasade;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    private DistribuerJournalpost distribuerJournalpost;

    @Before
    public void setup() throws TekniskException {
        distribuerJournalpost = new DistribuerJournalpost(doksysFasade, utenlandskMyndighetService);

        when(utenlandskMyndighetService.hentUtenlandskMyndighet(any(Landkoder.class))).thenReturn(lagUtenlandskMyndighet());
    }

    @Test
    public void utfør() throws MelosysException {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.MYNDIGHET);
        aktoer.setAktørId("123");
        aktoer.setInstitusjonId("SE:id");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Collections.singleton(aktoer));
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "12345");

        distribuerJournalpost.utfør(prosessinstans);

        ArgumentCaptor<StrukturertAdresse> captor = ArgumentCaptor.forClass(StrukturertAdresse.class);
        verify(doksysFasade).distribuerJournalpost(eq("12345"), captor.capture());

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_SAK_OG_BEHANDLING_AVSLUTTET);

        StrukturertAdresse strukturertAdresse = captor.getValue();
        assertThat(strukturertAdresse).isNotNull();
        assertThat(strukturertAdresse.gatenavn).isEqualTo("Svenskegatan 38");
        assertThat(strukturertAdresse.postnummer).isEqualTo("8080");
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.institusjonskode = "123456";
        utenlandskMyndighet.landkode = Landkoder.SE;
        utenlandskMyndighet.navn = "Svenska myndighetan";
        utenlandskMyndighet.gateadresse = "Svenskegatan 38";
        utenlandskMyndighet.poststed = "Svenska stan";
        utenlandskMyndighet.postnummer = "8080";
        return utenlandskMyndighet;
    }
}