package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagForetakUtland;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagNorskVirksomhet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvklarteVirksomheterGrunnlagTest {

    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;

    @Mock
    private KodeverkService kodeverkService;

    private AvklarteVirksomheterGrunnlag dataGrunnlag;

    @BeforeEach
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        dataGrunnlag = new AvklarteVirksomheterGrunnlag(mock(Behandling.class), avklarteVirksomheterService);
    }

    @Test
    void hentAlleNorskeVirksomheter_foreventerEnVirksomhet() throws IkkeFunnetException, TekniskException {
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(Collections.singletonList(lagNorskVirksomhet()));
        Collection<AvklartVirksomhet> norskeVirksomheter = dataGrunnlag.hentAlleNorskeVirksomheterMedAdresse();
        assertThat(norskeVirksomheter).hasSize(1);
        dataGrunnlag.hentAlleNorskeVirksomheterMedAdresse();
        verify(avklarteVirksomheterService, times(1)).hentAlleNorskeVirksomheter(any());
    }

    @Test
    void hentUtenlandskeArbeidsgivere_medUtenlandskArbeidsgiverOgSelvstendig_henterKunArbeidsgivere() {
        AvklartVirksomhet utenlandskSelvstendigForetak = new AvklartVirksomhet(lagForetakUtland(true));
        AvklartVirksomhet utenlandskArbeidsgiver = new AvklartVirksomhet(lagForetakUtland(false));

        List<AvklartVirksomhet> utenlandskeVirksomheter = Arrays.asList(utenlandskSelvstendigForetak, utenlandskArbeidsgiver);
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(utenlandskeVirksomheter);

        List<AvklartVirksomhet> utenlandskeArbeidsgivere = dataGrunnlag.hentUtenlandskeArbeidsgivere();
        assertThat(utenlandskeArbeidsgivere).containsExactly(utenlandskArbeidsgiver);
    }

    @Test
    void hentUtenlandskeSelvstendige_medUtenlandskArbeidsgiverOgSelvstendig_henterKunSelvstendige() {
        AvklartVirksomhet utenlandskSelvstendigForetak = new AvklartVirksomhet(lagForetakUtland(true));
        AvklartVirksomhet utenlandskArbeidsgiver = new AvklartVirksomhet(lagForetakUtland(false));

        List<AvklartVirksomhet> utenlandskeVirksomheter = Arrays.asList(utenlandskSelvstendigForetak, utenlandskArbeidsgiver);
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(utenlandskeVirksomheter);

        List<AvklartVirksomhet> utenlandskeSelvstendige = dataGrunnlag.hentUtenlandskeSelvstendige();
        assertThat(utenlandskeSelvstendige).containsExactly(utenlandskSelvstendigForetak);
    }

    @Test
    void hentHovedvirksomhet_medEnNorskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet avklartVirksomhet = dataGrunnlag.hentHovedvirksomhet();
        assertThat(avklartVirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet utenlandskAvklartVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(utenlandskAvklartVirksomhet));

        AvklartVirksomhet hovedvirksomhet = dataGrunnlag.hentHovedvirksomhet();
        dataGrunnlag.hentBivirksomheter();
        assertThat(hovedvirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    void hentHovedvirksomhet_medKunUtenlandskVirksomhet_girUtenlandskVirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(Collections.emptyList());

        AvklartVirksomhet hovedvirksomhet = dataGrunnlag.hentHovedvirksomhet();
        assertThat(hovedvirksomhet).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }

    @Test
    void hentBivirksomheter_medEnNorskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(Collections.singletonList(norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    void hentBivirksomheter_medEnUtenlandskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, TekniskException {
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(Collections.emptyList());

        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    void hentBivirksomheter_medToNorskeVirksomheter_girEnNorskBivirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(Arrays.asList(norskVirksomhet, norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).containsExactly(norskVirksomhet);
    }

    @Test
    void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girUtenlandskBivirksomhet() throws IkkeFunnetException, TekniskException {
        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));

        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(Collections.singletonList(norskVirksomhet));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).hasSize(1);

        assertThat(bivirksomheter.iterator().next()).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }
}
