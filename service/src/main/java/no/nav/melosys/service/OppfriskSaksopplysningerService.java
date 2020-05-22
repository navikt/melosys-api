package no.nav.melosys.service;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.KontrollresultatService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory.utledSaksopplysningTyper;

@Service
public class OppfriskSaksopplysningerService {
    private static final Logger log = LoggerFactory.getLogger(OppfriskSaksopplysningerService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final FagsakService fagsakService;
    private final KontrollresultatService kontrollresultatService;
    private final InngangsvilkaarService inngangsvilkaarService;
    private final RegisteropplysningerService registeropplysningerService;
    private final TpsFasade tpsFasade;

    public OppfriskSaksopplysningerService(BehandlingService behandlingService,
                                           BehandlingsresultatService behandlingsresultatService,
                                           FagsakService fagsakService,
                                           KontrollresultatService kontrollresultatService,
                                           InngangsvilkaarService inngangsvilkaarService,
                                           RegisteropplysningerService registeropplysningerService,
                                           TpsFasade tpsFasade) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.fagsakService = fagsakService;
        this.kontrollresultatService = kontrollresultatService;
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.registeropplysningerService = registeropplysningerService;
        this.tpsFasade = tpsFasade;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void oppfriskSaksopplysning(long behandlingID) throws MelosysException {
        log.info("Starter oppfrisking av behandlingID: {} ", behandlingID);

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        String aktørID = behandling.getFagsak().hentBruker().getAktørId();
        String brukerID = tpsFasade.hentIdentForAktørId(aktørID);

        BehandlingsgrunnlagData grunnlagData = null;
        LocalDate fom = null;
        LocalDate tom = null;

        if (behandling.erBehandlingAvSøknad()) {
            grunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
            fom = grunnlagData.periode.getFom();
            tom = grunnlagData.periode.getTom();
        } else if (behandling.erBehandlingAvSed()) {
            SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
            fom = sedDokument.getLovvalgsperiode().getFom();
            tom = sedDokument.getLovvalgsperiode().getTom();
        }

        RegisteropplysningerRequest registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(behandlingID)
            .saksopplysningTyper(utledSaksopplysningTyper(behandling.getTema()))
            .fnr(brukerID)
            .fom(fom)
            .tom(tom)
            .build();

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest);
        behandlingsresultatService.tømBehandlingsresultat(behandlingID);

        if (behandling.erBehandlingAvSed()) {
            kontrollresultatService.utførKontrollerOgRegistrerFeil(behandlingID);
        }

        Fagsak fagsak = behandling.getFagsak();
        if (grunnlagData != null && !Sakstyper.EU_EOS.equals(fagsak.getType())) {
            boolean kvalifisererForEF_883_2004 = inngangsvilkaarService.vurderOgLagreInngangsvilkår(behandlingID, grunnlagData.soeknadsland.landkoder, grunnlagData.periode);
            fagsakService.oppdaterType(fagsak, kvalifisererForEF_883_2004);
        }
    }
}
