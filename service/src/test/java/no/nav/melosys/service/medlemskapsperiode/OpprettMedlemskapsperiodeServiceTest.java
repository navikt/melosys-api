package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.SoeknadFtrl;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettMedlemskapsperiodeServiceTest {

    @Mock
    private MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private UtledMottaksdato utledMottaksdato;

    private OpprettMedlemskapsperiodeService opprettMedlemskapsperiodeService;

    private final long behandlingsresultatID = 123321;

    @BeforeEach
    public void setup() {
        opprettMedlemskapsperiodeService = new OpprettMedlemskapsperiodeService(medlemAvFolketrygdenRepository, behandlingsresultatService, utledMottaksdato);
    }

    @Test
    void utledMedlemskapsperioderFraSøknad_dataFraSøknadSatt_lagrerMedlemskapsperioder() {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.getVilkaarsresultater().add(lagOppfyltVilkår(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID));
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);
        when(medlemAvFolketrygdenRepository.save(any(MedlemAvFolketrygden.class))).thenAnswer(returnsFirstArg());
        when(utledMottaksdato.getMottaksdato(any())).thenReturn(LocalDate.now());


        assertThat(
            opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(behandlingsresultatID, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A)
        ).isNotEmpty();
    }

    @Test
    void utledMedlemskapsperioderFraSøknad_oppfyllerIkkeVilkår_kasterFeil() {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(
                behandlingsresultatID, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A))
            .withMessageContaining("er påkrevd for bestemmelse");
    }

    @Test
    void utledMedlemskapsperioderFraSøknad_støtterIkkeBestemmelse_kasterFeil() {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);

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
        behandling.setTema(Behandlingstema.YRKESAKTIV);
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        SoeknadFtrl søknad = new SoeknadFtrl();

        søknad.periode = new Periode(LocalDate.now(), null);
        søknad.soeknadsland.landkoder.add("BR");
        søknad.setTrygdedekning(Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER);
        mottatteOpplysninger.setMottatteOpplysningerdata(søknad);
        behandling.setMottatteOpplysninger(mottatteOpplysninger);
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
