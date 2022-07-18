package no.nav.melosys.service.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING;

@Service
public class AngiBehandlingsresultatService {

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
        var sakstema = fagsak.getTema();
        var sakstype = fagsak.getType();
        var behandlingstype = behandling.getType();
        var behandlingstema = behandling.getTema();

        if (sakstema == Sakstemaer.MEDLEMSKAP_LOVVALG) {
            if (behandlingsresultattype == MEDLEM_I_FOLKETRYGDEN &&
                sakstype == FTRL &&
                asList(FØRSTEGANG, NY_VURDERING).contains(behandlingstype) &&
                asList(YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST).contains(behandlingstema)) {
                return;
            }
            if (behandlingsresultattype == UNNTATT_MEDLEMSKAP &&
                sakstype == FTRL &&
                asList(FØRSTEGANG, NY_VURDERING).contains(behandlingstype) &&
                behandlingstema == UNNTAK_MEDLEMSKAP) {
                return;
            }
            if (behandlingsresultattype == FASTSATT_LOVVALGSLAND &&
                asList(EU_EOS, TRYGDEAVTALE).contains(sakstype) &&
                asList(FØRSTEGANG, NY_VURDERING).contains(behandlingstype) &&
                asList(YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST).contains(behandlingstema)) {
                return;
            }
            if (behandlingsresultattype == AVSLAG_SØKNAD &&
                asList(FØRSTEGANG, NY_VURDERING).contains(behandlingstype) &&
                asList(ARBEID_ETT_LAND_ØVRIG, YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST, UNNTAK_MEDLEMSKAP).contains(behandlingstema)) {
                return;
            }
        }

        throw new FunksjonellException("Denne saken kan ikke sette behandlingsresultattype til " + behandlingsresultattype);
    }

}
