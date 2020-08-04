package no.nav.melosys.service.altinn;

import java.util.List;

import no.nav.melosys.altinn.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.altinn.SoknadMottakConsumer;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AltinnSoeknadServiceTest {

    @Mock
    private SoknadMottakConsumer soknadMottakConsumer;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private TpsFasade tpsFasade;

    private AltinnSoeknadService altinnSoeknadService;

    private final String soknadID = "13423";

    @Captor
    private ArgumentCaptor<OpprettSakRequest> captor;

    @Before
    public void setup() {
        altinnSoeknadService = new AltinnSoeknadService(soknadMottakConsumer, fagsakService, behandlingsgrunnlagService, tpsFasade);
    }

    @Test
    public void opprettFagsakOgBehandlingFraAltinnSøknad_soeknadEksisterer_verifiserFagsakBehandlingOgBehandlinggrunnlagOpprettet() throws FunksjonellException, TekniskException {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();
        final String aktørID = "123321123";

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        assertThat(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID)).isEqualTo(fagsak.hentAktivBehandling());

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getBehandlingstema()).isEqualTo(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        assertThat(req.getBehandlingstype()).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(req.getArbeidsgiver()).isEqualTo(søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer());
        assertThat(req.getAktørID()).isEqualTo(aktørID);
    }

    private MedlemskapArbeidEOSM lagMedlemskapArbeidEOSM() {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = new MedlemskapArbeidEOSM();
        Innhold innhold = new Innhold();
        medlemskapArbeidEOSM.setInnhold(innhold);

        innhold.setFullmakt(new Fullmakt());
        innhold.getFullmakt().setFullmektigVirksomhetsnummer("123333");

        innhold.setArbeidsgiver(new Arbeidsgiver());
        innhold.getArbeidsgiver().setVirksomhetsnummer("53254352");
        innhold.getArbeidsgiver().setKontaktperson(new Kontaktperson());
        innhold.getArbeidsgiver().getKontaktperson().setKontaktpersonNavn("Navne Navnesen");

        innhold.setArbeidstaker(new Arbeidstaker());
        innhold.getArbeidstaker().setFoedselsnummer("12345612345");
        return medlemskapArbeidEOSM;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        fagsak.setBehandlinger(List.of(behandling));

        return fagsak;
    }
}