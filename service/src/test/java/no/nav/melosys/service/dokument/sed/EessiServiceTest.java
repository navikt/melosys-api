package no.nav.melosys.service.dokument.sed;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Sets;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EessiServiceTest {

    @Mock
    private SedDataBygger sedDataBygger;
    @Mock
    private EessiConsumer eessiConsumer;

    private EessiService eessiService;

    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EnhancedRandom enhancedRandom = new EnhancedRandomBuilder().build();

    @Before
    public void setup() throws Exception {
        eessiService = new EessiService(sedDataBygger, eessiConsumer, "true");

        behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("123");

        behandlingsresultat = new Behandlingsresultat();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.SK);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));

        when(sedDataBygger.lag(any(Behandling.class))).thenReturn(new SedDataDto());
        when(sedDataBygger.lagUtkast(any(Behandling.class))).thenReturn(new SedDataDto());
    }

    @Test
    public void opprettOgSendSed_verifiserKorrektSedType() throws Exception {
        eessiService.opprettOgSendSed(behandling, behandlingsresultat);
        verify(eessiConsumer).opprettOgSendSed(any(SedDataDto.class));
    }

//    @Test
//    public void opprettOgSendSed_ingenLovvalgsperiode_forventException() throws Exception {
//        expectedException.expect(TekniskException.class);
//        expectedException.expectMessage("Finner ingen lovvalgsperiode!");
//        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet());
//        eessiService.opprettOgSendSed(behandling, behandlingsresultat);
//    } TODO kommenter inn når sed-feilmeldinger blir kastet fra service igjen

    @Test
    public void opprettBucOgSed_verifiserKorrektSedType() throws Exception {
        when(eessiConsumer.opprettBucOgSed(any(SedDataDto.class), anyString())).thenReturn("localhost:3000");

        eessiService.opprettBucOgSed(behandling, "LA_BUC_01", "SE", "SE:001");
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), eq("LA_BUC_01"));
    }

    @Test
    public void hentMottakerinstitusjoner_forventListeMedRettType() throws MelosysException {
        when(eessiConsumer.hentMottakerinstitusjoner(anyString())).thenReturn(Arrays.asList(
            enhancedRandom.nextObject(Institusjon.class),
            enhancedRandom.nextObject(Institusjon.class)
        ));

        List<Institusjon> mottakerinstitusjoner = eessiService.hentMottakerinstitusjoner("LA_BUC_01");

        verify(eessiConsumer).hentMottakerinstitusjoner(anyString());
        assertThat(mottakerinstitusjoner).hasSize(2);
        assertThat(mottakerinstitusjoner).hasOnlyElementsOfType(Institusjon.class);
    }

    @Test(expected = MelosysException.class)
    public void hentMottakerinstitusjoner_medFeilIConsumer_forventTomListe() throws MelosysException {
        when(eessiConsumer.hentMottakerinstitusjoner(anyString())).thenThrow(new IntegrasjonException("Error!"));
        List<Institusjon> institusjon = eessiService.hentMottakerinstitusjoner("LA_BUC_01");
    }

    @Test
    public void hentTilknyttedeSeder_forventListeMedRettType() throws MelosysException {
        when(eessiConsumer.hentTilknyttedeSeder(anyLong(), anyString())).thenReturn(Arrays.asList(
            enhancedRandom.nextObject(SedInformasjon.class),
            enhancedRandom.nextObject(SedInformasjon.class),
            enhancedRandom.nextObject(SedInformasjon.class)
        ));

        List<SedInformasjon> tilknyttedeSeder = eessiService.hentTilknyttedeSeder(123L, "utkast");

        verify(eessiConsumer).hentTilknyttedeSeder(anyLong(), anyString());
        assertThat(tilknyttedeSeder).hasSize(3);
        assertThat(tilknyttedeSeder).hasOnlyElementsOfType(SedInformasjon.class);
    }

    @Test(expected = MelosysException.class)
    public void hentTilknyttedeSeder_medFeilIConsumer_forventException() throws MelosysException {
        when(eessiConsumer.hentTilknyttedeSeder(anyLong(), anyString())).thenThrow(new IntegrasjonException("Error!"));
        eessiService.hentTilknyttedeSeder(123L, "utkast");
    }
}