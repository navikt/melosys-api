package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.time.Period;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
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

    public static MangelbrevBruker av(DokgenBrevbestilling brevbestilling) throws TekniskException, IkkeFunnetException {
        Behandling behandling = brevbestilling.getBehandling();
        OrganisasjonDokument org = brevbestilling.getOrg();
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();
        Behandlingsresultat behandlingsresultat = brevbestilling.getBehandlingsresultat();

        return new MangelbrevBruker(
            new Mangelbrev.Builder()
                .medFnr(personDokument.fnr)
                .medSaksnummer(fagsak.getSaksnummer())
                .medDagensDato(Instant.now())
                .medNavnBruker(personDokument.sammensattNavn)
                .medNavnMottaker((org == null ? personDokument.sammensattNavn : org.getNavn()))
                .medAdresselinjer(mapAdresselinjer(org, brevbestilling.getKontaktopplysning(), personDokument))
                .medPostnr(mapPostnr(org, personDokument))
                .medPoststed(mapPoststed(org, personDokument))
                .medLand(mapLandForAdresse(org, personDokument))
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
