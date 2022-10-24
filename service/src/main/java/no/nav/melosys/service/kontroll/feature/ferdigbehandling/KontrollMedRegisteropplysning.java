package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
class KontrollMedRegisteropplysning {

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerService registeropplysningerService;
    private final Kontroll kontroll;

    public KontrollMedRegisteropplysning(BehandlingService behandlingService,
                                         BehandlingsresultatService behandlingsresultatService,
                                         PersondataFasade persondataFasade,
                                         RegisteropplysningerService registeropplysningerService,
                                         Kontroll kontroll) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerService = registeropplysningerService;
        this.kontroll = kontroll;
    }

    public void kontroller(long behandlingId, Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        Sakstyper sakstype = behandling.getFagsak().getType();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        kontrollerVedtak(behandling, behandlingsresultat, sakstype, behandlingsresultattype);
    }

    public void kontrollerVedtak(Behandling behandling,
                                 Behandlingsresultat behandlingsresultat, Sakstyper sakstype,
                                 Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        hentNyeRegisteropplysninger(behandlingsresultat, behandling);
        kontroll.kontrollerVedtak(behandling.getId(), sakstype, behandlingsresultattype);
    }

    private void hentNyeRegisteropplysninger(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        LocalDate fom;
        LocalDate tom;

        // Er det mulig å ikke vere medlem i en periode? Hvorfor bruker vi i det hele tatt lovvalgsperiode her?
        if (behandling.getFagsak().getType().equals(Sakstyper.FTRL)) {
            List<Medlemskapsperiode> list = new ArrayList(behandlingsresultat.getMedlemAvFolketrygden().getMedlemskapsperioder());
            fom = list.get(0).getFom();
            tom = list.get(0).getTom();
        } else {
            PeriodeOmLovvalg lovvalgsperiode = behandlingsresultat.hentValidertPeriodeOmLovvalg();
            fom = lovvalgsperiode.getFom();
            tom = lovvalgsperiode.getTom();
        }
        String fnr = persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID());

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.getId())
                .fnr(fnr)
                .fom(fom)
                .tom(tom)
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .medlemskapsopplysninger().build())
                .build());
    }
}
