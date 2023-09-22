package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
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
import static org.mockito.Mockito.*;

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
        verify(medlemAvFolketrygdenRepository, times(1)).save(any());
    }

    @Test
    void utledMedlemskapsperioderFraSøknad_dataFraSøknadSatt_medlemskapsperioderEksisterer() {
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        behandlingsresultat.getVilkaarsresultater().add(lagOppfyltVilkår(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID));
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        Medlemskapsperiode medlemskapsperiode = new Medlemskapsperiode();
        medlemAvFolketrygden.addMedlemskapsperiode(medlemskapsperiode);
        behandlingsresultat.setMedlemAvFolketrygden(medlemAvFolketrygden);

        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)).thenReturn(behandlingsresultat);
        when(medlemAvFolketrygdenRepository.save(medlemAvFolketrygden)).thenReturn(medlemAvFolketrygden);

        assertThat(
            opprettMedlemskapsperiodeService.utledMedlemskapsperioderFraSøknad(behandlingsresultatID, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A)
        ).isNotEmpty();
        verify(medlemAvFolketrygdenRepository, times(1)).save(medlemAvFolketrygden);
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
        SøknadNorgeEllerUtenforEØS søknad = new SøknadNorgeEllerUtenforEØS();

        søknad.periode = new Periode(LocalDate.now(), null);
        søknad.soeknadsland.landkoder.add("BR");
        søknad.setTrygdedekning(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER);
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
