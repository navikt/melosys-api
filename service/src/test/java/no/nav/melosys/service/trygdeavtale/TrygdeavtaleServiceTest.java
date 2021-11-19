package no.nav.melosys.service.trygdeavtale;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.*;
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie.tilMedfolgendeFamilie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrygdeavtaleServiceTest {
    private final static String ORGNR_1 = "11111111111";
    private final static String ORGNR_2 = "22222222222";
    private final static String NAVN_1 = "Navn 1";
    private final static String NAVN_2 = "Navn 2";
    private final static String UUID_BARN = "0bad5c70-8a3f-4fc7-9031-d3aebd6b68de";
    private final static String UUID_EKTEFELLE = "1212121212121-4fc7-9031-ab34332121ff";
    private final static String BEGRUNNELSE_BARN = "begrunnelse barn";
    private final static String BEGRUNNELSE_SAMBOER = "begrunnelse samboer";

    @Mock
    private RegisterOppslagService registerOppslagService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private TrygdeavtaleService trygdeavtaleService;

    @BeforeEach
    void init() {
        trygdeavtaleService = new TrygdeavtaleService(registerOppslagService, behandlingsgrunnlagService, avklarteMedfolgendeFamilieService, avklarteVirksomheterService, lovvalgsperiodeService);
    }

    @Test
    void leggInnTrygdeAvtaleDataForOgKunneFatteVetak() {
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(1L)).thenReturn(lagBehandlingsgrunnlag());

        TrygdeavtaleResultat trygdeavtaleResultat = lagTrygdeavtaleResultat();

        trygdeavtaleService.overførResultat(1L, trygdeavtaleResultat);

        verify(behandlingsgrunnlagService, never()).oppdaterBehandlingsgrunnlag(any());
        verify(avklarteMedfolgendeFamilieService).lagreMedfolgendeFamilieSomAvklartefakta(eq(1L), any());
        verify(avklarteVirksomheterService).lagreVirksomheterSomAvklartefakta(1L, List.of(ORGNR_1));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(1L, expectedLovvalgsperioder());
    }

    @Test
    void overførResultat_medToLandkoder_kasterTekniskException() {
        Behandlingsgrunnlag behandlingsgrunnlag = lagBehandlingsgrunnlag();
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().soeknadsland.landkoder = List.of("GB", "NO");

        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(1L)).thenReturn(behandlingsgrunnlag);
        TrygdeavtaleResultat trygdeavtaleResultatDto = lagTrygdeavtaleResultat();

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> trygdeavtaleService.overførResultat(1L, trygdeavtaleResultatDto))
            .withMessageContaining("Forventet ett land i behandlingsgrunnlagdata soeknadsland.landkoder, men fant: [GB, NO]");
    }

    @Test
    void overførResultat_manglerLandkoder_kasterTekniskException() {
        Behandlingsgrunnlag behandlingsgrunnlag = lagBehandlingsgrunnlag();
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().soeknadsland.landkoder = List.of();

        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(1L)).thenReturn(behandlingsgrunnlag);
        TrygdeavtaleResultat trygdeavtaleResultatDto = lagTrygdeavtaleResultat();

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> trygdeavtaleService.overførResultat(1L, trygdeavtaleResultatDto))
            .withMessageContaining("Forventet ett land i behandlingsgrunnlagdata soeknadsland.landkoder, men fant: []");
    }

    private TrygdeavtaleResultat lagTrygdeavtaleResultat() {
        return new TrygdeavtaleResultat.Builder()
            .virksomheter(List.of(ORGNR_1))
            .bestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1.getKode())
            .familie(new AvklarteMedfolgendeFamilie(Set.of(), Set.of(
                new IkkeOmfattetFamilie(
                    UUID_BARN,
                    Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
                    BEGRUNNELSE_BARN),
                new IkkeOmfattetFamilie(
                    UUID_EKTEFELLE,
                    Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(),
                    BEGRUNNELSE_SAMBOER)
            )))
            .build();
    }

    private static Behandlingsgrunnlag lagBehandlingsgrunnlag() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        SoeknadTrygdeavtale behandlingsgrunnlagdata = new SoeknadTrygdeavtale();
        behandlingsgrunnlagdata.soeknadsland.landkoder.add("GB");
        behandlingsgrunnlagdata.periode = new Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagdata);
        return behandlingsgrunnlag;
    }

    private Collection<Lovvalgsperiode> expectedLovvalgsperioder() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.of(2020, 1, 1));
        lovvalgsperiode.setTom(LocalDate.of(2021, 1, 1));
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.GB);
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.PLIKTIG);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        return List.of(lovvalgsperiode);
    }

    @Test
    void hentVirksomheter_fraRegisterOppslag_mappesKorrekt() {
        var selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.orgnr = ORGNR_1;
        var selvstendigArbeid = new SelvstendigArbeid();
        selvstendigArbeid.selvstendigForetak = List.of(selvstendigForetak);

        var juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        juridiskArbeidsgiverNorge.ekstraArbeidsgivere = List.of(ORGNR_2);

        var behandling = lagBehandlingMedVirksomheter(
            selvstendigArbeid,
            juridiskArbeidsgiverNorge,
            emptyList(),
            emptySet()
        );

        when(registerOppslagService.hentOrganisasjon(ORGNR_1)).thenReturn(lagOrganisasjonsDokument(ORGNR_1, NAVN_1));
        when(registerOppslagService.hentOrganisasjon(ORGNR_2)).thenReturn(lagOrganisasjonsDokument(ORGNR_2, NAVN_2));

        var response = trygdeavtaleService.hentVirksomheter(behandling);

        assertThat(response)
            .hasSize(2)
            .containsEntry(ORGNR_1, NAVN_1)
            .containsEntry(ORGNR_2, NAVN_2);
    }

    @Test
    void hentVirksomheter_fraBehandlingSaksopplysning_mappesKorrekt() {
        var behandling = lagBehandlingMedVirksomheter(
            new SelvstendigArbeid(),
            new JuridiskArbeidsgiverNorge(),
            emptyList(),
            Set.of(
                lagArbForhSaksopplysning(List.of(ORGNR_1, ORGNR_2)),
                lagOrgSaksopplysning(ORGNR_1, NAVN_1),
                lagOrgSaksopplysning(ORGNR_2, NAVN_2))
        );

        var response = trygdeavtaleService.hentVirksomheter(behandling);

        assertThat(response)
            .hasSize(2)
            .containsEntry(ORGNR_1, NAVN_1)
            .containsEntry(ORGNR_2, NAVN_2);
    }

    @Test
    void hentVirksomheter_fraBehandlingsgrunnlagForetakUtland_mappesKorrekt() {
        var behandling = lagBehandlingMedVirksomheter(
            new SelvstendigArbeid(),
            new JuridiskArbeidsgiverNorge(),
            lagForetakUtland(Map.of(ORGNR_1, NAVN_1, ORGNR_2, NAVN_2)),
            emptySet()
        );

        var response = trygdeavtaleService.hentVirksomheter(behandling);

        assertThat(response)
            .hasSize(2)
            .containsEntry(ORGNR_1, NAVN_1)
            .containsEntry(ORGNR_2, NAVN_2);
    }

    @Test
    void hentVirksomheter_ingenVirksomheter_tomMap() {
        var behandling = lagBehandlingMedVirksomheter(
            new SelvstendigArbeid(),
            new JuridiskArbeidsgiverNorge(),
            emptyList(),
            emptySet()
        );

        var response = trygdeavtaleService.hentVirksomheter(behandling);

        assertThat(response).size().isEqualTo(0);
    }

    @Test
    void hentFamiliemedlemmer_barnOgEktefelle_fyltListe() {
        var behandling = lagBehandlingMedFamilie(List.of(
            tilMedfolgendeFamilie("uuid1", "01.01.01", "navn1", MedfolgendeFamilie.Relasjonsrolle.BARN),
            tilMedfolgendeFamilie("uuid2", "01.01.01", "navn2", MedfolgendeFamilie.Relasjonsrolle.BARN),
            tilMedfolgendeFamilie("uuid3", "01.01.01", "navn3", MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER)
        ));

        var response = trygdeavtaleService.hentFamiliemedlemmer(behandling);

        assertThat(response)
            .hasSize(3)
            .flatExtracting(
                MedfolgendeFamilie::getUuid,
                MedfolgendeFamilie::getNavn,
                MedfolgendeFamilie::getRelasjonsrolle)
            .containsExactlyInAnyOrder(
                "uuid1", "navn1", MedfolgendeFamilie.Relasjonsrolle.BARN,
                "uuid2", "navn2", MedfolgendeFamilie.Relasjonsrolle.BARN,
                "uuid3", "navn3", MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER
            );
    }

    @Test
    void hentFamiliemedlemmer_ingenFamilie_tomListe() {
        var behandling = lagBehandlingMedFamilie(emptyList());
        var response = trygdeavtaleService.hentFamiliemedlemmer(behandling);
        assertThat(response).isEmpty();
    }


    private Behandling lagBehandlingMedFamilie(List<MedfolgendeFamilie> familie) {
        var personOpplysninger = new OpplysningerOmBrukeren();
        personOpplysninger.medfolgendeFamilie.addAll(familie);

        var behandlingsgrunnlagdata = new BehandlingsgrunnlagData();
        behandlingsgrunnlagdata.personOpplysninger = personOpplysninger;

        var behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagdata);

        var behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandling;
    }

    private Behandling lagBehandlingMedVirksomheter(SelvstendigArbeid selvstendigArbeid,
                                                    JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge,
                                                    List<ForetakUtland> foretakUtland,
                                                    Set<Saksopplysning> saksopplysninger) {
        var behandlingsgrunnlagdata = new BehandlingsgrunnlagData();
        behandlingsgrunnlagdata.selvstendigArbeid = selvstendigArbeid;
        behandlingsgrunnlagdata.juridiskArbeidsgiverNorge = juridiskArbeidsgiverNorge;
        behandlingsgrunnlagdata.foretakUtland = foretakUtland;

        var behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagdata);

        var behandling = new Behandling();
        behandling.setSaksopplysninger(saksopplysninger);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandling;
    }

    private List<ForetakUtland> lagForetakUtland(Map<String, String> uuidNavn) {
        return uuidNavn.entrySet().stream()
            .map(un -> {
                var foretakUtland = new ForetakUtland();
                foretakUtland.uuid = un.getKey();
                foretakUtland.navn = un.getValue();
                return foretakUtland;
            })
            .toList();
    }

    private Saksopplysning lagOrgSaksopplysning(String orgnr, String navn) {
        var saksopplysning = new Saksopplysning();
        saksopplysning.setId(Long.parseLong(orgnr));
        saksopplysning.setType(SaksopplysningType.ORG);
        saksopplysning.setDokument(lagOrganisasjonsDokument(orgnr, navn));
        return saksopplysning;
    }

    private OrganisasjonDokument lagOrganisasjonsDokument(String orgnr, String navn) {
        var organisasjonsDokument = new OrganisasjonDokument();
        organisasjonsDokument.setOrgnummer(orgnr);
        organisasjonsDokument.setNavn(navn);
        return organisasjonsDokument;
    }

    private Saksopplysning lagArbForhSaksopplysning(List<String> orgnumre) {
        var arbeidsforholdDokument = new ArbeidsforholdDokument();
        arbeidsforholdDokument.arbeidsforhold = orgnumre.stream()
            .map(orgnr -> {
                var arbeidsforhold = new Arbeidsforhold();
                arbeidsforhold.arbeidsgivertype = Aktoertype.ORGANISASJON;
                arbeidsforhold.arbeidsgiverID = orgnr;
                return arbeidsforhold;
            })
            .toList();

        var saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysning.setDokument(arbeidsforholdDokument);
        return saksopplysning;
    }
}
