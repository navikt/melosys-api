package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.time.Period;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.DokgenMetaKey;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;

public class MangelbrevBruker extends Mangelbrev {

    private MangelbrevBruker(Builder builder) {
        super(builder);
    }

    private MangelbrevBruker(DokgenBrevbestilling brevbestilling, Builder builder) throws TekniskException {
        super(brevbestilling, builder);
    }

    public static MangelbrevBruker av(DokgenBrevbestilling brevbestilling) throws IkkeFunnetException, TekniskException {
        Fagsak fagsak = brevbestilling.getBehandling().getFagsak();
        Behandlingsresultat behandlingsresultat = brevbestilling.getBehandlingsresultat();

        return new MangelbrevBruker(
            brevbestilling,
            new Mangelbrev.Builder()
                .medDatoMottatt(brevbestilling.getForsendelseMottatt())
                .medDatoVedtatt(hentVedtaksdato(behandlingsresultat))
                .medDatoInnsendingsfrist(Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV)))
                .medSakstype(fagsak.getType())
                .medBehandlingstype(fagsak.getSistOppdaterteBehandling().getType())
                .medSaksbehandlerNavn(fagsak.getEndretAv())
                .medFritekstMangelinfo(brevbestilling.getVariabeltFelt(DokgenMetaKey.FRITEKST_MANGELINFO, String.class))
                .medFritekstMottaksinfo(brevbestilling.getVariabeltFelt(DokgenMetaKey.FRITEKST_MOTTAKSINFO, String.class))
        );
    }
}
