package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
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

import static no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie.*;
import static no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie.tilMedfolgendeFamilie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrygdeavtaleTjenesteTest {
    private final static String ORGNR_1 = "11111111111";
    private final static String UUID_BARN_1 = UUID.randomUUID().toString();
    private final static String UUID_BARN_2 = UUID.randomUUID().toString();
    private final static String UUID_EKTEFELLE = UUID.randomUUID().toString();
    private final static String BEGRUNNELSE_BARN = "begrunnelse barn";
    private final static String BEGRUNNELSE_SAMBOER = "begrunnelse samboer";
    private static final String EKTEFELLE_FNR = "01108049800";
    private static final String BARN1_FNR = "01100099728";
    private static final String BARN2_FNR = "02109049878";
    private static final String BARN_NAVN_1 = "Doffen Duck";
    private static final String BARN_NAVN_2 = "Dole Duck";
    private static final String EKTEFELLE_NAVN = "Dolly Duck";

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
    void overførResultat_medTrygdeavtaleResultatDto_mappesKorrekt() {
        var trygdeavtaleResultatDto = lagTrygdeavtaleResultatDto();
        trygdeavtaleTjeneste.overførResultat(1L, trygdeavtaleResultatDto);

        verify(trygdeavtaleService).overførResultat(eq(1L), trygdeavtaleResultatArgumentCaptor.capture());
        var trygdeavtaleResultat = trygdeavtaleResultatArgumentCaptor.getValue();

        assertThat(trygdeavtaleResultat)
            .isNotNull()
            .extracting(
                TrygdeavtaleResultat::virksomhet,
                TrygdeavtaleResultat::bestemmelse,
                TrygdeavtaleResultat::lovvalgsperiodeFom,
                TrygdeavtaleResultat::lovvalgsperiodeTom
            )
            .containsExactlyInAnyOrder(
                trygdeavtaleResultatDto.virksomhet(),
                trygdeavtaleResultatDto.bestemmelse(),
                trygdeavtaleResultatDto.lovvalgsperiodeFom(),
                trygdeavtaleResultatDto.lovvalgsperiodeTom()
            );
        assertThat(trygdeavtaleResultat.familie().getFamilieIkkeOmfattetAvNorskTrygd())
            .hasSize(2)
            .flatExtracting(
                IkkeOmfattetFamilie::getUuid,
                IkkeOmfattetFamilie::getBegrunnelse,
                IkkeOmfattetFamilie::getBegrunnelseFritekst)
            .containsExactlyInAnyOrder(
                UUID_BARN_1, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(), BEGRUNNELSE_BARN,
                UUID_EKTEFELLE, Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(), BEGRUNNELSE_SAMBOER
            );
        assertThat(trygdeavtaleResultat.familie().getFamilieOmfattetAvNorskTrygd())
            .hasSize(1)
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

    @Test
    void hentResultat_byggOppResultat_returnererKorrekt() {
        Behandling behandling = lagBehandling();
        behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().personOpplysninger.medfolgendeFamilie =
            List.of(
                tilMedfolgendeFamilie(UUID_EKTEFELLE, EKTEFELLE_FNR, EKTEFELLE_NAVN, Relasjonsrolle.EKTEFELLE_SAMBOER),
                tilMedfolgendeFamilie(UUID_BARN_1, BARN1_FNR, BARN_NAVN_1, MedfolgendeFamilie.Relasjonsrolle.BARN),
                tilMedfolgendeFamilie(UUID_BARN_2, BARN2_FNR, BARN_NAVN_2, MedfolgendeFamilie.Relasjonsrolle.BARN)
            );

        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);
        when(trygdeavtaleService.hentResultat(1L)).thenReturn(lagTrygdeavtaleResultat());

        var response = trygdeavtaleTjeneste.hentResultat(1L).getBody();

        assertThat(response).isEqualTo(lagTrygdeavtaleResultatDto());
    }

    private static Behandlingsgrunnlag lagBehandlingsgrunnlag() {
        var behandlingsgrunnlagdata = new BehandlingsgrunnlagData();
        behandlingsgrunnlagdata.soeknadsland.landkoder.add(Landkoder.GB.getKode());
        behandlingsgrunnlagdata.periode = new Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
        var behandlingsgrunnlag = new Behandlingsgrunnlag();
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

    private TrygdeavtaleResultatDto lagTrygdeavtaleResultatDto() {
        return new TrygdeavtaleResultatDto.Builder()
            .virksomhet(ORGNR_1)
            .bestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1.getKode())
            .lovvalgsperiodeFom(LocalDate.now())
            .lovvalgsperiodeTom(LocalDate.now().plusYears(1))
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

    TrygdeavtaleResultat lagTrygdeavtaleResultat() {
        return new TrygdeavtaleResultat
            .Builder()
            .virksomhet(ORGNR_1)
            .lovvalgsperiodeFom(LocalDate.now())
            .lovvalgsperiodeTom(LocalDate.now().plusYears(1))
            .bestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1.getKode())
            .familie(lagAvklartMedfølgendeBarn()).build();
    }

    private AvklarteMedfolgendeFamilie lagAvklartMedfølgendeBarn() {
        var ektefelle = new IkkeOmfattetFamilie(UUID_EKTEFELLE, Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(), BEGRUNNELSE_SAMBOER);
        var barn1 = new IkkeOmfattetFamilie(UUID_BARN_1, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(), BEGRUNNELSE_BARN);
        barn1.setIdent(BARN1_FNR);
        var barn2 = new OmfattetFamilie(UUID_BARN_2);
        barn2.setIdent(BARN2_FNR);
        return new AvklarteMedfolgendeFamilie(
            Set.of(barn2), Set.of(ektefelle, barn1)
        );
    }
}
