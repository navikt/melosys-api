package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklarteVirksomheterGrunnlagTest {

    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;

    private AvklarteVirksomheterGrunnlag dataGrunnlag;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet arbeidsgiver = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Arrays.asList(arbeidsgiver));

        dataGrunnlag = new AvklarteVirksomheterGrunnlag(mock(Behandling.class), avklarteVirksomheterService, mock(KodeverkService.class));
    }

    private Behandling lagBehandling(SoeknadDokument søknad, PersonDokument person) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(lagSoeknadssaksopplysning(søknad));
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(person));
        return behandling;
    }

    @Test
    public void hentAlleNorskeVirksomheter_foreventerEnVirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Collection<AvklartVirksomhet> norskeVirksomheter = dataGrunnlag.hentAlleNorskeVirksomheterMedAdresse();
        assertThat(norskeVirksomheter).hasSize(1);
        dataGrunnlag.hentAlleNorskeVirksomheterMedAdresse();
        verify(avklarteVirksomheterService, times(1)).hentAlleNorskeVirksomheter(any(), any());
    }

    @Test
    public void hentUtenlandskeArbeidsgivere_medUtenlandskArbeidsgiverOgSelvstendig_henterKunArbeidsgivere() throws TekniskException {
        AvklartVirksomhet utenlandskSelvstendigForetak = new AvklartVirksomhet(lagForetakUtland(true));
        AvklartVirksomhet utenlandskArbeidsgiver = new AvklartVirksomhet(lagForetakUtland(false));

        List<AvklartVirksomhet> utenlandskeVirksomheter = Arrays.asList(utenlandskSelvstendigForetak, utenlandskArbeidsgiver);
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(utenlandskeVirksomheter);

        List<AvklartVirksomhet> utenlandskeArbeidsgivere = dataGrunnlag.hentUtenlandskeArbeidsgivere();
        assertThat(utenlandskeArbeidsgivere).containsExactly(utenlandskArbeidsgiver);
    }

    @Test
    public void hentUtenlandskeSelvstendige_medUtenlandskArbeidsgiverOgSelvstendig_henterKunSelvstendige() throws TekniskException {
        AvklartVirksomhet utenlandskSelvstendigForetak = new AvklartVirksomhet(lagForetakUtland(true));
        AvklartVirksomhet utenlandskArbeidsgiver = new AvklartVirksomhet(lagForetakUtland(false));

        List<AvklartVirksomhet> utenlandskeVirksomheter = Arrays.asList(utenlandskSelvstendigForetak, utenlandskArbeidsgiver);
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(utenlandskeVirksomheter);

        List<AvklartVirksomhet> utenlandskeSelvstendige = dataGrunnlag.hentUtenlandskeSelvstendige();
        assertThat(utenlandskeSelvstendige).containsExactly(utenlandskSelvstendigForetak);
    }

    @Test
    public void hentHovedvirksomhet_medEnNorskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet avklartVirksomhet = dataGrunnlag.hentHovedvirksomhet();
        assertThat(avklartVirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet utenlandskAvklartVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(utenlandskAvklartVirksomhet));

        AvklartVirksomhet hovedvirksomhet = dataGrunnlag.hentHovedvirksomhet();
        dataGrunnlag.hentBivirksomheter();
        assertThat(hovedvirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medKunUtenlandskVirksomhet_girUtenlandskVirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.emptyList());

        AvklartVirksomhet hovedvirksomhet = dataGrunnlag.hentHovedvirksomhet();
        assertThat(hovedvirksomhet).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }

    @Test
    public void hentBivirksomheter_medEnNorskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    public void hentBivirksomheter_medEnUtenlandskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.emptyList());

        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    public void hentBivirksomheter_medToNorskeVirksomheter_girEnNorskBivirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Arrays.asList(norskVirksomhet, norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).containsExactly(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girUtenlandskBivirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland(false));

        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.hentBivirksomheter();
        assertThat(bivirksomheter).hasSize(1);

        assertThat(bivirksomheter.iterator().next()).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }
}
