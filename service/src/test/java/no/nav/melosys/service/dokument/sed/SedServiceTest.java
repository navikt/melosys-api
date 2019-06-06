package no.nav.melosys.service.dokument.sed;

import java.util.List;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.InstitusjonDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.SedinfoDto;
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
public class SedServiceTest {

    @Mock
    private SedDataBygger sedDataBygger;
    @Mock
    private EessiConsumer eessiConsumer;

    private SedService sedService;

    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        sedService = new SedService(sedDataBygger, eessiConsumer, "true");

        behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("123");

        behandlingsresultat = new Behandlingsresultat();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.SK);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));

        when(sedDataBygger.lag(any(Behandling.class))).thenReturn(new SedDataDto());
    }

    @Test
    public void opprettOgSendSed_verifiserKorrektSedType() throws Exception {
        sedService.opprettOgSendSed(behandling, behandlingsresultat);
        verify(eessiConsumer).opprettOgSendSed(any(SedDataDto.class));
    }

//    @Test
//    public void opprettOgSendSed_ingenLovvalgsperiode_forventException() throws Exception {
//        expectedException.expect(TekniskException.class);
//        expectedException.expectMessage("Finner ingen lovvalgsperiode!");
//        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet());
//        sedService.opprettOgSendSed(behandling, behandlingsresultat);
//    } TODO kommenter inn når sed-feilmeldinger blir kastet fra service igjen

    @Test
    public void opprettBucOgSed_verifiserKorrektSedType() throws Exception {
        sedService.opprettBucOgSed(behandling, "LA_BUC_01", "SE", "SE:001");
        verify(eessiConsumer).opprettBucOgSed(any(SedDataDto.class), eq("LA_BUC_01"));
    }

    @Test
    public void hentMottakerinstitusjoner_verifiserConsumerKall() throws MelosysException {
        sedService.hentMottakerinstitusjoner("LA_BUC_01");
        verify(eessiConsumer).hentMottakerinstitusjoner(anyString());
    }

    @Test
    public void hentMottakerinstitusjoner_medFeilIConsumer_forventTomListe() throws MelosysException {
        when(eessiConsumer.hentMottakerinstitusjoner(anyString())).thenThrow(new IntegrasjonException("Error!"));

        List<InstitusjonDto> institusjonDtoer = sedService.hentMottakerinstitusjoner("LA_BUC_01");

        verify(eessiConsumer).hentMottakerinstitusjoner(anyString());
        assertThat(institusjonDtoer).isEmpty();
    }

    @Test
    public void hentTilknyttedeSeder_verifiserConsumerKall() throws MelosysException {
        sedService.hentTilknyttedeSeder(123L);
        verify(eessiConsumer).hentTilknyttedeSedUtkast(anyLong());
    }

    @Test
    public void hentTilknyttedeSeder_medFeilIConsumer_forventTomList() throws MelosysException {
        when(eessiConsumer.hentTilknyttedeSedUtkast(anyLong())).thenThrow(new IntegrasjonException("Error!"));

        List<SedinfoDto> sedinfoDtoer = sedService.hentTilknyttedeSeder(123L);

        verify(eessiConsumer).hentTilknyttedeSedUtkast(anyLong());
        assertThat(sedinfoDtoer).isEmpty();
    }
}