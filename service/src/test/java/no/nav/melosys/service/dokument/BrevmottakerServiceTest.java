package no.nav.melosys.service.dokument;

import java.util.List;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.REPRESENTANT;
import static org.assertj.core.api.Assertions.assertThat;
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
    private Behandling behandling;

    private BrevmottakerService brevmottakerService;

    @Before
    public void setup() throws TekniskException {
        brevmottakerService = new BrevmottakerService(kontaktopplysningService, avklarteVirksomheterService, utenlandskMyndighetService);
        when(avklarteVirksomheterService.hentArbeidsgivendeOrgnumre(eq(behandling))).thenReturn(Sets.newHashSet("123456789", "987654321"));
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
}
