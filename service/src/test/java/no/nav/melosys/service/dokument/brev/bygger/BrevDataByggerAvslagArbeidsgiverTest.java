package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Art12_1_Begrunnelser;
import no.nav.melosys.domain.kodeverk.Art12_1_Vesentlig_Virksomhet_Begrunnelser;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AvklarteVirksomheterTest;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerAvslagArbeidsgiverTest {
    @Mock
    AvklartefaktaService avklartefaktaService;

    @Mock
    RegisterOppslagService registerOppslagService;

    @Mock
    VilkaarsresultatRepository vilkaarsresultatRepository;

    @Mock
    LovvalgsperiodeService lovvalgsperiodeService;

    private BrevDataByggerAvslagArbeidsgiver brevDataByggerAvslagArbeidsgiver;

    @Before
    public void setUp() {
        brevDataByggerAvslagArbeidsgiver = new BrevDataByggerAvslagArbeidsgiver(avklartefaktaService,
                                                                                registerOppslagService,
                                                                                lovvalgsperiodeService,
                                                                                vilkaarsresultatRepository);
    }

    @Test
    public void lag_avslagArbeidsgiverBrev_harVilkaarBegrunnelser() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);

        PersonDokument personDokument = new PersonDokument();
        personDokument.sammensattNavn = "Navn Navnesen";
        Saksopplysning person = new Saksopplysning();
        person.setDokument(personDokument);
        person.setType(SaksopplysningType.PERSONOPPLYSNING);

        Set<Saksopplysning> saksopplysninger =
        AvklarteVirksomheterTest.lagSøknadOgArbeidsforholdOpplysninger(Arrays.asList("987654321"),
                                                                       Collections.emptyList(),
                                                                       Arrays.asList("123456789"));

        saksopplysninger.add(person);
        behandling.setSaksopplysninger(saksopplysninger);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.DE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        when(lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId())).thenReturn(Collections.singletonList(lovvalgsperiode));

        Set<String> orgSet = new HashSet<>(Collections.singletonList("987654321"));
        when(avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId())).thenReturn(orgSet);

        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("987654321");
        when(registerOppslagService.hentOrganisasjoner(orgSet)).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));

        Vilkaarsresultat vilkaarsresultatArt121 = lagVilkårresultat(Vilkaar.FO_883_2004_ART12_1, Art12_1_Begrunnelser.IKKE_OMFATTET_LENGE_NOK_I_NORGE_FOER.getKode());
        Vilkaarsresultat vesentligVirksomhet = lagVilkårresultat(Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET, Art12_1_Vesentlig_Virksomhet_Begrunnelser.FOR_LITE_KONTRAKTER_NORGE.getKode());

        when(vilkaarsresultatRepository.findByBehandlingsresultatId(anyLong())).thenReturn(Arrays.asList(vilkaarsresultatArt121, vesentligVirksomhet));

        String saksbehandler = "saksbehandler";
        BrevDataAvslagArbeidsgiver brevData = (BrevDataAvslagArbeidsgiver) brevDataByggerAvslagArbeidsgiver.lag(behandling, saksbehandler);
        assertThat(brevData.hovedvirksomhet.orgnr).isEqualTo("987654321");
    }

    private Vilkaarsresultat lagVilkårresultat(Vilkaar vilkaarType, String vilkårbegrunnelseKode) {
        VilkaarBegrunnelse begrunnelser = new VilkaarBegrunnelse();
        begrunnelser.setKode(vilkårbegrunnelseKode);
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setOppfylt(false);
        vilkaarsresultat.setVilkaar(vilkaarType);
        vilkaarsresultat.setBegrunnelser(Collections.singleton(begrunnelser));
        return vilkaarsresultat;
    }
}