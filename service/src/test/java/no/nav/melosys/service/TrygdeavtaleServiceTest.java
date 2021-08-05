package no.nav.melosys.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.*;
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TrygdeavtaleServiceTest {
    private final static long BEH_ID = 1L;
    private final static String ORGNR_1 = "11111111111";
    private final static String ORGNR_2 = "22222222222";
    private final static String NAVN_1 = "Navn 1";
    private final static String NAVN_2 = "Navn 2";

    @Mock
    private BehandlingService behandlingService;

    @Mock
    private RegisterOppslagService registerOppslagService;

    private TrygdeavtaleService trygdeavtaleService;

    @BeforeEach
    void init() {
        trygdeavtaleService = new TrygdeavtaleService(behandlingService, registerOppslagService);
    }

    @Test
    void hentVirksomheter_fraRegisterOppslag_mappesKorrekt() {
        var selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.orgnr = ORGNR_1;
        var selvstendigArbeid = new SelvstendigArbeid();
        selvstendigArbeid.selvstendigForetak = List.of(selvstendigForetak);

        var juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        juridiskArbeidsgiverNorge.ekstraArbeidsgivere = List.of(ORGNR_2);

        when(behandlingService.hentBehandling(BEH_ID)).thenReturn(lagBehandlingMedVirksomheter(
            selvstendigArbeid,
            juridiskArbeidsgiverNorge,
            emptyList(),
            Set.of(lagArbForhSaksopplysning(emptyList()))
        ));
        when(registerOppslagService.hentOrganisasjon(ORGNR_1)).thenReturn(lagOrganisasjonsDokument(ORGNR_1, NAVN_1));
        when(registerOppslagService.hentOrganisasjon(ORGNR_2)).thenReturn(lagOrganisasjonsDokument(ORGNR_2, NAVN_2));

        var response = trygdeavtaleService.hentVirksomheter(BEH_ID);

        assertThat(response).size().isEqualTo(2);
        assertThat(response).containsEntry(ORGNR_1, NAVN_1);
        assertThat(response).containsEntry(ORGNR_2, NAVN_2);
    }

    @Test
    void hentVirksomheter_fraBehandlingSaksopplysning_mappesKorrekt() {
        when(behandlingService.hentBehandling(BEH_ID)).thenReturn(lagBehandlingMedVirksomheter(
            new SelvstendigArbeid(),
            new JuridiskArbeidsgiverNorge(),
            emptyList(),
            Set.of(
                lagArbForhSaksopplysning(List.of(ORGNR_1, ORGNR_2)),
                lagOrgSaksopplysning(ORGNR_1, NAVN_1),
                lagOrgSaksopplysning(ORGNR_2, NAVN_2))
        ));

        var response = trygdeavtaleService.hentVirksomheter(BEH_ID);

        assertThat(response).size().isEqualTo(2);
        assertThat(response).containsEntry(ORGNR_1, NAVN_1);
        assertThat(response).containsEntry(ORGNR_2, NAVN_2);
    }

    @Test
    void hentVirksomheter_fraBehandlingsgrunnlagForetakUtland_mappesKorrekt() {
        when(behandlingService.hentBehandling(BEH_ID)).thenReturn(lagBehandlingMedVirksomheter(
            new SelvstendigArbeid(),
            new JuridiskArbeidsgiverNorge(),
            lagForetakUtland(Map.of(ORGNR_1, NAVN_1, ORGNR_2, NAVN_2)),
            Set.of(lagArbForhSaksopplysning(emptyList()))
        ));

        var response = trygdeavtaleService.hentVirksomheter(BEH_ID);

        assertThat(response).size().isEqualTo(2);
        assertThat(response).containsEntry(ORGNR_1, NAVN_1);
        assertThat(response).containsEntry(ORGNR_2, NAVN_2);
    }

    @Test
    void hentVirksomheter_ingenVirksomheter_tomMap() {
        when(behandlingService.hentBehandling(BEH_ID)).thenReturn(lagBehandlingMedVirksomheter(
            new SelvstendigArbeid(),
            new JuridiskArbeidsgiverNorge(),
            emptyList(),
            Set.of(lagArbForhSaksopplysning(emptyList()))
        ));

        var response = trygdeavtaleService.hentVirksomheter(BEH_ID);

        assertThat(response).size().isEqualTo(0);
    }

    @Test
    void hentFamiliemedlemmer_barnOgEktefelle_fyltListe() {
        when(behandlingService.hentBehandling(BEH_ID)).thenReturn(
            lagBehandlingMedFamilie(List.of(
                lagMedfolgendeFamilie("uuid1", "navn1", MedfolgendeFamilie.Relasjonsrolle.BARN),
                lagMedfolgendeFamilie("uuid2", "navn2", MedfolgendeFamilie.Relasjonsrolle.BARN),
                lagMedfolgendeFamilie("uuid3", "navn3", MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER)
            )));

        var response = trygdeavtaleService.hentFamiliemedlemmer(BEH_ID);

        assertThat(response).size().isEqualTo(3);
        assertThat(response)
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
        when(behandlingService.hentBehandling(BEH_ID)).thenReturn(lagBehandlingMedFamilie(emptyList()));
        var response = trygdeavtaleService.hentFamiliemedlemmer(BEH_ID);
        assertThat(response).size().isEqualTo(0);
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

    private MedfolgendeFamilie lagMedfolgendeFamilie(String uuid, String navn, MedfolgendeFamilie.Relasjonsrolle rolle) {
        var medfolgendeFamilie = new MedfolgendeFamilie();
        medfolgendeFamilie.uuid = uuid;
        medfolgendeFamilie.navn = navn;
        medfolgendeFamilie.relasjonsrolle = rolle;
        return medfolgendeFamilie;
    }

    private List<ForetakUtland> lagForetakUtland(Map<String, String> uuidNavn) {
        return uuidNavn.entrySet().stream()
            .map(un -> {
                var foretakUtland = new ForetakUtland();
                foretakUtland.uuid = un.getKey();
                foretakUtland.navn = un.getValue();
                return foretakUtland;
            })
            .collect(Collectors.toList());
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
            .collect(Collectors.toList());

        var saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysning.setDokument(arbeidsforholdDokument);
        return saksopplysning;
    }
}
