package no.nav.melosys.service.dokument;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevmottakerServiceTest {
    @Mock
    private KontaktopplysningService kontaktopplysningService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private Behandling behandling;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private Behandlingsresultat behandlingsresultat;
    private BrevmottakerService brevmottakerService;

    @Before
    public void setup() throws TekniskException, IkkeFunnetException {
        brevmottakerService = new BrevmottakerService(kontaktopplysningService, avklarteVirksomheterService, utenlandskMyndighetService, behandlingsresultatService);
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Sets.newHashSet("123456789", "987654321"));
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(eq(behandling))).thenReturn(Collections.singletonMap(lagUtenlandskMyndighet(), lagAktoerUtenlandskMyndighet()));

        behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolleOgIngenArbeidsgivere_feiler() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(eq(behandling))).thenReturn(Collections.emptyList());
        expectedException.expect(FunksjonellException.class);

        brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);
        expectedException.expectMessage("Arbeidsgiver er ikke registrert");
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolle_girArbeidsgiverAktører() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere.stream()
            .map(Aktoer::getOrgnr))
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    public void avklarMottakere_medBareUtenlandskeArbeidsgivere_girIngenMottakere() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(null));
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Collections.emptySet());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(eq(behandling)))
            .thenReturn(Collections.singletonList(new AvklartVirksomhet(new ForetakUtland())));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);
        assertThat(arbeidsgivere).isEmpty();
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBruker_girArbeidsgiverAktører() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.BRUKER));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere.stream()
            .map(Aktoer::getOrgnr))
            .containsExactlyInAnyOrder("123456789", "987654321");
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolleOgRepresentantForArbeidsgiver_girRepresentantAktør() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.ARBEIDSGIVER));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere.stream()
            .map(Aktoer::getOrgnr))
            .containsExactly("REP-ORGNR");
    }

    @Test
    public void avklarMottakere_medArbeidsgiverRolleOgRepresentantForBegge_girRepresentantAktør() throws FunksjonellException, TekniskException {
        when(behandling.getFagsak()).thenReturn(lagFagsakMedRepresentant(Representerer.BEGGE));

        List<Aktoer> arbeidsgivere = brevmottakerService.avklarMottakere(null, Mottaker.av(ARBEIDSGIVER), behandling);

        assertThat(arbeidsgivere.stream()
            .map(Aktoer::getOrgnr))
            .containsExactly("REP-ORGNR");
    }

    @Test
    public void avklarMottakere_art12_1_CZerReservertFraA1_forventerIngenAktør() throws FunksjonellException, TekniskException {
        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.av(MYNDIGHET), behandling);
        assertThat(myndigheter.isEmpty());
    }

    @Test
    public void avklarMottakere_art_11_4_2_CZerReservertFraA1_forventerIngenAktør() throws FunksjonellException, TekniskException {
        behandlingsresultat.hentValidertLovvalgsperiode().setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);
        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ATTEST_A1, Mottaker.av(MYNDIGHET), behandling);
        assertThat(myndigheter.isEmpty());
    }

    @Test
    public void avklarMottakere_A001_CZerReservertFraA1_forventerMyndighetAktør() throws FunksjonellException, TekniskException {
        List<Aktoer> myndigheter = brevmottakerService.avklarMottakere(Produserbaredokumenter.ANMODNING_UNNTAK, Mottaker.av(MYNDIGHET), behandling);
        assertThat(myndigheter.stream()
                .map(Aktoer::getInstitusjonId))
                .containsExactly("CZ:SZUC10416");
    }


    private Fagsak lagFagsakMedRepresentant(Representerer representerer) {
        Fagsak fagsak = new Fagsak();
        if (representerer != null) {
            Aktoer representant = new Aktoer();
            representant.setRepresenterer(representerer);
            representant.setRolle(REPRESENTANT);
            representant.setOrgnr("REP-ORGNR");
            fagsak.setAktører(Sets.newHashSet(representant));
        }
        return fagsak;
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.landkode = Landkoder.CZ;
        utenlandskMyndighet.institusjonskode = "SZUC10416";
        utenlandskMyndighet.preferanser = Collections.singleton(new Preferanse(1L, Preferanse.PreferanseEnum.RESERVERT_FRA_A1));

        return utenlandskMyndighet;
    }

    private Aktoer lagAktoerUtenlandskMyndighet() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(MYNDIGHET);
        aktoer.setInstitusjonId("CZ:SZUC10416");
        return aktoer;
    }
}
