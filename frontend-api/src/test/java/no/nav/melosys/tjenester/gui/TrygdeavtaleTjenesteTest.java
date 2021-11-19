package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleResultat;
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleService;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleResultatDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrygdeavtaleTjenesteTest {
    private final static String ORGNR_1 = "11111111111";
    private final static String UUID_BARN_1 = "0bad5c70-8a3f-4fc7-9031-d3aebd6b68de";
    private final static String UUID_BARN_2 = "1f598113-03a6-431b-99f2-0c42f61348cd";
    private final static String UUID_EKTEFELLE = "1212121212121-4fc7-9031-ab34332121ff";
    private final static String BEGRUNNELSE_BARN = "begrunnelse barn";
    private final static String BEGRUNNELSE_SAMBOER = "begrunnelse samboer";

    @Mock
    private TrygdeavtaleService trygdeavtaleService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private Aksesskontroll aksesskontroll;

    @Captor
    private ArgumentCaptor<TrygdeavtaleResultat> trygdeavtaleResultatArgumentCaptor;

    private TrygdeavtaleTjeneste trygdeavtaleTjeneste;

    private static final Behandling behandling = lagBehandling();

    @BeforeEach
    void init() {
        trygdeavtaleTjeneste = new TrygdeavtaleTjeneste(trygdeavtaleService, behandlingService, aksesskontroll);
    }

    @Test
    void overførResultat_medTrygdeavtaleResultatDto_overførerKorrekt() {
        var trygdeavtaleResultatDto = lagTrygdeavtaleResultat();
        trygdeavtaleTjeneste.overførResultat(1L, trygdeavtaleResultatDto);

        verify(trygdeavtaleService).overførResultat(eq(1L), trygdeavtaleResultatArgumentCaptor.capture());
        var trygdeavtaleResultat = trygdeavtaleResultatArgumentCaptor.getValue();

        assertThat(trygdeavtaleResultat)
            .isNotNull()
            .extracting(
                TrygdeavtaleResultat::virksomheter,
                TrygdeavtaleResultat::bestemmelse)
            .containsExactlyInAnyOrder(
                trygdeavtaleResultatDto.virksomheter(),
                trygdeavtaleResultatDto.bestemmelse()
            );
        assertThat(trygdeavtaleResultat.familie().getFamilieIkkeOmfattetAvNorskTrygd())
            .isNotNull()
            .flatExtracting(
                IkkeOmfattetFamilie::getUuid,
                IkkeOmfattetFamilie::getBegrunnelse,
                IkkeOmfattetFamilie::getBegrunnelseFritekst)
            .containsExactlyInAnyOrder(
                UUID_BARN_1, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(), BEGRUNNELSE_BARN,
                UUID_EKTEFELLE, Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(), BEGRUNNELSE_SAMBOER
            );
        assertThat(trygdeavtaleResultat.familie().getFamilieOmfattetAvNorskTrygd())
            .isNotNull()
            .flatExtracting(OmfattetFamilie::getUuid)
            .containsExactly(UUID_BARN_2);
    }

    @Test
    void hentTrygdeavtaleInfo_utenVirksomhetOgBarnEktefelle_returnererKorrekt() {
        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);

        var response = trygdeavtaleTjeneste.hentTrygdeavtaleInfo(1L, false, false).getBody();

        verify(trygdeavtaleService, never()).hentVirksomheter(any());
        verify(trygdeavtaleService, never()).hentFamiliemedlemmer(any());

        assertThat(response).isNotNull();
        assertThat(response.aktoerId()).isEqualTo(behandling.getFagsak().hentAktørID());
        assertThat(response.behandlingstema()).isEqualTo(behandling.getTema().getKode());
        var behandlingsgrunnlagdata = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        assertThat(response.periodeFom()).isEqualTo(behandlingsgrunnlagdata.periode.getFom());
        assertThat(response.periodeTom()).isEqualTo(behandlingsgrunnlagdata.periode.getTom());
        assertThat(response.soeknadsland()).isEqualTo(behandlingsgrunnlagdata.soeknadsland.landkoder);
    }

    @Test
    void hentTrygdeavtaleInfo_medVirksomhetOgBarnEktefelle_returnererKorrekt() {
        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);

        var response = trygdeavtaleTjeneste.hentTrygdeavtaleInfo(1L, true, true).getBody();

        verify(trygdeavtaleService).hentVirksomheter(any());
        verify(trygdeavtaleService).hentFamiliemedlemmer(any());

        assertThat(response).isNotNull();
        assertThat(response.aktoerId()).isEqualTo(behandling.getFagsak().hentAktørID());
        assertThat(response.behandlingstema()).isEqualTo(behandling.getTema().getKode());
        var behandlingsgrunnlagdata = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        assertThat(response.periodeFom()).isEqualTo(behandlingsgrunnlagdata.periode.getFom());
        assertThat(response.periodeTom()).isEqualTo(behandlingsgrunnlagdata.periode.getTom());
        assertThat(response.soeknadsland()).isEqualTo(behandlingsgrunnlagdata.soeknadsland.landkoder);
    }

    private static Behandlingsgrunnlag lagBehandlingsgrunnlag() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        SoeknadTrygdeavtale behandlingsgrunnlagdata = new SoeknadTrygdeavtale();
        behandlingsgrunnlagdata.soeknadsland.landkoder.add("GB");
        behandlingsgrunnlagdata.periode = new Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagdata);
        return behandlingsgrunnlag;
    }

    private static Behandling lagBehandling() {
        var bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("AktørId");
        var fagsak = new Fagsak();
        fagsak.getAktører().add(bruker);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag());
        return behandling;
    }

    private TrygdeavtaleResultatDto lagTrygdeavtaleResultat() {
        return new TrygdeavtaleResultatDto.Builder()
            .virksomheter(List.of(ORGNR_1))
            .bestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1.getKode())
            .addBarn(
                UUID_BARN_1,
                false,
                Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
                BEGRUNNELSE_BARN)
            .addBarn(UUID_BARN_2, true, null, null)
            .ektefelle(
                UUID_EKTEFELLE,
                false,
                Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(),
                BEGRUNNELSE_SAMBOER)
            .build();
    }
}
