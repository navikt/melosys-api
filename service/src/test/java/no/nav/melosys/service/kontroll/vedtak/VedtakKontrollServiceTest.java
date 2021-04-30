package no.nav.melosys.service.kontroll.vedtak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VedtakKontrollServiceTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private final long behandlingID = 33L;
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
    private final PersonDokument personDokument = new PersonDokument();
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();

    private VedtakKontrollService vedtakKontrollService;

    @Before
    public void setup() throws FunksjonellException {
        Saksopplysning medlSaksopplysning = new Saksopplysning();
        medlSaksopplysning.setType(SaksopplysningType.MEDL);
        medlSaksopplysning.setDokument(medlemskapDokument);

        Saksopplysning persopplysning = new Saksopplysning();
        persopplysning.setType(SaksopplysningType.PERSOPL);
        persopplysning.setDokument(personDokument);
        personDokument.bostedsadresse.setPoststed("altOK");

        Behandling behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        behandling.getSaksopplysninger().add(medlSaksopplysning);
        behandling.getSaksopplysninger().add(persopplysning);
        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);
        when(lovvalgsperiodeService.hentValidertLovvalgsperiode(eq(behandlingID))).thenReturn(lovvalgsperiode);

        vedtakKontrollService = new VedtakKontrollService(behandlingService, lovvalgsperiodeService);
    }

    @Test
    public void utførKontroller_periodeUnder24MndArt12IkkeOverlappendePeriode_returnererTomCollection() throws TekniskException, FunksjonellException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void utførKontroller_periodeOver24MndArt16IkkeOverlappendePeriode_returnererTomCollection() throws TekniskException, FunksjonellException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void utførKontroller_periodeOver24MndArt12MedOverlappendePeriode_returnererCollectionMedToKoder() throws TekniskException, FunksjonellException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(2));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(LocalDate.now().plusMonths(2), LocalDate.now().plusYears(2));
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemskapDokument.getMedlemsperiode().add(medlemsperiode);

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).extracting(Kontrollfeil::getKode).containsExactlyInAnyOrder(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, Kontroll_begrunnelser.PERIODEN_OVER_24_MD);
    }

    @Test
    public void utførKontroller_manglerBostedsadresse_returnererKode() throws TekniskException, FunksjonellException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        personDokument.bostedsadresse = new Bostedsadresse();

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.MANGLENDE_BOSTEDSADRESSE);
    }

    @Test
    public void utførKontroller_periodeManglerSluttdato_returnererKode() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(null);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.INGEN_SLUTTDATO);
    }

    @Test
    public void utførKontroller_arbeidsstedManglerFelter_returnererKode() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        behandlingsgrunnlagData.arbeidPaaLand.fysiskeArbeidssteder = List.of(new FysiskArbeidssted());

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED);
    }

    @Test
    public void utførKontroller_foretakUtlandManglerFelter_returnererKode() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        behandlingsgrunnlagData.foretakUtland = List.of(new ForetakUtland());

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL);
    }
}
