package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class OppfriskSaksopplysningerService {
    private static final Logger log = LoggerFactory.getLogger(OppfriskSaksopplysningerService.class);

    private final BehandlingRepository behandlingRepo;
    private final BehandlingsresultatService behandlingsresultatService;
    private final FagsakService fagsakService;
    private final RegelmodulService regelmodulService;
    private final RegisteropplysningerService registeropplysningerService;
    private final TpsFasade tpsFasade;

    public OppfriskSaksopplysningerService(BehandlingRepository behandlingRepo,
                                           BehandlingsresultatService behandlingsresultatService,
                                           FagsakService fagsakService,
                                           RegelmodulService regelmodulService,
                                           RegisteropplysningerService registeropplysningerService,
                                           TpsFasade tpsFasade) {
        this.behandlingRepo = behandlingRepo;
        this.behandlingsresultatService = behandlingsresultatService;
        this.fagsakService = fagsakService;
        this.regelmodulService = regelmodulService;
        this.registeropplysningerService = registeropplysningerService;
        this.tpsFasade = tpsFasade;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void oppfriskSaksopplysning(long behandlingID) throws MelosysException {
        log.info("Starter oppfrisking av behandlingID: {} ", behandlingID);

        Behandling behandling = behandlingRepo.findWithSaksopplysningerById(behandlingID);
        if (behandling == null) {
            throw new TekniskException("Behandling ikke funnet med behandlingID" + behandlingID);
        }

        String aktørID = behandling.getFagsak().hentBruker().getAktørId();
        String brukerID = tpsFasade.hentIdentForAktørId(aktørID);

        BehandlingsgrunnlagData grunnlagData = null;
        LocalDate fom = null, tom = null;

        if (harBehandlingsgrunnlagSøknad(behandling.getTema())) {
            grunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
            fom = grunnlagData.periode.getFom();
            tom = grunnlagData.periode.getTom();
        }
        if (harSedOpplysninger(behandling.getTema())) {
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

        Fagsak fagsak = behandling.getFagsak();
        if (grunnlagData != null && !Sakstyper.EU_EOS.equals(fagsak.getType())) {
            fagsak.setType(regelmodulService.kvalifisererForEf883_2004(behandlingID, grunnlagData.soeknadsland, grunnlagData.periode)
                ? Sakstyper.EU_EOS : Sakstyper.UKJENT);
            fagsakService.lagre(fagsak);
        }
    }

    private boolean harBehandlingsgrunnlagSøknad(Behandlingstema tema) {
        return tema == Behandlingstema.UTSENDT_ARBEIDSTAKER
            || tema == Behandlingstema.UTSENDT_SELVSTENDIG
            || tema == Behandlingstema.ARBEID_FLERE_LAND
            || tema == Behandlingstema.IKKE_YRKESAKTIV
            || tema == Behandlingstema.ARBEID_ETT_LAND_ØVRIG
            || tema == Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND;
    }

    private boolean harSedOpplysninger(Behandlingstema tema) {
        return tema == Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
            || tema == Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
            || tema == Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            || tema == Behandlingstema.BESLUTNING_LOVVALG_NORGE
            || tema == Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND;
    }

    private RegisteropplysningerRequest.SaksopplysningTyper utledSaksopplysningTyper(Behandlingstema behandlingstema) throws TekniskException {
        switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER:
            case UTSENDT_SELVSTENDIG:
            case ARBEID_FLERE_LAND:
            case IKKE_YRKESAKTIV:
            case ARBEID_ETT_LAND_ØVRIG:
            case ARBEID_NORGE_BOSATT_ANNET_LAND:
                return RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .personhistorikkopplysninger()
                    .arbeidsforholdopplysninger()
                    .inntektsopplysninger()
                    .medlemskapsopplysninger()
                    .organisasjonsopplysninger()
                    .sakOgBehandlingopplysninger()
                    .build();
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING:
            case REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE:
                return RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .personhistorikkopplysninger()
                    .inntektsopplysninger()
                    .medlemskapsopplysninger()
                    .utbetalingsopplysninger()
                    .build();
            case ANMODNING_OM_UNNTAK_HOVEDREGEL:
                return RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .personhistorikkopplysninger()
                    .arbeidsforholdopplysninger()
                    .inntektsopplysninger()
                    .medlemskapsopplysninger()
                    .organisasjonsopplysninger()
                    .utbetalingsopplysninger()
                    .build();
            case BESLUTNING_LOVVALG_NORGE:
            case BESLUTNING_LOVVALG_ANNET_LAND:
                return RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .personhistorikkopplysninger()
                    .arbeidsforholdopplysninger()
                    .inntektsopplysninger()
                    .medlemskapsopplysninger()
                    .organisasjonsopplysninger()
                    .sakOgBehandlingopplysninger()
                    .utbetalingsopplysninger()
                    .build();
            case ØVRIGE_SED:
            case TRYGDETID:
                return RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .build();
            default:
                throw new TekniskException("Ugyldig behandlingstema " + behandlingstema + " for oppfrisking av registeropplysninger");
        }
    }
}
