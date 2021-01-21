package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.DokgenMetaKey;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;

public class MangelbrevBruker extends Mangelbrev {

    protected MangelbrevBruker(String fnr, String saksnummer, Instant dagensDato, String navnBruker,
                               String navnMottaker, List<String> adresselinjer, String postnr,
                               String poststed, String land,
                               Instant datoMottatt, Instant datoVedtatt, Instant datoInnsendingsfrist,
                               Sakstyper sakstype, Behandlingstyper behandlingstype,
                               String saksbehandlerNavn, String fritekstMangelinfo, String fritekstMottaksinfo) {
        super(fnr, saksnummer, dagensDato, navnBruker, navnMottaker, adresselinjer, postnr, poststed,
            land, datoMottatt, datoVedtatt, datoInnsendingsfrist, sakstype, behandlingstype, saksbehandlerNavn,
            fritekstMangelinfo, fritekstMottaksinfo);
    }

    public static MangelbrevBruker av(DokgenBrevbestilling brevbestilling) throws TekniskException, IkkeFunnetException {
        Behandling behandling = brevbestilling.getBehandling();
        OrganisasjonDokument org = brevbestilling.getOrg();
        Fagsak fagsak = behandling.getFagsak();
        PersonDokument personDokument = behandling.hentPersonDokument();
        Behandlingsresultat behandlingsresultat = brevbestilling.getBehandlingsresultat();

        return new MangelbrevBruker(
            personDokument.fnr,
            fagsak.getSaksnummer(),
            Instant.now(),
            personDokument.sammensattNavn,
            (org == null ? personDokument.sammensattNavn : org.getNavn()),
            mapAdresselinjer(org, brevbestilling.getKontaktopplysning(), personDokument),
            mapPostnr(org, personDokument),
            mapPoststed(org, personDokument),
            mapLandForAdresse(org, personDokument),
            brevbestilling.getForsendelseMottatt(),
            hentVedtaksdato(behandlingsresultat),
            brevbestilling.getForsendelseMottatt().plus(SAKSBEHANDLINGSTID_DAGER, ChronoUnit.DAYS),
            fagsak.getType(),
            fagsak.getSistOppdaterteBehandling().getType(),
            fagsak.getEndretAv(),
            brevbestilling.getVariabeltFelt(DokgenMetaKey.FRITEKST_MANGELINFO, String.class),
            brevbestilling.getVariabeltFelt(DokgenMetaKey.FRITEKST_MOTTAKSINFO, String.class)
        );
    }
}
