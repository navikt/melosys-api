package no.nav.melosys.saksflyt.steg.vs;

import java.util.Collections;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.LandvelgerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.saksflyt.SaksflytTestUtils.lagUtenlandskMyndighet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DistribuerJournalpostTest {
    @Mock
    private DoksysFasade doksysFasade;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    private DistribuerJournalpost distribuerJournalpost;

    @Before
    public void setup() throws TekniskException, IkkeFunnetException {
        distribuerJournalpost = new DistribuerJournalpost(doksysFasade, behandlingsgrunnlagService, landvelgerService, utenlandskMyndighetService);
        when(landvelgerService.hentBostedsland(anyLong(), any())).thenReturn(Landkoder.SE);
        when(utenlandskMyndighetService.hentUtenlandskMyndighet(any())).thenReturn(lagUtenlandskMyndighet());
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(anyLong())).thenReturn(new Behandlingsgrunnlag());
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
        behandling.setId(1L);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "12345");

        distribuerJournalpost.utfør(prosessinstans);

        ArgumentCaptor<StrukturertAdresse> captor = ArgumentCaptor.forClass(StrukturertAdresse.class);
        verify(doksysFasade).distribuerJournalpost(eq("12345"), captor.capture());

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_STATUS_BEH_AVSL);

        StrukturertAdresse strukturertAdresse = captor.getValue();
        assertThat(strukturertAdresse).isNotNull();
        assertThat(strukturertAdresse.gatenavn).isEqualTo("Svenskegatan 38");
        assertThat(strukturertAdresse.postnummer).isEqualTo("8080");
    }
}
