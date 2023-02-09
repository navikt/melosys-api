package no.nav.melosys.service.behandling;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG;
import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;

@Service
public class AngiBehandlingsresultatService {

    private static final Logger log = LoggerFactory.getLogger(AngiBehandlingsresultatService.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;
    private final FagsakService fagsakService;

    public AngiBehandlingsresultatService(BehandlingsresultatService behandlingsresultatService,
                                          OppgaveService oppgaveService,
                                          FagsakService fagsakService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.fagsakService = fagsakService;
    }

    @Transactional
    public void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(long behandlingID, Behandlingsresultattyper behandlingsresultattype) {
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        var fagsak = behandlingsresultat.getBehandling().getFagsak();

        validerBehandlingsresultattype(behandlingsresultattype, behandlingsresultat.getBehandling(), fagsak);

        log.info("Avslutter sak {} og setter behandlingsresultattype {} på behandling {}", fagsak.getSaksnummer(), behandlingsresultattype, behandlingID);
        behandlingsresultat.setType(behandlingsresultattype);
        behandlingsresultatService.lagre(behandlingsresultat);
        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
    }

    private void validerBehandlingsresultattype(Behandlingsresultattyper behandlingsresultattype, Behandling behandling, Fagsak fagsak) {
        var sakstype = fagsak.getType();
        var sakstema = fagsak.getTema();
        var behandlingstema = behandling.getTema();
        var behandlingstype = behandling.getType();

        switch (behandlingsresultattype) {
            case MEDLEM_I_FOLKETRYGDEN -> {
                if (erGyldigEndringForMEDLEM_I_FOLKETRYGDEN(sakstype, sakstema, behandlingstema, behandlingstype))
                    return;
            }
            case UNNTATT_MEDLEMSKAP -> {
                if (erGyldigEndringForUNTATT_MEDLEMSKAP(sakstype, sakstema, behandlingstema, behandlingstype))
                    return;
            }
            case FASTSATT_LOVVALGSLAND -> {
                if (erGyldigEndringForFASTSATT_LOVVALGSLAND(sakstype, sakstema, behandlingstema, behandlingstype))
                    return;
            }
            case AVSLAG_SØKNAD -> {
                if (erGyldigEndringForAVSLAG_SØKNAD(sakstema, behandlingstema, behandlingstype))
                    return;
            }
            case MEDHOLD, KLAGEINNSTILLING, AVVIST_KLAGE -> {
                if (behandlingstype == KLAGE) return;
            }
            case OMGJORT -> {
                if (behandlingstype == NY_VURDERING) return;
            }
            case REGISTRERT_UNNTAK, DELVIS_GODKJENT_UNNTAK -> {
                if (erGyldigEndringForUnntak(sakstype, behandlingstema))
                    return;
            }
            default ->
                throw new FunksjonellException("Kan ikke endre til behandlingsresultattype: " + behandlingsresultattype.getBeskrivelse());
        }

        throw new FunksjonellException(String.format("Kan ikke endre behandlingsresultattype til %s på sak %s", behandlingsresultattype, fagsak.getSaksnummer()));
    }

    private boolean erGyldigEndringForMEDLEM_I_FOLKETRYGDEN(Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        return (sakstype == FTRL &&
            sakstema == MEDLEMSKAP_LOVVALG &&
            Set.of(YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST).contains(behandlingstema) &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype)) || erGyldigEndringForUnntak(sakstype, behandlingstema);

    }

    private boolean erGyldigEndringForUNTATT_MEDLEMSKAP(Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        return sakstype == FTRL &&
            sakstema == MEDLEMSKAP_LOVVALG &&
            behandlingstema == UNNTAK_MEDLEMSKAP &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype);
    }

    private boolean erGyldigEndringForFASTSATT_LOVVALGSLAND(Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        return Set.of(EU_EOS, TRYGDEAVTALE).contains(sakstype) &&
            sakstema == MEDLEMSKAP_LOVVALG &&
            Set.of(ARBEID_KUN_NORGE, YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST).contains(behandlingstema) &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype);
    }

    private boolean erGyldigEndringForAVSLAG_SØKNAD(Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        return sakstema == MEDLEMSKAP_LOVVALG &&
            Set.of(ARBEID_TJENESTEPERSON_ELLER_FLY, ARBEID_KUN_NORGE, YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST, UNNTAK_MEDLEMSKAP).contains(behandlingstema) &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype);
    }

    private boolean erGyldigEndringForUnntak(Sakstyper sakstype, Behandlingstema behandlingstema) {
        return (sakstype == TRYGDEAVTALE && Set.of(ANMODNING_OM_UNNTAK_HOVEDREGEL, REGISTRERING_UNNTAK).contains(behandlingstema))
            || (sakstype == EU_EOS && behandlingstema == A1_ANMODNING_OM_UNNTAK_PAPIR);
    }
}
