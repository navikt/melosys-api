package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.util.Collection;
import java.util.Objects;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll.FerdigbehandlingKontrollsett;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;

@Component
class Kontroll {
    private final BehandlingService behandlingService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    public Kontroll(BehandlingService behandlingService, LovvalgsperiodeService lovvalgsperiodeService, PersondataFasade persondataFasade, Unleash unleash) {
        this.behandlingService = behandlingService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
    }

    public void kontroller(long behandlingId, Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        var sakstype = behandling.getFagsak().getType();
        kontrollerVedtak(behandlingId, sakstype, behandlingsresultattype);
    }

    public void kontrollerVedtak(long behandlingID, Sakstyper sakstype, Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        Collection<Kontrollfeil> kontrollfeil = utførKontroller(behandlingID, sakstype, behandlingsresultattype);

        if (!kontrollfeil.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                kontrollfeil.stream().map(Kontrollfeil::tilDto).toList());
        }
    }

    Collection<Kontrollfeil> utførKontroller(long behandlingID, Sakstyper sakstype, Behandlingsresultattyper behandlingsresultattype) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);

        return switch (behandlingsresultattype) {
            case AVSLAG_MANGLENDE_OPPL, HENLEGGELSE -> utførKontrollerForAvslagOgHenleggelse(behandling);
            default -> utførKontroller(behandling, sakstype);
        };
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
        var regelsettForVedtak = FerdigbehandlingKontrollsett.hentRegelsettForVedtak(sakstype);

        FerdigbehandlingKontrollData ferdigbehandlingKontrollData;
        if (sakstype.equals(Sakstyper.FTRL) && unleash.isEnabled("melosys.folketrygden.mvp")) {
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
        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
            MottatteOpplysningerData mottatteOpplysningerData = null;

            if (!SaksbehandlingRegler.harTomFlyt(behandling, unleash.isEnabled("melosys.folketrygden.mvp"))) {
                mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
            }
            Persondata persondata = hentPersondata(behandling);
            return FerdigbehandlingKontrollData.lagKontrollDataForAvslag(persondata, mottatteOpplysningerData);
        } else  {
            MottatteOpplysningerData mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
            Persondata persondata = hentPersondata(behandling);
            return FerdigbehandlingKontrollData.lagKontrollDataForAvslag(persondata, mottatteOpplysningerData);
        }
    }

    private FerdigbehandlingKontrollData hentVedtakKontrollData(Behandling behandling) {
        Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(behandling.getId());
        Lovvalgsperiode opprinneligLovvalgsperiode = lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(behandling.getId()).orElse(null);
        MottatteOpplysningerData mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
        MedlemskapDokument medlemskapDokument = behandling.hentMedlemskapDokument();
        Persondata persondata = hentPersondata(behandling);

        return new FerdigbehandlingKontrollData(medlemskapDokument, persondata, mottatteOpplysningerData,
            lovvalgsperiode, opprinneligLovvalgsperiode);
    }

    private FerdigbehandlingKontrollData hentVedtakKontrollDataFTRL(Behandling behandling) {
        MedlemskapDokument medlemskapDokument = behandling.hentMedlemskapDokument();
        MottatteOpplysningerData mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
        Persondata persondata = hentPersondata(behandling);
        return FerdigbehandlingKontrollData.lagKontrollDataForFTRL(medlemskapDokument,persondata, mottatteOpplysningerData);
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
    }
}
