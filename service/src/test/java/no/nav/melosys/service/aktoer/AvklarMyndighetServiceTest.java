package no.nav.melosys.service.aktoer;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AvklarMyndighetServiceTest {

    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    @Mock
    private FagsakService fagsakService;

    private UtenlandskMyndighetService utenlandskMyndighetService;

    private Behandling behandling;

    private String forventetInstitusjonIdIT;
    private String forventetInstitusjonIdCZ;

    private UtenlandskMyndighet utenlandskMyndighet;
    private UtenlandskMyndighet utenlandskMyndighetReservert;

    @Captor
    ArgumentCaptor<List<String>> stringListArgumentCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        utenlandskMyndighetService = new UtenlandskMyndighetService(utenlandskMyndighetRepository, landvelgerService, fagsakService);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        fagsak.setType(Sakstyper.EU_EOS);

        behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);

        utenlandskMyndighet = lagUtenlandskMyndighet(Landkoder.IT, "IT123", null);
        utenlandskMyndighetReservert = lagUtenlandskMyndighet(Landkoder.CZ, "CZ123", Preferanse.PreferanseEnum.RESERVERT_FRA_A1);

        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(anyLong())).thenReturn(Arrays.asList(Landkoder.IT, Landkoder.CZ));

        forventetInstitusjonIdIT = Landkoder.IT + ":" + utenlandskMyndighet.institusjonskode;
        forventetInstitusjonIdCZ = Landkoder.CZ + ":" + utenlandskMyndighetReservert.institusjonskode;
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet(Landkoder landkode, String institusjonId, Preferanse.PreferanseEnum preferanse) {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.institusjonskode = institusjonId;
        utenlandskMyndighet.landkode = landkode;
        if (preferanse != null) {
            utenlandskMyndighet.preferanser.add(new Preferanse(1L, preferanse));
        }
        return utenlandskMyndighet;
    }

    @Test
    public void lagUtenlandskMyndighetFraBehandling_forventAktoerMedGyldigInstitusjonsId() {
        when(utenlandskMyndighetRepository.findByLandkodeIsIn(anyCollection())).thenReturn(Arrays.asList(utenlandskMyndighet, utenlandskMyndighetReservert));

        Map<UtenlandskMyndighet, Aktoer> aktoerer = utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(behandling);
        assertThat(aktoerer).isNotEmpty();
        assertThat(aktoerer.values().iterator().next().getInstitusjonId()).isEqualTo(forventetInstitusjonIdIT);
    }

    @Test
    public void avklarMyndighetSomAktørOgLagre_forventkorrektInstitusjonsId() {
        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.IT))).thenReturn(Optional.of(utenlandskMyndighet));
        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.CZ))).thenReturn(Optional.of(utenlandskMyndighetReservert));

        utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);

        verify(fagsakService).oppdaterMyndigheter(eq(behandling.getFagsak().getSaksnummer()), stringListArgumentCaptor.capture());
        assertThat(stringListArgumentCaptor.getValue()).containsExactlyInAnyOrder(forventetInstitusjonIdIT, forventetInstitusjonIdCZ);
    }
}
