package no.nav.melosys.service;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

public final class SaksbehandlingDataFactory {
    public static Behandling lagBehandling() {
        return lagBehandling(new BehandlingsgrunnlagData());
    }

    public static Behandling lagBehandling(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(lagFagsak());
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        return behandling;
    }

    public static Fagsak lagFagsak() {
        return lagFagsak("MEL-test");
    }

    public static Fagsak lagFagsak(String saksnummer) {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.getAktører().add(lagBruker());
        fagsak.setGsakSaksnummer(123L);
        return fagsak;
    }

    public static Aktoer lagBruker() {
        var aktoer = new Aktoer();
        aktoer.setRolle(BRUKER);
        aktoer.setAktørId("aktørID");
        return aktoer;
    }
}
