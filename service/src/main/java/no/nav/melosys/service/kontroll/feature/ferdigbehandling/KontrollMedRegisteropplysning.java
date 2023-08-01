package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;

@Component
class KontrollMedRegisteropplysning {

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerService registeropplysningerService;
    private final Kontroll kontroll;
    private final Unleash unleash;

    public KontrollMedRegisteropplysning(BehandlingService behandlingService,
                                         BehandlingsresultatService behandlingsresultatService,
                                         PersondataFasade persondataFasade,
                                         RegisteropplysningerService registeropplysningerService,
                                         Kontroll kontroll, Unleash unleash) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerService = registeropplysningerService;
        this.kontroll = kontroll;
        this.unleash = unleash;
    }

    public Collection<Kontrollfeil> kontroller(long behandlingId, Behandlingsresultattyper behandlingsresultattype, Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        Sakstyper sakstype = behandling.getFagsak().getType();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        return kontrollerVedtak(behandling, behandlingsresultat, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres);
    }

    public Collection<Kontrollfeil> kontrollerVedtak(Behandling behandling,
                                                     Behandlingsresultat behandlingsresultat,
                                                     Sakstyper sakstype,
                                                     Behandlingsresultattyper behandlingsresultattype,
                                                     Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres) {
        hentNyeRegisteropplysninger(behandlingsresultat, behandling);
        return kontroll.kontrollerVedtak(behandling.getId(), sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres);
    }

    private void hentNyeRegisteropplysninger(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        var folketrygdenToggleEnabled = unleash.isEnabled("melosys.folketrygden.mvp");

        LocalDate fraOgMed;
        LocalDate tilOgMed;
        if (behandling.getFagsak().getType().equals(Sakstyper.FTRL) && folketrygdenToggleEnabled) {
            Medlemskapsperiode medlemskapsperiode = behandlingsresultat.hentValidertMedlemskapsPeriode();
            fraOgMed = medlemskapsperiode.getFom();
            tilOgMed = medlemskapsperiode.getTom();
        } else {
            PeriodeOmLovvalg lovvalgsperiode = behandlingsresultat.hentValidertPeriodeOmLovvalg();
            fraOgMed = lovvalgsperiode.getFom();
            tilOgMed = lovvalgsperiode.getTom();
        }
        String fnr = persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID());

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.getId())
                .fnr(fnr)
                .fom(fraOgMed)
                .tom(tilOgMed)
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .medlemskapsopplysninger().build())
                .build());
    }
}
