package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.SaksopplysningerData;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll.FerdigbehandlingKontrollsett;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.HENLEGGELSE;

@Component
class Kontroll {
    private static final Logger log = LoggerFactory.getLogger(Kontroll.class);

    private final BehandlingService behandlingService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final PersondataFasade persondataFasade;
    private final SaksbehandlingRegler saksbehandlingRegler;
    private final Unleash unleash;

    public Kontroll(BehandlingService behandlingService, LovvalgsperiodeService lovvalgsperiodeService, AvklarteVirksomheterService avklarteVirksomheterService, PersondataFasade persondataFasade, SaksbehandlingRegler saksbehandlingRegler, Unleash unleash) {
        this.behandlingService = behandlingService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.persondataFasade = persondataFasade;
        this.saksbehandlingRegler = saksbehandlingRegler;
        this.unleash = unleash;
    }

    public Collection<Kontrollfeil> kontroller(long behandlingId, Behandlingsresultattyper behandlingsresultattype, Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres) {
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        var sakstype = behandling.getFagsak().getType();
        return kontrollerVedtak(behandlingId, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres);
    }

    public Collection<Kontrollfeil> kontrollerVedtak(long behandlingID, Sakstyper sakstype, Behandlingsresultattyper behandlingsresultattype, Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres) {
        return utførKontroller(behandlingID, sakstype, behandlingsresultattype).stream()
            .filter(feil -> skalViseFeil(feil, kontrollerSomSkalIgnoreres, behandlingID))
            .toList();
    }

    private boolean skalViseFeil(Kontrollfeil kontrollfeil, Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres, long behandlingID) {
        if (kontrollerSomSkalIgnoreres != null && kontrollerSomSkalIgnoreres.contains(kontrollfeil.getKode())) {
            log.info("Ignorerer kontroll %s for behandling %s".formatted(kontrollfeil.getKode().getKode(), behandlingID));
            return false;
        }
        return true;
    }

    Collection<Kontrollfeil> utførKontroller(long behandlingID, Sakstyper sakstype, Behandlingsresultattyper behandlingsresultattype) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);

        if (behandlingsresultattype == AVSLAG_MANGLENDE_OPPL || behandlingsresultattype == HENLEGGELSE) {
            return utførKontrollerForAvslagOgHenleggelse(behandling);
        } else {
            return utførKontroller(behandling, sakstype);
        }
    }

    private Collection<Kontrollfeil> utførKontrollerForAvslagOgHenleggelse(Behandling behandling) {
        var regelsettForAvslagOgHenleggelse = FerdigbehandlingKontrollsett.hentRegelsettForAvslagOgHenleggelse();
        var ferdigbehandlingKontrollData = hentKontrollDataForAvslagOgHenleggelse(behandling);
        return regelsettForAvslagOgHenleggelse.stream()
            .map(f -> f.apply(ferdigbehandlingKontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private Collection<Kontrollfeil> utførKontroller(Behandling behandling, Sakstyper sakstype) {
        boolean harRegistreringUnntakFraMedlemskapFlyt = saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling);
        boolean harIkkeYrkesaktivFlyt = saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling);

        var regelsettForVedtak = FerdigbehandlingKontrollsett.hentRegelsettForVedtak(sakstype, harRegistreringUnntakFraMedlemskapFlyt, harIkkeYrkesaktivFlyt);

        FerdigbehandlingKontrollData ferdigbehandlingKontrollData;
        if (sakstype.equals(Sakstyper.FTRL) && unleash.isEnabled(ToggleName.FOLKETRYGDEN_MVP)) {
            ferdigbehandlingKontrollData = hentVedtakKontrollDataFTRL(behandling);
        } else {
            ferdigbehandlingKontrollData = hentVedtakKontrollData(behandling);
        }

        return regelsettForVedtak.stream()
            .map(f -> f.apply(ferdigbehandlingKontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private FerdigbehandlingKontrollData hentKontrollDataForAvslagOgHenleggelse(Behandling behandling) {
        MottatteOpplysningerData mottatteOpplysningerData = null;

        if (!saksbehandlingRegler.harTomFlyt(behandling)) {
            mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
        }
        Persondata persondata = hentPersondata(behandling);
        SaksopplysningerData saksopplysningerData = hentSaksopplysningerData(behandling);

        return FerdigbehandlingKontrollData.lagKontrollDataForAvslag(persondata, mottatteOpplysningerData, saksopplysningerData);
    }

    private FerdigbehandlingKontrollData hentVedtakKontrollData(Behandling behandling) {
        Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(behandling.getId());
        Lovvalgsperiode opprinneligLovvalgsperiode = lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(behandling.getId()).orElse(null);
        MottatteOpplysningerData mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
        MedlemskapDokument medlemskapDokument = behandling.hentMedlemskapDokument();
        Persondata persondata = hentPersondata(behandling);
        SaksopplysningerData saksopplysningerData = hentSaksopplysningerData(behandling);

        return new FerdigbehandlingKontrollData(medlemskapDokument, persondata, mottatteOpplysningerData,
            lovvalgsperiode, opprinneligLovvalgsperiode, saksopplysningerData);
    }

    private FerdigbehandlingKontrollData hentVedtakKontrollDataFTRL(Behandling behandling) {
        MedlemskapDokument medlemskapDokument = behandling.hentMedlemskapDokument();
        MottatteOpplysningerData mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
        Persondata persondata = hentPersondata(behandling);
        return FerdigbehandlingKontrollData.lagKontrollDataForFTRL(persondata, mottatteOpplysningerData, medlemskapDokument);
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
    }

    private SaksopplysningerData hentSaksopplysningerData(Behandling behandling) {
        return new SaksopplysningerData(avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling));
    }
}
