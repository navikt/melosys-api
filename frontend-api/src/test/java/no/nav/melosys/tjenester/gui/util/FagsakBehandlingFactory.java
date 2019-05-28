package no.nav.melosys.tjenester.gui.util;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Landkoder;

public class FagsakBehandlingFactory {
    public static Fagsak fagsakMedBehandlinger(Behandlingsstatus behandlingsstatusFørst,
                                         Behandlingsstatus BehandlingsstatusAndre,
                                         Behandlingsstatus BehandlingsstatusTredje
    ) {
        ArrayList<Behandling> behandlinger = new ArrayList<>();
        Fagsak fagsak = new Fagsak();
        Behandling b1 = new Behandling();
        b1.setId(1L);
        b1.setType(Behandlingstyper.SOEKNAD);
        b1.setRegistrertDato(Instant.parse("2019-01-10T10:37:30.00Z"));
        b1.setEndretDato(Instant.parse("2019-01-12T10:37:30.00Z"));
        b1.setStatus(behandlingsstatusFørst);

        HashSet<Saksopplysning> saksopplysninger = new HashSet<>();
        saksopplysninger.add(lagPersonSaksopplysning());
        saksopplysninger.add(lagSøknadOpplysning());

        b1.setSaksopplysninger(saksopplysninger);

        Behandling b2 = new Behandling();
        b2.setId(2L);
        b2.setStatus(BehandlingsstatusAndre);
        b2.setEndretDato(Instant.parse("2018-11-11T10:37:30.00Z"));
        b2.setRegistrertDato(Instant.parse("2018-11-12T10:37:30.00Z"));

        Behandling b3 = new Behandling();
        b2.setId(3L);
        b3.setStatus(BehandlingsstatusTredje);
        b3.setRegistrertDato(Instant.parse("2018-09-11T10:37:30.00Z"));
        b3.setEndretDato(Instant.parse("2018-09-12T10:37:30.00Z"));

        behandlinger.add(b1);
        behandlinger.add(b2);
        behandlinger.add(b3);
        fagsak.setBehandlinger(behandlinger);
        return fagsak;
    }

    private static Saksopplysning lagPersonSaksopplysning() {
        Saksopplysning saksopplysningPerson = new Saksopplysning();
        saksopplysningPerson.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        personDokument.sammensattNavn = "Joe Moe";
        saksopplysningPerson.setDokument(personDokument);
        return saksopplysningPerson;
    }

    private static Saksopplysning lagSøknadOpplysning() {
        SoeknadDokument soeknadDokument = new SoeknadDokument();
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse.landkode = "SE";
        soeknadDokument.soeknadsland.landkoder.add(Landkoder.DK.getKode());
        soeknadDokument.arbeidUtland = new ArrayList<>();
        soeknadDokument.arbeidUtland.add(arbeidUtland);
        soeknadDokument.oppholdUtland.oppholdslandkoder.add("FI");
        soeknadDokument.periode = new no.nav.melosys.domain.dokument.soeknad.Periode(
            LocalDate.of(2019,1,1), LocalDate.of(2019,2,1));
        Saksopplysning saksopplysningSøknad = new Saksopplysning();
        saksopplysningSøknad.setType(SaksopplysningType.SØKNAD);
        saksopplysningSøknad.setDokument(soeknadDokument);
        return saksopplysningSøknad;
    }
}
