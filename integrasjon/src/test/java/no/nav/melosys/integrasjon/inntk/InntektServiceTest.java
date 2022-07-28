package no.nav.melosys.integrasjon.inntk;

import java.time.YearMonth;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektMock;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Uttrekksperiode;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InntektServiceTest {

    @Spy
    private InntektMock inntektMock = new InntektMock();

    private InntektService inntektService;

    @Captor
    private ArgumentCaptor<HentInntektListeBolkRequest> captor;

    @BeforeEach
    void setUp() {
        DokumentFactory dokumentFactory = new DokumentFactory(JaxbConfig.getJaxb2Marshaller(), new XsltTemplatesFactory());
        inntektService = new InntektService(inntektMock, dokumentFactory);
    }

    @Test
    void hentInntektListe_periodeEtterJan2015_henterInntekt() {
        Saksopplysning saksopplysning = inntektService.hentInntektListe("99999999992", YearMonth.of(2017, 6), YearMonth.of(2017, 8));
        InntektDokument dokument = (InntektDokument) saksopplysning.getDokument();
        assertThat(dokument).isNotNull();
    }

    @Test
    void hentInntektListe_fomFørJan2015_henterInntektMedFomJan2015() throws Exception {
        Saksopplysning saksopplysning = inntektService.hentInntektListe("99999999992", YearMonth.of(2014, 6), YearMonth.of(2017, 8));
        InntektDokument dokument = (InntektDokument) saksopplysning.getDokument();
        assertThat(dokument).isNotNull();

        verify(inntektMock).hentInntektListeBolk(captor.capture());
        Uttrekksperiode uttrekksperiodeReq = captor.getValue().getUttrekksperiode();
        assertThat(uttrekksperiodeReq.getMaanedFom().getYear()).isEqualTo(2015);
        assertThat(uttrekksperiodeReq.getMaanedFom().getMonth()).isEqualTo(1);

        assertThat(uttrekksperiodeReq.getMaanedTom().getYear()).isEqualTo(2017);
        assertThat(uttrekksperiodeReq.getMaanedTom().getMonth()).isEqualTo(8);
    }

    @Test
    void hentInntektListe_helePeriodeFørJan2015_returnererTomInntektListe() throws HentInntektListeBolkUgyldigInput, HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter {
        Saksopplysning saksopplysning = inntektService.hentInntektListe("99999999992", YearMonth.of(2012, 1), YearMonth.of(2014, 12));

        verify(inntektMock, never()).hentInntektListeBolk(any());
        assertThat(saksopplysning.getKilder()).isNotEmpty();
        assertThat(saksopplysning.getKilder().iterator().next().getMottattDokument()).isNotNull();
        assertThat(saksopplysning.getDokument())
            .isInstanceOf(InntektDokument.class)
            .extracting(s -> ((InntektDokument) s).getArbeidsInntektMaanedListe())
            .asList()
            .isEmpty();
    }
}
