package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettBrevOgJournalpostTest {

    @Mock
    private EessiService eessiService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    private OpprettBrevOgJournalpost opprettBrevOgJournalpost;

    @Before
    public void setup() throws MelosysException {
        when(joarkFasade.opprettJournalpost(any(OpprettJournalpost.class), anyBoolean())).thenReturn("1234");
        when(eessiService.hentSedTypeForAnmodningUnntakSvar(anyLong())).thenReturn(SedType.A002);
        when(eessiService.genererSedForhåndsvisning(anyLong(), any(SedType.class))).thenReturn("pdf".getBytes());
        when(utenlandskMyndighetService.hentUtenlandskMyndighet(any(Landkoder.class))).thenReturn(lagUtenlandskMyndighet());
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn("123");

        opprettBrevOgJournalpost = new OpprettBrevOgJournalpost(eessiService, joarkFasade, tpsFasade, utenlandskMyndighetService);
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = opprettProsessinstans();
        opprettBrevOgJournalpost.utfør(prosessinstans);

        ArgumentCaptor<OpprettJournalpost> captor = ArgumentCaptor.forClass(OpprettJournalpost.class);
        verify(joarkFasade).opprettJournalpost(captor.capture(), eq(true));

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_DISTRIBUER_JOURNALPOST);
        assertThat(prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)).isEqualTo("1234");

        OpprettJournalpost opprettJournalpost = captor.getValue();
        assertThat(opprettJournalpost).isNotNull();
        assertThat(opprettJournalpost.getJournalførendeEnhet()).isEqualTo("4530");
    }

    private static Prosessinstans opprettProsessinstans() {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setAktørId("123");
        myndighet.setInstitusjonId("SE:id");

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("321");

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        fagsak.setGsakSaksnummer(1111L);
        fagsak.setAktører(Sets.newHashSet(myndighet, bruker));

        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.navn = "utenlandsk myndighet";
        return utenlandskMyndighet;
    }
}