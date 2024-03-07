package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KontrollMedRegisterOpplysningServiceTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;
    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private OrganisasjonOppslagService organisasjonOppslagService;
    @Mock
    private MedlemskapsperiodeService medlemskapsperiodeService;
    private KontrollMedRegisteropplysning kontrollMedRegisterOpplysning;

    private final long behandlingID = 1L;
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final MedlemskapDokument medlemskapDokument = new MedlemskapDokument();

    private final MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
    private final Behandling behandling = lagBehandling(mottatteOpplysningerData);

    @BeforeEach
    void setup() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        Saksopplysning medlSaksopplysning = new Saksopplysning();
        medlSaksopplysning.setType(SaksopplysningType.MEDL);
        medlSaksopplysning.setDokument(medlemskapDokument);

        behandling.getSaksopplysninger().add(medlSaksopplysning);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);

        Kontroll kontroll = new Kontroll(behandlingService, lovvalgsperiodeService, avklarteVirksomheterService, persondataFasade, organisasjonOppslagService, saksbehandlingRegler, medlemskapsperiodeService);
        kontrollMedRegisterOpplysning = new KontrollMedRegisteropplysning(behandlingService, persondataFasade, registeropplysningerService, kontroll);
    }

    @Test
    void kontrollerVedtakMedNyeRegisteropplysninger_feilFraKontroller_kasterExceptionMedFeilkode() {
        when(persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID())).thenReturn("fnr");
        when(lovvalgsperiodeService.hentLovvalgsperiode(behandlingID)).thenReturn(lovvalgsperiode);

        Collection<Kontrollfeil> kontrollfeilCollection = kontrollMedRegisterOpplysning.kontrollerVedtak(behandling, Sakstyper.EU_EOS, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, null);
        assertThat(kontrollfeilCollection).hasSize(1);
        assertThat(kontrollfeilCollection.stream().findFirst().get().getKode().getKode()).isEqualTo(Kontroll_begrunnelser.INGEN_SLUTTDATO.getKode());
    }

    @Test
    void kontrollerVedtak_oppdaterRegisteropplysninger_oppdatererRegisteropplysninger() throws ValideringException {
        lovvalgsperiode.setTom(LocalDate.now());
        when(persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID())).thenReturn("fnr");
        when(lovvalgsperiodeService.hentLovvalgsperiode(behandlingID)).thenReturn(lovvalgsperiode);

        kontrollMedRegisterOpplysning.kontroller(behandling.getId(), Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN, null);
        verify(registeropplysningerService).hentOgLagreOpplysninger(any());
    }

    @Test
    void kontrollerVedtak_AvslagPersonUtenRegistrertAdresse_returnererKode() {
        when(persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID())).thenReturn("fnr");
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());

        var feilIgnoreres = Set.of(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER);
        var annenFeilIgnoreres = Set.of(Kontroll_begrunnelser.MANGLER_VIRKSOMHET);

        assertThat(kontrollMedRegisterOpplysning.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, feilIgnoreres)).isEmpty();

        assertThat(kontrollMedRegisterOpplysning.kontroller(behandlingID, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, annenFeilIgnoreres)).hasSize(1);
    }
}
