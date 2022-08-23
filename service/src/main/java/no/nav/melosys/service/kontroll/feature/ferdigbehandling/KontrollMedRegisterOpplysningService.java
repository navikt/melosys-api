package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KontrollMedRegisterOpplysningService {

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerService registeropplysningerService;

    private final FerdigbehandlingKontrollService ferdigbehandlingKontrollService;

    public KontrollMedRegisterOpplysningService(BehandlingService behandlingService,
                                                BehandlingsresultatService behandlingsresultatService,
                                                PersondataFasade persondataFasade,
                                                RegisteropplysningerService registeropplysningerService,
                                                FerdigbehandlingKontrollService ferdigbehandlingKontrollService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerService = registeropplysningerService;
        this.ferdigbehandlingKontrollService = ferdigbehandlingKontrollService;
    }

    @Transactional
    public void kontroller(long behandlingId, Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        var sakstype = behandling.getFagsak().getType();
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        kontrollerVedtak(behandling, behandlingsresultat, sakstype, behandlingsresultattype);
    }

    public void kontrollerVedtak(Behandling behandling,
                                 Behandlingsresultat behandlingsresultat, Sakstyper sakstype,
                                 Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        hentNyeRegisteropplysninger(behandlingsresultat, behandling);
        ferdigbehandlingKontrollService.kontrollerVedtak(behandling.getId(), sakstype, behandlingsresultattype);
    }

    private void hentNyeRegisteropplysninger(Behandlingsresultat behandlingsresultat, Behandling behandling) {
        PeriodeOmLovvalg lovvalgsperiode = behandlingsresultat.hentValidertPeriodeOmLovvalg();
        String fnr = persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID());

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.getId())
                .fnr(fnr)
                .fom(lovvalgsperiode.getFom())
                .tom(lovvalgsperiode.getTom())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .medlemskapsopplysninger().build())
                .build());
    }
}
