package no.nav.melosys.service.dokument.brev;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerA001;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerA001Test {
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private UtenlandskMyndighetRepository myndighetsRepo;
    @Mock
    private VilkaarsresultatRepository vilkårRepo;

    @Mock
    private EregFasade ereg;

    @Mock
    private Behandling behandling;

    private Set<String> avklarteOrganisasjoner;

    private SoeknadDokument søknad;
    private MedlemskapDokument medlDokument;
    private ArbeidsforholdDokument arbDokument;

    private BrevDataByggerA001 brevDataByggerA001;

    private String orgnr1 = "12345678910";
    private String orgnr2 = "10987654321";

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {


        avklarteOrganisasjoner = new HashSet<>();
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(avklarteOrganisasjoner);

        Landkoder unntakFraLovvalgsland = Landkoder.SE;
        Anmodningsperiode periode = new Anmodningsperiode();
        periode.setUnntakFraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        periode.setUnntakFraLovvalgsland(unntakFraLovvalgsland);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Arrays.asList(periode));

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        when(myndighetsRepo.findByLandkode(any())).thenReturn(Optional.of(utenlandskMyndighet));

        VilkaarBegrunnelse begrunnelse = new VilkaarBegrunnelse();
        begrunnelse.setKode("God grunn");
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setVilkaar(Vilkaar.FO_883_2004_ART16_1);
        vilkaarsresultat.setBegrunnelser(new HashSet<>(Arrays.asList(begrunnelse)));
        when(vilkårRepo.findByBehandlingsresultatIdAndVilkaar(anyLong(), any())).thenReturn(Optional.of(vilkaarsresultat));

        StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
        oppgittAdresse.gatenavn = "HjemmeGata";
        oppgittAdresse.husnummer = "23B";
        oppgittAdresse.postnummer = "0165";
        oppgittAdresse.poststed = "Oslo";
        oppgittAdresse.landkode = Landkoder.NO.getKode();

        søknad = new SoeknadDokument();
        søknad.bosted.oppgittAdresse = oppgittAdresse;

        Saksopplysning soeknad = new Saksopplysning();
        soeknad.setDokument(søknad);
        soeknad.setType(SaksopplysningType.SØKNAD);

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = orgnr1;
        søknad.foretakUtland.add(foretakUtland);

        medlDokument = new MedlemskapDokument();
        Saksopplysning medl = new Saksopplysning();
        medl.setDokument(medlDokument);
        medl.setType(SaksopplysningType.MEDL);

        arbDokument = new ArbeidsforholdDokument();
        lagArbeidsforhold(orgnr2,
                LocalDate.of(2005, 1, 11),
                LocalDate.of(2017, 8, 11));

        Saksopplysning aareg = new Saksopplysning();
        aareg.setDokument(arbDokument);
        aareg.setType(SaksopplysningType.ARBFORH);

        Saksopplysning person = new Saksopplysning();
        PersonDokument personDok = new PersonDokument();
        person.setDokument(personDok);
        person.setType(SaksopplysningType.PERSOPL);
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(soeknad, person, medl, aareg)));

        OrganisasjonsDetaljer detaljer = mock(OrganisasjonsDetaljer.class);
        when(detaljer.hentStrukturertForretningsadresse()).thenReturn(lagStrukturertAdresse());

        leggTilTestorganisasjon("navn1", orgnr1, detaljer);
        leggTilTestorganisasjon("navn2", orgnr2, detaljer);

        brevDataByggerA001 = new BrevDataByggerA001(lovvalgsperiodeService, anmodningsperiodeService, myndighetsRepo, vilkårRepo);
    }

    private BrevDataGrunnlag lagDokumentressurs() throws TekniskException {
        RegisterOppslagSystemService registerOppslagService = new RegisterOppslagSystemService(ereg, mock(TpsFasade.class));
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        KodeverkService kodeverkService = mock(KodeverkService.class);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");
        return new BrevDataGrunnlag(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }

    private void leggTilTestorganisasjon(String navn, String orgnummer, OrganisasjonsDetaljer detaljer) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        OrganisasjonDokument orgDok = new OrganisasjonDokument();
        orgDok.setNavn(Arrays.asList(navn));
        orgDok.setOrgnummer(orgnummer);
        orgDok.setOrganisasjonDetaljer(detaljer);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ORG);
        saksopplysning.setDokument(orgDok);
        when(ereg.hentOrganisasjon(eq(orgnummer))).thenReturn(saksopplysning);
    }

    private Arbeidsforhold lagArbeidsforhold(String orgnr, LocalDate fom, LocalDate tom) {
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = orgnr;
        arbeidsforhold.ansettelsesPeriode = new Periode(fom, tom);
        arbDokument.arbeidsforhold.add(arbeidsforhold);

        return arbeidsforhold;
    }

    @Test
    public void testHentAvklarteSelvstendigeForetak() throws MelosysException {
        avklarteOrganisasjoner.add("12345678910");

        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = "12345678910";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        SelvstendigForetak foretak2 = new SelvstendigForetak();
        foretak2.orgnr = "10987654321";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak2);

        BrevDataA001 brevDataDto = (BrevDataA001) brevDataByggerA001.lag(lagDokumentressurs(),"Z12345");
        assertThat(brevDataDto.selvstendigeVirksomheter.stream()
                .map(nv -> nv.orgnr)).containsOnly(foretak.orgnr);
    }

    @Test
    public void testHentAvklarteNorskeForetak() throws MelosysException {
        avklarteOrganisasjoner.add(orgnr1);

        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add(orgnr1);

        BrevDataA001 brevDataDto = (BrevDataA001) brevDataByggerA001.lag(lagDokumentressurs(),"Z12345");
        assertThat(brevDataDto.selvstendigeVirksomheter.stream()
                .map(nv -> nv.orgnr)).containsOnly(orgnr1);
        assertThat(brevDataDto.arbeidsgivendeVirkomsheter.stream()
                .map(nv -> nv.orgnr)).containsOnly(orgnr1);
    }

    @Test
    public void testIngenAvklarteforetak() throws MelosysException {
        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        BrevDataA001 brevDataDto = (BrevDataA001) brevDataByggerA001.lag(lagDokumentressurs(), "Z12345");
        assertThat(brevDataDto.ansettelsesperiode).isEmpty();
    }

    @Test
    public void testAnsettelsesperiode() throws MelosysException {
        avklarteOrganisasjoner.add(orgnr1);

        lagArbeidsforhold(orgnr1,
                          LocalDate.of(1976, 10, 23),
                          LocalDate.of(1978, 10, 23));

        Arbeidsforhold forventet = lagArbeidsforhold(orgnr1,
                          LocalDate.of(2005, 1, 11),
                          LocalDate.of(2018, 8, 11));

        // Senere arbeidsforhold, men ikke et valgt arbeidsforhold
        lagArbeidsforhold(orgnr2,
                LocalDate.of(2010, 10, 23),
                LocalDate.of(2017, 10, 23));

        BrevData brevDataA001 = brevDataByggerA001.lag(lagDokumentressurs(), "Z12345");
        assertThat(((BrevDataA001)brevDataA001).ansettelsesperiode.get()).isEqualTo(forventet.ansettelsesPeriode);
    }

    @Test
    public void testIngenAnsettelsePeriode() throws MelosysException {
        avklarteOrganisasjoner.add(orgnr1);

        BrevData brevDataA001 = brevDataByggerA001.lag(lagDokumentressurs(), "Z12345");
        assertThat(((BrevDataA001)brevDataA001).ansettelsesperiode.isPresent()).isFalse();
    }
}