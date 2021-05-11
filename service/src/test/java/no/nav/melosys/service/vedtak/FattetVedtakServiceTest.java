package no.nav.melosys.service.vedtak;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avgift.Trygdeavgift;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.vedtak.dto.FattetVedtak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagBostedsadresse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FattetVedtakServiceTest {

    @Mock
    private FattetVedtakProducer mockFattetVedtakProducer;

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private BehandlingsresultatService mockBehandlingsresultatService;

    @Captor
    private ArgumentCaptor<FattetVedtak> fattetVedtakCaptor;

    private FattetVedtakService fattetVedtakService;

    @BeforeEach
    void setUp() {
        fattetVedtakService = new FattetVedtakService(mockFattetVedtakProducer, mockBehandlingService, mockBehandlingsresultatService);
    }

    @Test
    void fattetVedtakFtrl_skalPubliseres() {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockBehandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(lagBehandlingsresultat());
        fattetVedtakService.publiserFattetVedtak(123L);

        verify(mockFattetVedtakProducer).publiserMelding(fattetVedtakCaptor.capture());

        FattetVedtak fattetVedtak = fattetVedtakCaptor.getValue();
        assertThat(fattetVedtak).isNotNull();
        assertThat(fattetVedtak.sak()).isNotNull();
        assertThat(fattetVedtak.lovvalgOgMedlemskapsperioder()).isNotNull();
    }


    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setVedtakMetadata(lagVedtakMetadata());
        behandlingsresultat.setMedlemAvFolketrygden(lagMedlemAvFolketrygden());
        return behandlingsresultat;
    }

    private MedlemAvFolketrygden lagMedlemAvFolketrygden() {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(lagFastsattTrygdeavgift());
        medlemAvFolketrygden.setMedlemskapsperioder(lagMedlemskapsperioder());

        return medlemAvFolketrygden;
    }

    private Collection<Medlemskapsperiode> lagMedlemskapsperioder() {
        Medlemskapsperiode m = new Medlemskapsperiode();
        m.setBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8);
        m.setFom(LocalDate.now());
        m.setTom(LocalDate.now().plusYears(1));
        m.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        m.setTrygdedekning(Trygdedekninger.HELSE_OG_PENSJONSDEL);
        m.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        m.setTrygdeavgift(lagTrygdeavgift());

        return List.of(m);
    }

    private Collection<Trygdeavgift> lagTrygdeavgift() {
        Trygdeavgift norsk = new Trygdeavgift();
        norsk.setAvgiftForInntekt(Trygdeavgift.AvgiftForInntekt.NORSK_INNTEKT);
        norsk.setTrygdesats(BigDecimal.valueOf(2.3));
        norsk.setTrygdeavgiftsbeløpMd(BigDecimal.valueOf(1150));
        norsk.setAvgiftskode("M2E");
        Trygdeavgift utenlandsk = new Trygdeavgift();
        utenlandsk.setAvgiftForInntekt(Trygdeavgift.AvgiftForInntekt.UTENLANDSK_INNTEKT);
        utenlandsk.setTrygdesats(BigDecimal.valueOf(4.3));
        utenlandsk.setTrygdeavgiftsbeløpMd(BigDecimal.valueOf(430));
        utenlandsk.setAvgiftskode("M2D");

        return List.of(norsk, utenlandsk);
    }

    private FastsattTrygdeavgift lagFastsattTrygdeavgift() {
        FastsattTrygdeavgift trygdeavgift = new FastsattTrygdeavgift();
        Aktoer betalesAv = new Aktoer();
        betalesAv.setOrgnr("987654321");
        betalesAv.setRolle(Aktoersroller.ARBEIDSGIVER);

        trygdeavgift.setBetalesAv(betalesAv);
        trygdeavgift.setTrygdeavgiftstype(Trygdeavgift_typer.ENDELIG);
        trygdeavgift.setAvgiftspliktigNorskInntektMnd(50000L);
        trygdeavgift.setAvgiftspliktigUtenlandskInntektMnd(10000L);
        trygdeavgift.setRepresentantNr("000123");

        return trygdeavgift;
    }

    private VedtakMetadata lagVedtakMetadata() {
        VedtakMetadata vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setVedtaksdato(Instant.now());
        return vedtakMetadata;
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag());
        behandling.setFagsak(lagFagsak());
        behandling.getSaksopplysninger().add(lagPersonDokument());
        return behandling;
    }

    private Fagsak lagFagsak() {
        Aktoer rep = new Aktoer();
        rep.setRepresenterer(Representerer.BRUKER);
        rep.setOrgnr("987654321");
        rep.setRolle(Aktoersroller.REPRESENTANT);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        fagsak.setRegistrertDato(Instant.now());
        fagsak.setAktører(Set.of(rep));
        fagsak.setType(Sakstyper.FTRL);
        return fagsak;
    }

    private Behandlingsgrunnlag lagBehandlingsgrunnlag() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(lagSoeknadFtrlData());
        return behandlingsgrunnlag;
    }

    private BehandlingsgrunnlagData lagSoeknadFtrlData() {
        SoeknadFtrl soeknadFtrl = new SoeknadFtrl();
        return soeknadFtrl;
    }

    private Saksopplysning lagPersonDokument() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.statsborgerskap = new Land(Land.BELGIA);
        personDokument.fnr = "12345678901";
        personDokument.fornavn = "For";
        personDokument.mellomnavn = "Mellom";
        personDokument.etternavn = "Etter";
        personDokument.bostedsadresse = lagBostedsadresse();
        return lagSaksopplysning(SaksopplysningType.PERSOPL, personDokument);
    }

    private Saksopplysning lagSaksopplysning(SaksopplysningType type, SaksopplysningDokument dokument) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(type);
        saksopplysning.setDokument(dokument);
        return saksopplysning;
    }
}
