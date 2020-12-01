package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.Periode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettMedlemskapsperiodeServiceTest {

    @Mock
    private MedlemskapsperiodeRepository medlemskapsperiodeRepository;
    @Mock
    private MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private OpprettMedlemskapsperiodeService opprettMedlemskapsperiodeService;

    private final long behandlingsresultatID = 123321;

    @BeforeEach
    public void setup() {
        opprettMedlemskapsperiodeService = new OpprettMedlemskapsperiodeService(medlemAvFolketrygdenRepository, behandlingsresultatService);
    }

    @Test
    void utledMedlemskapsperioderFraSøknad_dataFraSøknadSatt_lagrerMedlemskapsperioder() throws FunksjonellException {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.getVilkaarsresultater().add(lagOppfyltVilkår(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID));
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingsresultatID))).thenReturn(behandlingsresultat);
        when(medlemAvFolketrygdenRepository.save(any(MedlemAvFolketrygden.class))).thenAnswer(returnsFirstArg());
        assertThat(
            opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(behandlingsresultatID, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A)
        ).isNotEmpty();
    }

    @Test
    void utledMedlemskapsperioderFraSøknad_oppfyllerIkkeVilkår_kasterFeil() throws FunksjonellException {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingsresultatID))).thenReturn(behandlingsresultat);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(
                behandlingsresultatID, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A))
            .withMessageContaining("er påkrevd for bestemmelse");
    }

    @Test
    void utledMedlemskapsperioderFraSøknad_støtterIkkeBestemmelse_kasterFeil() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(
                behandlingsresultatID, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_D))
            .withMessageContaining("Støtter ikke");
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.FTRL);
        behandling.setFagsak(fagsak);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        SoeknadFtrl søknad = new SoeknadFtrl();

        søknad.periode = new Periode(LocalDate.now(), null);
        søknad.soeknadsland.landkoder.add("BR");
        søknad.setTrygdedekning(Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknad);
        behandlingsgrunnlag.setMottaksdato(LocalDate.now());
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        behandlingsresultat.setBehandling(behandling);

        return behandlingsresultat;
    }

    private Vilkaarsresultat lagOppfyltVilkår(Vilkaar vilkår) {
        var vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setVilkaar(vilkår);
        vilkaarsresultat.setOppfylt(true);
        return vilkaarsresultat;
    }

}