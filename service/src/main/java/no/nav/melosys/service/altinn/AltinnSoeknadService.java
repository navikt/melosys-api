package no.nav.melosys.service.altinn;

import no.nav.melosys.altinn.MedlemskapArbeidEOSM;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.altinn.SoknadMottakConsumer;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AltinnSoeknadService {

    private final SoknadMottakConsumer soknadMottakConsumer;
    private final FagsakService fagsakService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final TpsFasade tpsFasade;

    public AltinnSoeknadService(SoknadMottakConsumer soknadMottakConsumer,
                                FagsakService fagsakService,
                                BehandlingsgrunnlagService behandlingsgrunnlagService,
                                @Qualifier("system") TpsFasade tpsFasade) {
        this.soknadMottakConsumer = soknadMottakConsumer;
        this.fagsakService = fagsakService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.tpsFasade = tpsFasade;
    }

    public Behandling opprettFagsakOgBehandlingFraAltinnSøknad(String søknadReferanse) throws FunksjonellException, TekniskException {
        MedlemskapArbeidEOSM søknad = soknadMottakConsumer.hentSøknad(søknadReferanse);

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID(hentAktørID(søknad))
            .medArbeidsgiver(hentArbeidsgiver(søknad))
            .medRepresentant(hentRepresentant(søknad))
            .medRepresentantKontaktperson(hentRepresentantKontaktPerson(søknad))
            .medRepresentantRepresenterer(hentRepresenterer(søknad))
            .medBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medBehandlingstype(Behandlingstyper.SOEKNAD)
            .build();

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        behandlingsgrunnlagService.opprettBehandlingsgrunnlag(1L, new BehandlingsgrunnlagData()); //TODO: MELSOYS-3527

        return fagsak.hentAktivBehandling();
    }

    private String hentAktørID(MedlemskapArbeidEOSM søknad) throws IkkeFunnetException {
        return tpsFasade.hentAktørIdForIdent(søknad.getInnhold().getArbeidstaker().getFoedselsnummer());
    }

    private String hentArbeidsgiver(MedlemskapArbeidEOSM søknad) {
        return søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer();
    }

    private String hentRepresentant(MedlemskapArbeidEOSM søknad) {
        return søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer();
    }

    private Representerer hentRepresenterer(MedlemskapArbeidEOSM søknad) {
        if(søknad.getInnhold().getFullmakt().isFullmaktFraArbeidstaker() == null) return null;
        return søknad.getInnhold().getFullmakt().isFullmaktFraArbeidstaker() ? Representerer.BRUKER : Representerer.ARBEIDSGIVER; //FIXME: BRUKER eller BEGGE?
    }

    private String hentRepresentantKontaktPerson(MedlemskapArbeidEOSM søknad) {
        return søknad.getInnhold().getArbeidsgiver().getKontaktperson().getKontaktpersonNavn();
    }
}
