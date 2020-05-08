package no.nav.melosys.saksflyt.steg.reg;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentOrganisasjonsopplysningerTest {

    @Mock
    private EregFasade eregFasade;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private BehandlingService behandlingService;
    @InjectMocks
    private RegisteropplysningerService registeropplysningerService;

    private HentOrganisasjonsopplysninger agent;
    private Behandling behandling;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        agent = new HentOrganisasjonsopplysninger(registeropplysningerService);

        behandling = new Behandling();
        behandling.setId(111L);
        behandling.setFagsak(new Fagsak());
        behandling.setSaksopplysninger(new HashSet<>());
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        String orgnr1 = "12";

        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument();
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = orgnr1;
        arbeidsforholdDokument.getArbeidsforhold().add(arbeidsforhold);
        when(saksopplysningerService.finnArbeidsforholdsopplysninger(anyLong())).thenReturn(Optional.of(arbeidsforholdDokument));

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ORG);
        when(eregFasade.hentOrganisasjon(eq(orgnr1))).thenReturn(saksopplysning);
    }

    @Test
    public void utfoerSteg() throws MelosysException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        agent.utfør(p);

        ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
        verify(behandlingService).lagre(behandlingCaptor.capture());

        Set<Saksopplysning> saksopplysninger = behandlingCaptor.getValue().getSaksopplysninger();

        assertThat(saksopplysninger).isNotNull();
        assertThat(saksopplysninger.size()).isEqualTo(1);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_MEDL_OPPL);
    }
}
