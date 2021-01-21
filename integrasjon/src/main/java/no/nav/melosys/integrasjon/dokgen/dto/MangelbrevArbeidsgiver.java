package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.DokgenMetaKey;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;

public class MangelbrevArbeidsgiver extends Mangelbrev {

    private final String navnFullmektig;

    private MangelbrevArbeidsgiver(Builder builder, String navnFullmektig) {
        super(builder);
        this.navnFullmektig = navnFullmektig;
    }

    public String getNavnFullmektig() {
        return navnFullmektig;
    }

    public static MangelbrevArbeidsgiver av(DokgenBrevbestilling brevbestilling) throws TekniskException, IkkeFunnetException {
        Behandling behandling = brevbestilling.getBehandling();
        OrganisasjonDokument org = brevbestilling.getOrg();
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();
        Behandlingsresultat behandlingsresultat = brevbestilling.getBehandlingsresultat();

        return new MangelbrevArbeidsgiver(
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
                .medDatoInnsendingsfrist(brevbestilling.getForsendelseMottatt().plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS))
                .medSakstype(fagsak.getType())
                .medBehandlingstype(fagsak.getSistOppdaterteBehandling().getType())
                .medSaksbehandlerNavn(fagsak.getEndretAv())
                .medFritekstMangelinfo(brevbestilling.getVariabeltFelt(DokgenMetaKey.FRITEKST_MANGELINFO, String.class))
                .medFritekstMottaksinfo(brevbestilling.getVariabeltFelt(DokgenMetaKey.FRITEKST_MOTTAKSINFO, String.class)),
            brevbestilling.getVariabeltFelt(DokgenMetaKey.FULLMEKTIGNAVN, String.class)
        );
    }
}
