package no.nav.melosys.saksflyt.steg.reg;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentOrganisasjonsopplysningerTest {

    @Mock
    private EregFasade eregFasade;

    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    @Mock
    private BehandlingService behandlingService;

    private HentOrganisasjonsopplysninger agent;

    @Before
    public void setUp() {
        agent = new HentOrganisasjonsopplysninger(behandlingService, saksopplysningRepository, eregFasade);
    }

    @Test
    public void utfoerSteg() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ARBFORH);

        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument();
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        String orgnr1 = "12";
        arbeidsforhold.arbeidsgiverID = orgnr1;
        arbeidsforholdDokument.getArbeidsforhold().add(arbeidsforhold);

        saksopplysning.setDokument(arbeidsforholdDokument);
        saksopplysninger.add(saksopplysning);

        p.getBehandling().setSaksopplysninger(saksopplysninger);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(p.getBehandling());
        when(eregFasade.hentOrganisasjon(eq(orgnr1))).thenReturn(new Saksopplysning());

        agent.utførSteg(p);

        verify(saksopplysningRepository).save(any(Saksopplysning.class));
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_MEDL_OPPL);
    }
}
