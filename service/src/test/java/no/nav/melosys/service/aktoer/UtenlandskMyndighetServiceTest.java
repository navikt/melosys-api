package no.nav.melosys.service.aktoer;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

    @Captor
    ArgumentCaptor<List<String>> stringListArgumentCaptor;

    private final long BEHANDLING_ID = 1L;

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


        verify(fagsakServiceMock).oppdaterMyndighetForTrygdeavtale(FagsakTestFactory.SAKSNUMMER, Land_iso2.NO);
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
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(List.of(Land_iso2.SE));
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.setLandkode(Land_iso2.SE);
        when(utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.SE))
            .thenReturn(Optional.of(utenlandskMyndighet));


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);


        verify(fagsakServiceMock).oppdaterMyndigheterForEuEos(eq(FagsakTestFactory.SAKSNUMMER), anyCollection());
        verifyNoMoreInteractions(fagsakServiceMock);
    }

    @Test
    void avklarUtenlandskMyndighetSomAktørOgLagre_oppdatererMyndigheterMedRiktigId() {
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID))
            .thenReturn(List.of(Land_iso2.SE, Land_iso2.DK));

        UtenlandskMyndighet svenskUtenlandskMyndighet = new UtenlandskMyndighet();
        svenskUtenlandskMyndighet.setLandkode(Land_iso2.SE);
        svenskUtenlandskMyndighet.setInstitusjonskode("INSTITUSJONSKODE");
        UtenlandskMyndighet danskUtenlandskMyndighet = new UtenlandskMyndighet();
        danskUtenlandskMyndighet.setLandkode(Land_iso2.DK);
        danskUtenlandskMyndighet.setInstitusjonskode(null);
        when(utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.SE)).thenReturn(Optional.of(svenskUtenlandskMyndighet));
        when(utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.DK)).thenReturn(Optional.of(danskUtenlandskMyndighet));


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);


        verify(fagsakServiceMock).oppdaterMyndigheterForEuEos(FagsakTestFactory.SAKSNUMMER, List.of("SE:INSTITUSJONSKODE", "DK"));
        verifyNoMoreInteractions(fagsakServiceMock);
    }

    @Test
    void avklarUtenlandskMyndighetSomAktørOgLagre_kasterIkkeFunnetException_nårUtenlandskmyndighetIkkeErFunnet() {
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
    void avklarUtenlandskMyndighetSomAktørOgLagre_forventkorrektInstitusjonsId() {
        var utenlandskMyndighet = lagUtenlandskMyndighet(Land_iso2.IT, "IT123", null);
        var utenlandskMyndighetReservert = lagUtenlandskMyndighet(Land_iso2.CZ, "CZ123", Preferanse.PreferanseEnum.RESERVERT_FRA_A1);

        when(utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.IT)).thenReturn(Optional.of(utenlandskMyndighet));
        when(utenlandskMyndighetRepositoryMock.findByLandkode(Land_iso2.CZ)).thenReturn(Optional.of(utenlandskMyndighetReservert));
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(Arrays.asList(Land_iso2.IT, Land_iso2.CZ));


        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);


        verify(fagsakServiceMock).oppdaterMyndigheterForEuEos(eq(behandling.getFagsak().getSaksnummer()), stringListArgumentCaptor.capture());
        assertThat(stringListArgumentCaptor.getValue()).containsExactlyInAnyOrder(Landkoder.IT + ":" + utenlandskMyndighet.getInstitusjonskode(), Landkoder.CZ + ":" + utenlandskMyndighetReservert.getInstitusjonskode());
    }

    @Test
    void lagUtenlandskeMyndigheterFraBehandling_mapperUtenlandskmyndighetTilAktør() {
        Collection<Land_iso2> utenlandskeMyndigheterLandkoder = List.of(Land_iso2.SE, Land_iso2.DK);
        when(landvelgerServiceMock.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID))
            .thenReturn(utenlandskeMyndigheterLandkoder);

        UtenlandskMyndighet svenskUtenlandskMyndighet = new UtenlandskMyndighet();
        svenskUtenlandskMyndighet.setLandkode(Land_iso2.SE);
        svenskUtenlandskMyndighet.setInstitusjonskode("INSTSE");
        svenskUtenlandskMyndighet.setPostnummer("123");
        UtenlandskMyndighet danskUtenlandskMyndighet = new UtenlandskMyndighet();
        danskUtenlandskMyndighet.setLandkode(Land_iso2.DK);
        danskUtenlandskMyndighet.setInstitusjonskode("INSTDK");
        danskUtenlandskMyndighet.setPostnummer("123");
        List<UtenlandskMyndighet> utenlandskMyndighetList = List.of(
            svenskUtenlandskMyndighet, danskUtenlandskMyndighet
        );
        when(utenlandskMyndighetRepositoryMock.findByLandkodeIsIn(utenlandskeMyndigheterLandkoder)).thenReturn(
            utenlandskMyndighetList
        );


        Map<UtenlandskMyndighet, Mottaker> resultat = utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling);


        assertThat(resultat).extractingFromEntries(
                Map.Entry::getKey,
                entry -> entry.getValue().getRolle(),
                entry -> entry.getValue().getInstitusjonID()
            )
            .containsExactly(Tuple.tuple(
                    svenskUtenlandskMyndighet,
                    Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET,
                    "SE:INSTSE"
                ),
                Tuple.tuple(
                    danskUtenlandskMyndighet,
                    Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET,
                    "DK:INSTDK"
                )
            );
    }

    @Test
    void lagUtenlandskeMyndigheterFraBehandling_forventAktoerMedGyldigInstitusjonsId() {
        var utenlandskMyndighet = lagUtenlandskMyndighet(Land_iso2.IT, "IT123", null);
        var utenlandskMyndighetReservert = lagUtenlandskMyndighet(Land_iso2.CZ, "CZ123", Preferanse.PreferanseEnum.RESERVERT_FRA_A1);

        when(utenlandskMyndighetRepositoryMock.findByLandkodeIsIn(anyCollection())).thenReturn(Arrays.asList(utenlandskMyndighet, utenlandskMyndighetReservert));


        Map<UtenlandskMyndighet, Mottaker> mottakere = utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling);


        assertThat(mottakere).isNotEmpty();
        assertThat(mottakere.values().iterator().next().getInstitusjonID()).isEqualTo(Landkoder.IT + ":" + utenlandskMyndighet.getInstitusjonskode());
    }


    private Behandling lagBehandling() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setFagsak(fagsak);

        return behandling;
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet(Land_iso2 landkode, String institusjonID, Preferanse.PreferanseEnum preferanse) {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.setInstitusjonskode(institusjonID);
        utenlandskMyndighet.setLandkode(landkode);
        utenlandskMyndighet.setPostnummer("123");
        if (preferanse != null) {
            var preferanser = new HashSet<Preferanse>();
            preferanser.add(new Preferanse(1L, preferanse));
            utenlandskMyndighet.setPreferanser(preferanser);
        }
        return utenlandskMyndighet;
    }
}
