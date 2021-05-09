package no.nav.melosys.saksflyt.steg.sed;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.Vedlegg;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedSomBrevService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideresendSoknadTest {
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private SedSomBrevService sedSomBrevService;

    private VideresendSoknad videresendSoknad;

    private final Behandling behandling = new Behandling();
    private final Journalpost journalpost = new Journalpost("123");

    private static final String MOTTAKER_INSTITUSJON = "SE:123";

    @BeforeEach
    void setup() throws SikkerhetsbegrensningException, IntegrasjonException {
        videresendSoknad = new VideresendSoknad(eessiService, behandlingsresultatService,
            joarkFasade, fagsakService, sedSomBrevService);

        behandling.setId(1L);
        behandling.setInitierendeJournalpostId("123");
        journalpost.setHoveddokument(new ArkivDokument());
        journalpost.getHoveddokument().setTittel("tittel på deg");
        journalpost.getHoveddokument().setDokumentId("44444");
    }

    @Test
    void utfør_vedleggFinnesIkke_forventFunksjonellException() {
        Prosessinstans prosessinstans = opprettProsessinstans();
        prosessinstans.getBehandling().setInitierendeJournalpostId(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknad.utfør(prosessinstans))
            .withMessageContaining("Kan ikke videresende søknad uten vedlegg");
    }

    @Test
    void utfør_skalSendesUtlandErEessiKlar_senderSedIBuc3() {
        Prosessinstans prosessinstans = opprettProsessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MOTTAKERE, List.of("SE:123"));

        Behandling behandling = prosessinstans.getBehandling();
        Long behandlingID = 1L;
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

        final byte[] vedlegg = new byte[10];
        final var dokumentReferanse = new DokumentReferanse(behandling.getInitierendeJournalpostId(),
            journalpost.getHoveddokument().getDokumentId());
        prosessinstans.setData(ProsessDataKey.VEDLEGG_SED, Set.of(dokumentReferanse));
        final Vedlegg forventetVedlegg = new Vedlegg(vedlegg, "tittel");
        when(eessiService.lagEessiVedlegg(any(), anyCollection())).thenReturn(Set.of(forventetVedlegg));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        videresendSoknad.utfør(prosessinstans);

        verify(eessiService).opprettOgSendSed(eq(behandlingID), eq(List.of(MOTTAKER_INSTITUSJON)), eq(BucType.LA_BUC_03),
            argThat(collection -> collection.contains(forventetVedlegg)), isNull());
        assertThat(prosessinstans.getData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID)).isNull();
    }

    @Test
    void utfør_skalSendesUtlandErIkkeEessiKlar_senderA008SomBrev() {
        Prosessinstans prosessinstans = opprettProsessinstans();
        Behandling behandling = prosessinstans.getBehandling();
        String opprettetJournalpostID = "532523";

        byte[] vedlegg = new byte[10];
        prosessinstans.setData(ProsessDataKey.VEDLEGG_SED,
            Set.of(new DokumentReferanse(behandling.getInitierendeJournalpostId(), journalpost.getHoveddokument().getDokumentId())));

        when(joarkFasade.hentJournalpost(behandling.getInitierendeJournalpostId())).thenReturn(journalpost);
        when(joarkFasade.hentDokument(behandling.getInitierendeJournalpostId(), journalpost.getHoveddokument().getDokumentId()))
            .thenReturn(vedlegg);
        when(sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(any(SedType.class), any(Landkoder.class), any(), any()))
            .thenReturn(opprettetJournalpostID);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        behandlingsresultat.setBehandling(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(fagsakService.hentFagsak(any())).thenReturn(lagFagsak());

        videresendSoknad.utfør(prosessinstans);

        verify(sedSomBrevService)
            .lagJournalpostForSendingAvSedSomBrev(eq(SedType.A008), any(Landkoder.class), eq(behandling), anyList());
        assertThat(prosessinstans.getData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID)).isEqualTo(opprettetJournalpostID);
        assertThat(prosessinstans.getData(ProsessDataKey.DISTRIBUER_MOTTAKER_LAND, Landkoder.class)).isEqualTo(Landkoder.SE);
    }

    private Prosessinstans opprettProsessinstans() {
        behandling.setFagsak(lagFagsak());

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }

    private static Fagsak lagFagsak() {
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
        fagsak.setAktører(Set.of(myndighet, bruker));

        return fagsak;
    }
}
