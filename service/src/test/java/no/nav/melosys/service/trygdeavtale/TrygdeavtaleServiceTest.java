package no.nav.melosys.service.trygdeavtale;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.*;
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie.Relasjonsrolle.BARN;
import static no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER;
import static no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie.tilMedfolgendeFamilie;
import static no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.INNVILGET;
import static no.nav.melosys.domain.kodeverk.Medlemskapstyper.PLIKTIG;
import static no.nav.melosys.domain.kodeverk.Trygdedekninger.FULL_DEKNING_FTRL;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrygdeavtaleServiceTest {
    private final static String ORGNR_1 = "11111111111";
    private final static String ORGNR_2 = "22222222222";
    private final static String NAVN_1 = "Navn 1";
    private final static String NAVN_2 = "Navn 2";
    private final static String UUID_BARN_1 = UUID.randomUUID().toString();
    private final static String UUID_BARN_2 = UUID.randomUUID().toString();
    private final static String UUID_EKTEFELLE = UUID.randomUUID().toString();
    private final static String BEGRUNNELSE_BARN = "begrunnelse barn";
    private final static String BEGRUNNELSE_SAMBOER = "begrunnelse samboer";
    private final static LocalDate PERIODE_FOM = LocalDate.now();
    private final static LocalDate PERIODE_TOM = PERIODE_FOM.plusYears(1);

    @Mock
    private EregFasade eregFasade;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    @Captor
    private ArgumentCaptor<AvklarteMedfolgendeFamilie> avklarteMedfolgendeFamilieArgumentCaptor;
    @Captor
    private ArgumentCaptor<Collection<Lovvalgsperiode>> lovvalgsperioderArgumentCaptor;

    private TrygdeavtaleService trygdeavtaleService;

    @BeforeEach
    void init() {
        trygdeavtaleService = new TrygdeavtaleService(eregFasade, behandlingsgrunnlagService, avklarteMedfolgendeFamilieService, avklarteVirksomheterService, lovvalgsperiodeService);
    }

    @Test
    void overførResultat_altOk_lagresKorrekt() {
        var trygdeavtaleResultat = lagTrygdeavtaleResultat();

        trygdeavtaleService.overførResultat(1L, trygdeavtaleResultat);

        verify(avklarteMedfolgendeFamilieService).lagreMedfolgendeFamilieSomAvklartefakta(eq(1L), avklarteMedfolgendeFamilieArgumentCaptor.capture());
        verify(avklarteVirksomheterService).lagreVirksomheterSomAvklartefakta(1L, List.of(ORGNR_1));
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(1L), lovvalgsperioderArgumentCaptor.capture());

        assertThat(avklarteMedfolgendeFamilieArgumentCaptor.getValue().getFamilieIkkeOmfattetAvNorskTrygd())
            .isNotNull()
            .flatExtracting(
                IkkeOmfattetFamilie::getUuid,
                IkkeOmfattetFamilie::getBegrunnelse,
                IkkeOmfattetFamilie::getBegrunnelseFritekst)
            .containsExactlyInAnyOrder(
                UUID_BARN_1, OVER_18_AR.getKode(), BEGRUNNELSE_BARN,
                UUID_EKTEFELLE, EGEN_INNTEKT.getKode(), BEGRUNNELSE_SAMBOER
            );
        assertThat(avklarteMedfolgendeFamilieArgumentCaptor.getValue().getFamilieOmfattetAvNorskTrygd())
            .hasSize(1)
            .flatExtracting(OmfattetFamilie::getUuid)
            .containsExactly(UUID_BARN_2);

        assertThat(lovvalgsperioderArgumentCaptor.getValue())
            .hasSize(1)
            .flatExtracting(
                Lovvalgsperiode::getFom,
                Lovvalgsperiode::getTom,
                Lovvalgsperiode::getMedlemskapstype,
                Lovvalgsperiode::getDekning,
                Lovvalgsperiode::getInnvilgelsesresultat,
                Lovvalgsperiode::getBestemmelse,
                Lovvalgsperiode::getLovvalgsland
            )
            .containsExactly(
                trygdeavtaleResultat.lovvalgsperiodeFom(),
                trygdeavtaleResultat.lovvalgsperiodeTom(),
                PLIKTIG,
                FULL_DEKNING_FTRL,
                INNVILGET,
                Lovvalgbestemmelser_trygdeavtale_uk.valueOf(trygdeavtaleResultat.bestemmelse()),
                Landkoder.NO
            );
    }

    @Test
    void hentVirksomheter_fraEreg_mappesKorrekt() {
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

        when(eregFasade.hentOrganisasjonNavn(ORGNR_1)).thenReturn(NAVN_1);
        when(eregFasade.hentOrganisasjonNavn(ORGNR_2)).thenReturn(NAVN_2);

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
            tilMedfolgendeFamilie(UUID_BARN_1, "fnr1", "navn1", BARN),
            tilMedfolgendeFamilie(UUID_BARN_2, "fnr2", "navn2", BARN),
            tilMedfolgendeFamilie(UUID_EKTEFELLE, "fnr3", "navn3", EKTEFELLE_SAMBOER)
        ));

        var response = trygdeavtaleService.hentFamiliemedlemmer(behandling);

        assertThat(response)
            .hasSize(3)
            .flatExtracting(
                MedfolgendeFamilie::getUuid,
                MedfolgendeFamilie::getFnr,
                MedfolgendeFamilie::getNavn,
                MedfolgendeFamilie::getRelasjonsrolle)
            .containsExactlyInAnyOrder(
                UUID_BARN_1, "fnr1", "navn1", BARN,
                UUID_BARN_2, "fnr2", "navn2", BARN,
                UUID_EKTEFELLE, "fnr3", "navn3", EKTEFELLE_SAMBOER
            );
    }

    @Test
    void hentFamiliemedlemmer_ingenFamilie_tomListe() {
        var behandling = lagBehandlingMedFamilie(emptyList());
        var response = trygdeavtaleService.hentFamiliemedlemmer(behandling);
        assertThat(response).isEmpty();
    }

    private TrygdeavtaleResultat lagTrygdeavtaleResultat() {
        return new TrygdeavtaleResultat.Builder()
            .virksomhet(ORGNR_1)
            .bestemmelse(UK_ART6_1.getKode())
            .familie(new AvklarteMedfolgendeFamilie(
                Set.of(new OmfattetFamilie(UUID_BARN_2)),
                Set.of(
                    new IkkeOmfattetFamilie(
                        UUID_BARN_1,
                        OVER_18_AR.getKode(),
                        BEGRUNNELSE_BARN),
                    new IkkeOmfattetFamilie(
                        UUID_EKTEFELLE,
                        EGEN_INNTEKT.getKode(),
                        BEGRUNNELSE_SAMBOER)
                )
            ))
            .lovvalgsperiodeFom(PERIODE_FOM)
            .lovvalgsperiodeTom(PERIODE_TOM)
            .build();
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
