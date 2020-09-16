package no.nav.melosys.service.kontroll.unntak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningUnntakKontrollServiceTest {
    @Mock
    private BehandlingService behandlingService;

    private final long behandlingID = 33L;
    private final PersonDokument personDokument = new PersonDokument();
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();

    private AnmodningUnntakKontrollService anmodningUnntakKontrollService;

    @Before
    public void setup() throws IkkeFunnetException {
        Saksopplysning persopplysning = new Saksopplysning();
        persopplysning.setType(SaksopplysningType.PERSOPL);
        persopplysning.setDokument(personDokument);
        personDokument.bostedsadresse.setPoststed("altOK");

        Behandling behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        behandling.getSaksopplysninger().add(persopplysning);
        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);

        anmodningUnntakKontrollService = new AnmodningUnntakKontrollService(behandlingService);
    }

    @Test
    public void utførKontroller_manglerBostedsadresse_returnererKode() throws TekniskException, FunksjonellException {
        personDokument.bostedsadresse = new Bostedsadresse();

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_BOSTEDSADRESSE);
    }

    @Test
    public void utførKontroller_arbeidsstedManglerFelter_returnererKode() throws FunksjonellException, TekniskException {
        behandlingsgrunnlagData.arbeidUtland = List.of(new ArbeidUtland());

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED);
    }

    @Test
    public void utførKontroller_foretakUtlandManglerFelter_returnererKode() throws FunksjonellException, TekniskException {
        behandlingsgrunnlagData.foretakUtland = List.of(new ForetakUtland());

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL);
    }
}
