package no.nav.melosys.service.behandling;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING;

@Service
public class AngiBehandlingsresultatService {

    private static final String FEILMELDING = "Kan ikke sette behandlingsresultattype til %s på sak %s";

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

        behandlingsresultat.setType(behandlingsresultattype);
        behandlingsresultatService.lagre(behandlingsresultat);
        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);
    }

    private void validerBehandlingsresultattype(Behandlingsresultattyper behandlingsresultattype, Behandling behandling, Fagsak fagsak) {
        var sakstype = fagsak.getType();
        var sakstema = fagsak.getTema();
        var behandlingstype = behandling.getType();
        var behandlingstema = behandling.getTema();

        if (sakstema != Sakstemaer.MEDLEMSKAP_LOVVALG) {
            throw new FunksjonellException(String.format(FEILMELDING, behandlingsresultattype, fagsak.getSaksnummer()));
        }

        if (behandlingsresultattype == MEDLEM_I_FOLKETRYGDEN &&
            sakstype == FTRL &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype) &&
            Set.of(YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST).contains(behandlingstema)) {
            return;
        }
        if (behandlingsresultattype == UNNTATT_MEDLEMSKAP &&
            sakstype == FTRL &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype) &&
            behandlingstema == UNNTAK_MEDLEMSKAP) {
            return;
        }
        if (behandlingsresultattype == FASTSATT_LOVVALGSLAND &&
            Set.of(EU_EOS, TRYGDEAVTALE).contains(sakstype) &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype) &&
            Set.of(YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST).contains(behandlingstema)) {
            return;
        }
        if (behandlingsresultattype == AVSLAG_SØKNAD &&
            Set.of(FØRSTEGANG, NY_VURDERING).contains(behandlingstype) &&
            Set.of(ARBEID_ETT_LAND_ØVRIG, YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST, UNNTAK_MEDLEMSKAP).contains(behandlingstema)) {
            return;
        }

        throw new FunksjonellException(String.format(FEILMELDING, behandlingsresultattype, fagsak.getSaksnummer()));
    }

}
