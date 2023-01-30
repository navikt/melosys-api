package no.nav.melosys.service.aktoer;

import java.util.*;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtenlandskMyndighetServiceTest {

    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepositoryMock;

    @Mock
    private FagsakService fagsakServiceMock;

    @Mock
    private LandvelgerService landvelgerServiceMock;

    private UtenlandskMyndighetService utenlandskMyndighetService;

    private final long BEHANDLING_ID = 1L;
    private final String SAKSNUMMER = "MEL-1";

    Behandling behandling;

    @BeforeEach
    void init() {
        utenlandskMyndighetService = new UtenlandskMyndighetService(utenlandskMyndighetRepositoryMock, landvelgerServiceMock, fagsakServiceMock);
        behandling = lagBehandling();
    }

    @Test
    void avklarUtenlandskMyndighetSomAktørOgLagre_oppdatererMyndighetForTrygdeavtale() {
        behandling.getFagsak().setType(Sakstyper.TRYGDEAVTALE);
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(List.of(Land_iso2.NO));


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);


        verify(fagsakServiceMock).oppdaterMyndighetForTrygdeavtale(SAKSNUMMER, Land_iso2.NO);
        verifyNoMoreInteractions(fagsakServiceMock);
    }

    @Test
    void avklarUtenlandskMyndighetSomAktørOgLagre_kasterFunksjonellException_nårDetErFlereLandkoder() {
        behandling.getFagsak().setType(Sakstyper.TRYGDEAVTALE);
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(List.of(Land_iso2.NO,
            Land_iso2.BE));


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling))
            .withMessageContaining("Fant ingen eller flere enn ett trygdemyndighetsland" +
                " for bilaterale trygdeavtaler.");
    }

    @Test
    void avklarUtenlandskMyndighetSomAktørOgLagre_oppdatererMyndigheterForEuEos() {
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(List.of(Land_iso2.SE));
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.landkode = Land_iso2.SE;
        when(utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.SE))
            .thenReturn(Optional.of(utenlandskMyndighet));


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);


        verify(fagsakServiceMock).oppdaterMyndigheterForEuEos(eq(SAKSNUMMER), anyCollection());
        verifyNoMoreInteractions(fagsakServiceMock);
    }

    @Test
    void avklarUtenlandskMyndighetSomAktørOgLagre_oppdatererMyndigheterMedRiktigId() {
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID))
            .thenReturn(List.of(Land_iso2.SE, Land_iso2.DK));

        UtenlandskMyndighet svenskUtenlandskMyndighet = new UtenlandskMyndighet();
        svenskUtenlandskMyndighet.landkode = Land_iso2.SE;
        svenskUtenlandskMyndighet.institusjonskode = "INSTITUSJONSKODE";
        UtenlandskMyndighet danskUtenlandskMyndighet = new UtenlandskMyndighet();
        danskUtenlandskMyndighet.landkode = Land_iso2.DK;
        danskUtenlandskMyndighet.institusjonskode = null;
        when(utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.SE)).thenReturn(Optional.of(svenskUtenlandskMyndighet));
        when(utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.DK)).thenReturn(Optional.of(danskUtenlandskMyndighet));


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);


        verify(fagsakServiceMock).oppdaterMyndigheterForEuEos(SAKSNUMMER, List.of("SE:INSTITUSJONSKODE", "DK"));
        verifyNoMoreInteractions(fagsakServiceMock);
    }

    @Test
    void avklarUtenlandskMyndighetSomAktørOgLagre_kasterIkkeFunnetException_nårUtenlandskmyndighetIkkeErFunnet() {
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(List.of(Land_iso2.SE));
        when(utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.SE)).thenReturn(Optional.empty());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling))
            .withMessageContaining("Finner ikke utenlandskMyndighet for SE.");
    }

    @Test
    void hentUtenlandskMyndighet_kasterIkkeFunnetException_nårUtenlandskmyndighetIkkeErFunnet() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> utenlandskMyndighetService.hentUtenlandskMyndighet(Land_iso2.SE, null))
            .withMessageContaining("Finner ikke utenlandskMyndighet for SE.");
    }

    @Test
    void lagUtenlandskeMyndigheterFraBehandling_svelgerIkkeFunnetException_nårLandvelgerIkkeFinnerUtenlandskMyndighet() {
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID))
            .thenThrow(new IkkeFunnetException("asd"));

        assertThatNoException().isThrownBy(() -> utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling));
        verify(utenlandskMyndighetRepositoryMock).findByLandkodeIsIn(Collections.emptyList());
    }

    @Test
    void lagUtenlandskeMyndigheterFraBehandling_mapperUtenlandskmyndighetTilAktør() {
        Collection<Land_iso2> utenlandskeMyndigheterLandkoder = List.of(Land_iso2.SE, Land_iso2.DK);
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID))
            .thenReturn(utenlandskeMyndigheterLandkoder);

        UtenlandskMyndighet svenskUtenlandskMyndighet = new UtenlandskMyndighet();
        svenskUtenlandskMyndighet.landkode = Land_iso2.SE;
        svenskUtenlandskMyndighet.institusjonskode = "INSTSE";
        UtenlandskMyndighet danskUtenlandskMyndighet = new UtenlandskMyndighet();
        danskUtenlandskMyndighet.landkode = Land_iso2.DK;
        danskUtenlandskMyndighet.institusjonskode = "INSTDK";
        List<UtenlandskMyndighet> utenlandskMyndighetList = List.of(
            svenskUtenlandskMyndighet, danskUtenlandskMyndighet
        );
        when(utenlandskMyndighetRepositoryMock.findByLandkodeIsIn(utenlandskeMyndigheterLandkoder)).thenReturn(
            utenlandskMyndighetList
        );


        Map<UtenlandskMyndighet, Aktoer> resultat = utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling);


        assertThat(resultat).extractingFromEntries(
                Map.Entry::getKey,
                entry -> entry.getValue().getRolle(),
                entry -> entry.getValue().getInstitusjonId()
            )
            .containsExactly(Tuple.tuple(
                    svenskUtenlandskMyndighet,
                    Aktoersroller.TRYGDEMYNDIGHET,
                    "SE:INSTSE"
                ),
                Tuple.tuple(
                    danskUtenlandskMyndighet,
                    Aktoersroller.TRYGDEMYNDIGHET,
                    "DK:INSTDK"
                )
            );
    }

    private Behandling lagBehandling() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setFagsak(fagsak);

        return behandling;
    }
}
