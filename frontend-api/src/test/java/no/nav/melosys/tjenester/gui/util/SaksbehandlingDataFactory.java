package no.nav.melosys.tjenester.gui.util;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

public final class SaksbehandlingDataFactory {
    public static Fagsak lagFagsak(String saksnummer) {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setType(Sakstyper.EU_EOS);
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

    public static Saksopplysning lagPersonSaksopplysning() {
        Saksopplysning saksopplysningPerson = new Saksopplysning();
        saksopplysningPerson.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        personDokument.setSammensattNavn("Joe Moe");
        saksopplysningPerson.setDokument(personDokument);
        return saksopplysningPerson;
    }

    public static Soeknad lagSøknadDokument() {
        Soeknad soeknad = new Soeknad();
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse.setLandkode("SE");
        soeknad.soeknadsland.landkoder.add(Landkoder.DK.getKode());
        soeknad.soeknadsland.erUkjenteEllerAlleEosLand = false;
        soeknad.arbeidPaaLand.fysiskeArbeidssteder = new ArrayList<>();
        soeknad.arbeidPaaLand.fysiskeArbeidssteder.add(fysiskArbeidssted);
        soeknad.oppholdUtland.oppholdslandkoder.add("FI");
        soeknad.periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(
            LocalDate.of(2019,1,1), LocalDate.of(2019,2,1));
        return soeknad;
    }
}
