package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll.FerdigbehandlingKontrollsett;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FerdigbehandlingKontrollService {
    private final BehandlingService behandlingService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final PersondataFasade persondataFasade;

    public FerdigbehandlingKontrollService(BehandlingService behandlingService, LovvalgsperiodeService lovvalgsperiodeService, PersondataFasade persondataFasade) {
        this.behandlingService = behandlingService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.persondataFasade = persondataFasade;
    }

    @Transactional
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

    public Collection<Kontrollfeil> utførKontroller(long behandlingID, Sakstyper sakstype, Behandlingsresultattyper behandlingsresultattype) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);

        return switch (behandlingsresultattype) {
            case AVSLAG_MANGLENDE_OPPL, HENLEGGELSE -> utførKontrollerForAvslagOgHenleggelse(behandling);
            default -> utførKontroller(behandling, sakstype);
        };
    }

    private Collection<Kontrollfeil> utførKontrollerForAvslagOgHenleggelse(Behandling behandling) {
        Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> vedtakKontroller = FerdigbehandlingKontrollsett.hentRegelsettForAvslagOgHenleggelse();
        var kontrollData = hentKontrollDataForAvslagOgHenleggelse(behandling);
        return vedtakKontroller.stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private Collection<Kontrollfeil> utførKontroller(Behandling behandling, Sakstyper sakstype) {
        Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> vedtakKontroller =
            FerdigbehandlingKontrollsett.hentRegelsettForVedtak(sakstype);
        var vedtakKontrollData = hentVedtakKontrollData(behandling);
        return vedtakKontroller.stream()
            .map(f -> f.apply(vedtakKontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private FerdigbehandlingKontrollData hentKontrollDataForAvslagOgHenleggelse(Behandling behandling) {
        BehandlingsgrunnlagData behandlingsgrunnlagData =
            behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        Persondata persondata = hentPersondata(behandling);
        return FerdigbehandlingKontrollData.lagKontrollDataForAvslag(persondata, behandlingsgrunnlagData);
    }

    private FerdigbehandlingKontrollData hentVedtakKontrollData(Behandling behandling) {
        Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandling.getId());
        Lovvalgsperiode opprinneligLovvalgsperiode =
            lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(behandling.getId()).orElse(null);
        var behandlingsgrunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        var medlemskapDokument = behandling.hentMedlemskapDokument();
        var persondata = hentPersondata(behandling);

        return new FerdigbehandlingKontrollData(medlemskapDokument, persondata, behandlingsgrunnlagData,
            lovvalgsperiode, opprinneligLovvalgsperiode);
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
    }
}
