package no.nav.melosys.saksflyt.steg.msa;

import java.util.ArrayList;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.altinn.AltinnSoeknadService;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOgFerdigstillJournalpostTest {

    @Mock
    private AltinnSoeknadService altinnSoeknadService;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private BehandlingService behandlingService;

    private OpprettOgFerdigstillJournalpost opprettOgFerdigstillJournalpost;

    private final String søknadID = "soknadid1";
    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final Behandling behandling = new Behandling();
    private final String ident = "00000000000";

    @Captor
    private ArgumentCaptor<OpprettJournalpost> captor;

    @Before
    public void setup() throws FunksjonellException {
        prosessinstans.setData(ProsessDataKey.MOTTATT_SOKNAD_ID, søknadID);

        opprettOgFerdigstillJournalpost = new OpprettOgFerdigstillJournalpost(
            altinnSoeknadService, tpsFasade, joarkFasade, behandlingService);

        AltinnDokument søknadDokument = new AltinnDokument(søknadID, "dokumentid1", "tittel1", AltinnDokument.AltinnDokumentType.SOKNAD.name(), "pdf");
        AltinnDokument fullmaktDokument = new AltinnDokument(søknadID, "dokumentid2", "tittel2", AltinnDokument.AltinnDokumentType.FULLMAKT.name(), "pdf");

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("3321231");

        Aktoer representant = new Aktoer();
        representant.setRolle(Aktoersroller.REPRESENTANT);
        representant.setRepresenterer(Representerer.BEGGE);

        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setAktører(Set.of(bruker, representant));
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        var dokumenter = new ArrayList<AltinnDokument>();
        dokumenter.add(søknadDokument);
        dokumenter.add(fullmaktDokument);
        when(altinnSoeknadService.hentDokumenterTilknyttetSoknad(eq(søknadID))).thenReturn(dokumenter);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn(ident);
        when(joarkFasade.opprettJournalpost(any(OpprettJournalpost.class), anyBoolean())).thenReturn("journalpostid123");
    }

    @Test
    public void utfør_journalpostBlirOpprettet_verifiser() throws MelosysException {
        opprettOgFerdigstillJournalpost.utfør(prosessinstans);

        verify(tpsFasade).hentIdentForAktørId(anyString());
        verify(joarkFasade).opprettJournalpost(captor.capture(), eq(true));
        verify(behandlingService).lagre(eq(behandling));

        OpprettJournalpost opprettJournalpost = captor.getValue();
        assertThat(opprettJournalpost)
            .extracting(Journalpost::getTema, Journalpost::getMottaksKanal, Journalpost::getArkivSakId, Journalpost::getBrukerId)
            .containsExactly("MED", "ALTINN", "123", ident);
        assertThat(opprettJournalpost.getHoveddokument())
            .extracting(ArkivDokument::getDokumentId, ArkivDokument::getTittel)
            .containsExactly(null, "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits");
        assertThat(opprettJournalpost.getVedlegg())
            .hasSize(1)
            .flatExtracting(ArkivDokument::getTittel)
            .containsExactly("Fullmakt");
    }


}