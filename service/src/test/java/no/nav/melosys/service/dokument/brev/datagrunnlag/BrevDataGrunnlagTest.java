package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAvklartMaritimtArbeid;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagMaritimtArbeid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevDataGrunnlagTest {
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private AvklartefaktaService avklartefaktaService;

    private DoksysBrevbestilling brevbestilling;
    private Soeknad søknad;
    private BrevDataGrunnlag dataGrunnlag;

    @BeforeEach
    void setUp() {
        søknad = new Soeknad();
        Behandling behandling = lagBehandling(søknad);

        brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        Persondata persondata = PersonopplysningerObjectFactory.lagPersonopplysninger();
        dataGrunnlag = new BrevDataGrunnlag(brevbestilling, kodeverkService, mock(AvklarteVirksomheterService.class), avklartefaktaService, persondata);
    }

    private Behandling lagBehandling(Soeknad søknad) {
        Fagsak fagsak = FagsakTestFactory.builder().medBruker().build();

        Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(søknad);
        behandling.setMottatteOpplysninger(mottatteOpplysninger);
        return behandling;
    }

    @Test
    void hentArbeidssteder_medMaritimtArbeid_girMaritimeArbeidssteder() {
        AvklartMaritimtArbeid maritimtArbeid = lagAvklartMaritimtArbeid();
        when(avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(anyLong()))
            .thenReturn(Collections.singletonMap("Dunfjæder", maritimtArbeid));
        Persondata persondata = PersonopplysningerObjectFactory.lagPersonopplysninger();
        dataGrunnlag = new BrevDataGrunnlag(brevbestilling, kodeverkService, mock(AvklarteVirksomheterService.class), avklartefaktaService, persondata);

        MaritimtArbeid maritimtArbeidISøknad = lagMaritimtArbeid();
        this.søknad.maritimtArbeid.add(maritimtArbeidISøknad);

        List<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidsstedGrunnlag().hentArbeidssteder();
        assertThat(arbeidssteder).hasSize(1);

        MaritimtArbeidssted arbeidssted = (MaritimtArbeidssted) arbeidssteder.get(0);
        assertThat(arbeidssted.getEnhetNavn()).isEqualTo(maritimtArbeidISøknad.getEnhetNavn());
        assertThat(arbeidssted.getForetakNavn()).isNullOrEmpty();
        assertThat(arbeidssted.getIdnummer()).isNullOrEmpty();
        assertThat(arbeidssted.getYrkesgruppe().getKode()).isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP.getKode());
    }

    @Test
    void hentArbeidssteder_medMaritimtArbeidUtenAvklartMaritimtArbeid_girTomListe() {
        MaritimtArbeid maritimtArbeidISøknad = lagMaritimtArbeid();
        this.søknad.maritimtArbeid.add(maritimtArbeidISøknad);

        when(avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(anyLong())).thenReturn(Collections.emptyMap());
        Persondata persondata = PersonopplysningerObjectFactory.lagPersonopplysninger();
        final BrevDataGrunnlag dataGrunnlagUtenAvklartMaritimtArbeid = new BrevDataGrunnlag(
            brevbestilling,
            kodeverkService,
            mock(AvklarteVirksomheterService.class),
            avklartefaktaService,
            persondata
        );

        Collection<Arbeidssted> arbeidssteder = dataGrunnlagUtenAvklartMaritimtArbeid.getArbeidsstedGrunnlag().hentArbeidssteder();
        assertThat(arbeidssteder).isEmpty();
    }
}
