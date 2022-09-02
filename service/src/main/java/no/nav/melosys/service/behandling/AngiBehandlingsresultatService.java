package no.nav.melosys.service.behandling;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;

@Service
public class AngiBehandlingsresultatService {

    private static final Logger log = LoggerFactory.getLogger(AngiBehandlingsresultatService.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final FagsakService fagsakService;

    public AngiBehandlingsresultatService(BehandlingsresultatService behandlingsresultatService,
                                          FagsakService fagsakService) {
        this.behandlingsresultatService = behandlingsresultatService;
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
    }

    private void validerBehandlingsresultattype(Behandlingsresultattyper behandlingsresultattype, Behandling behandling, Fagsak fagsak) {
        var sakstype = fagsak.getType();
        var sakstema = fagsak.getTema();
        var behandlingstema = behandling.getTema();
        var behandlingstype = behandling.getType();

        if (sakstema != Sakstemaer.MEDLEMSKAP_LOVVALG) {
            throw new FunksjonellException(String.format("Kan ikke endre behandlingsresultattype på sak %s siden den har sakstema %s", fagsak.getSaksnummer(), sakstema));
        }

        if (behandlingsresultattype == MEDLEM_I_FOLKETRYGDEN &&
            sakstype == FTRL &&
            Set.of(YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST).contains(behandlingstema) &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype)
        ) {
            return;
        }
        if (behandlingsresultattype == UNNTATT_MEDLEMSKAP &&
            sakstype == FTRL &&
            behandlingstema == UNNTAK_MEDLEMSKAP &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype)
        ) {
            return;
        }
        if (behandlingsresultattype == FASTSATT_LOVVALGSLAND &&
            Set.of(EU_EOS, TRYGDEAVTALE).contains(sakstype) &&
            Set.of(ARBEID_KUN_NORGE, YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST).contains(behandlingstema) &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype)
        ) {
            return;
        }
        if (behandlingsresultattype == AVSLAG_SØKNAD && Set.of(ARBEID_ETT_LAND_ØVRIG, ARBEID_TJENESTEPERSON_ELLER_FLY,
                                                               ARBEID_KUN_NORGE, YRKESAKTIV, IKKE_YRKESAKTIV,
                                                               PENSJONIST, UNNTAK_MEDLEMSKAP).contains(
            behandlingstema) && Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype)) {
            return;
        }
        if (Set.of(MEDHOLD, KLAGEINNSTILLING, AVVIST_KLAGE).contains(behandlingsresultattype) && behandlingstype == KLAGE) {
            return;
        }
        if (behandlingsresultattype == OMGJORT && behandlingstype == NY_VURDERING) {
            return;
        }


        throw new FunksjonellException(String.format("Kan ikke endre behandlingsresultattype til %s på sak %s", behandlingsresultattype, fagsak.getSaksnummer()));
    }

}
