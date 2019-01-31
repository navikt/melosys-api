package no.nav.melosys.service.dokument.sed;

import java.time.LocalDate;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.eux.model.nav.SED;
import no.nav.melosys.integrasjon.eux.consumer.EuxConsumer;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.dokument.sed.bygger.A009DataBygger;
import no.nav.melosys.service.dokument.sed.mapper.SedDataStub;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SedServiceTest {

    @Mock
    private EuxConsumer euxConsumer;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private SedDataByggerVelger sedDataByggerVelger;

    @InjectMocks
    private SedService sedService;

    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;

    private EnhancedRandom random;

    @Before
    public void setup() throws Exception {

        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .build();

        behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("123");

        behandlingsresultat = new Behandlingsresultat();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.SK);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));

        A009DataBygger dataBygger = Mockito.mock(A009DataBygger.class);
        when(sedDataByggerVelger.hent(any(SedType.class))).thenReturn(dataBygger);

        A009Data a009Data = SedDataStub.hent(new A009Data());
        a009Data.setLovvalgsperioder(new Lovvalgsperiode());
        a009Data.getLovvalgsperiode().setLovvalgsland(Landkoder.SK);
        a009Data.getLovvalgsperiode().setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        a009Data.getLovvalgsperiode().setFom(LocalDate.now());
        a009Data.getLovvalgsperiode().setTom(LocalDate.now());
        when(dataBygger.lag(any(Behandling.class))).thenReturn(a009Data);

        when(euxConsumer.hentInstitusjoner(anyString(), anyString())).thenReturn(Lists.newArrayList("NO:NAVT003"));

        Map<String, String> rinaInfo = Maps.newHashMap();
        rinaInfo.put("caseId","3333");
        rinaInfo.put("documentId", "aaa");
        when(euxConsumer.opprettBucOgSed(eq("LA_BUC_04"), anyString(), any(SED.class))).thenReturn(rinaInfo);
    }

    @Test
    public void opprettOgSendSed_verifiserKorrektSedType() throws Exception {
        sedService.opprettOgSendSed(behandling, behandlingsresultat);
        verify(euxConsumer, times(1)).sendSed(eq("3333"), any(), eq("aaa"));
    }

    @Test(expected = RuntimeException.class)
    public void opprettOgSendSed_ikkeStøttetBestemmelse_forventException() throws Exception {
        behandlingsresultat.getLovvalgsperioder().iterator().next().setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3E);
        sedService.opprettOgSendSed(behandling, behandlingsresultat);
    }

}