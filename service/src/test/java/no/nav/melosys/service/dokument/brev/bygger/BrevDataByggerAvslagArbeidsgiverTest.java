package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_vesentlig_virksomhet;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_1;
import static no.nav.melosys.service.SaksopplysningStubs.lagSøknadOgArbeidsforholdOpplysninger;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerAvslagArbeidsgiverTest {
    @Mock
    AvklartefaktaService avklartefaktaService;

    @Mock
    LandvelgerService landvelgerService;

    @Mock
    RegisterOppslagService registerOppslagService;

    @Mock
    KodeverkService kodeverkService;

    @Mock
    VilkaarsresultatRepository vilkaarsresultatRepository;

    @Mock
    LovvalgsperiodeService lovvalgsperiodeService;

    private BrevDataByggerAvslagArbeidsgiver brevDataByggerAvslagArbeidsgiver;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        when(landvelgerService.hentArbeidsland(anyLong())).thenReturn(Landkoder.AT);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");

        brevDataByggerAvslagArbeidsgiver = new BrevDataByggerAvslagArbeidsgiver(landvelgerService,
                                                                                lovvalgsperiodeService,
                                                                                vilkaarsresultatRepository);
    }

    @Test
    public void lag_avslagArbeidsgiverBrev_harVilkaarBegrunnelser() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(lagSoeknadssaksopplysning(new SoeknadDokument()));
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(new PersonDokument()));

        PersonDokument personDokument = new PersonDokument();
        personDokument.sammensattNavn = "Navn Navnesen";
        Saksopplysning person = new Saksopplysning();
        person.setDokument(personDokument);
        person.setType(SaksopplysningType.PERSOPL);

        Set<Saksopplysning> saksopplysninger = lagSøknadOgArbeidsforholdOpplysninger(Arrays.asList("987654321"),
            Collections.emptyList(),
            Arrays.asList("123456789"));

        saksopplysninger.add(person);
        behandling.setSaksopplysninger(saksopplysninger);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.DE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        when(lovvalgsperiodeService.hentLovvalgsperiode(behandling.getId())).thenReturn(lovvalgsperiode);

        Set<String> orgSet = new HashSet<>(Collections.singletonList("987654321"));
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.getId())).thenReturn(orgSet);

        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("987654321");
        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.hentStrukturertForretningsadresse()).thenReturn(lagStrukturertAdresse());
        organisasjonDokument.organisasjonDetaljer = organisasjonsDetaljer;

        when(registerOppslagService.hentOrganisasjoner(orgSet)).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));

        Vilkaarsresultat vilkaarsresultatArt121 = lagVilkårresultat(Vilkaar.FO_883_2004_ART12_1, Art12_1_begrunnelser.IKKE_OMFATTET_LENGE_NOK_I_NORGE_FOER.getKode());
        Vilkaarsresultat vesentligVirksomhet = lagVilkårresultat(Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET, Art12_1_vesentlig_virksomhet.FOR_LITE_KONTRAKTER_NORGE.getKode());

        when(vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(anyLong(), eq(FO_883_2004_ART12_1))).thenReturn(Optional.of(vilkaarsresultatArt121));
        when(vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(anyLong(), eq(ART12_1_VESENTLIG_VIRKSOMHET))).thenReturn(Optional.of(vesentligVirksomhet));

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        DokumentdataGrunnlag dataGrunnlag = new DokumentdataGrunnlag(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
        String saksbehandler = "saksbehandler";
        BrevDataAvslagArbeidsgiver brevData = (BrevDataAvslagArbeidsgiver) brevDataByggerAvslagArbeidsgiver.lag(dataGrunnlag, saksbehandler);
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