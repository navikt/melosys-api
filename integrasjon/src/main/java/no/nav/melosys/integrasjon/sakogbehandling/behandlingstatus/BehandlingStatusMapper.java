package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import java.time.LocalDateTime;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.*;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.integrasjon.KonverteringsUtils;

import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;

public class BehandlingStatusMapper {

    // Identifiserer unik hendelse for hendelsesprodusent.
    private final String hendelsesId;

    // Identifiserer opprettet behandling.
    private final String behandlingsID;

    // Identifiserer i hvilken sak behandlingen inngår, i gjeldende applikasjon (fagsystem) dersom en slik eksisterer.
    private final String applikasjonSakREF;

    // Unik identifikator for en hendelsesprodusent.
    private final Applikasjoner hendelsesprodusentREF; // http://nav.no/kodeverk/Kodeverk/Applikasjoner

    // Tidspunktet hendelsen inntraff/oppstod (i produsentsystemet).
    private final XMLGregorianCalendar hendelsesTidspunkt;

    // Identifiserer hvilken prosess behandlingen følger.
    private final Sakstemaer sakstema; // http://nav.no/kodeverk/Kodeverk/Arkivtemaer

    // Identifiserer hvilke(n) aktør(er), person eller organisasjon, som 'eier' saken behandlingen inngår i.
    private final Aktoer aktoerREF;

    private final String ansvarligEnhetREF;

    @SuppressWarnings("squid:S00107")
    private BehandlingStatusMapper(String hendelsesId,
                                   String behandlingsID,
                                   String saksnummer,
                                   Applikasjoner hendelsesprodusentREF,
                                   XMLGregorianCalendar hendelsestidspunkt,
                                   Sakstemaer sakstema,
                                   Aktoer aktoerREF,
                                   String ansvarligEnhetREF) {
        this.hendelsesId = hendelsesId;
        this.behandlingsID = behandlingsID;
        this.applikasjonSakREF = saksnummer;
        this.hendelsesprodusentREF = hendelsesprodusentREF;
        this.hendelsesTidspunkt = hendelsestidspunkt;
        this.sakstema = sakstema;
        this.aktoerREF = aktoerREF;
        this.ansvarligEnhetREF = ansvarligEnhetREF;
    }

    public String getHendelsesId() {
        return hendelsesId;
    }

    public String getBehandlingsID() {
        return behandlingsID;
    }

    public String getApplikasjonSakREF() {
        return applikasjonSakREF;
    }

    public Applikasjoner getHendelsesprodusentREF() {
        return hendelsesprodusentREF;
    }

    public XMLGregorianCalendar getHendelsesTidspunkt() {
        return hendelsesTidspunkt;
    }

    public Sakstemaer getSakstema() {
        return sakstema;
    }

    public Aktoer getAktoerREF() {
        return aktoerREF;
    }

    public String getAnsvarligEnhetREF() {
        return ansvarligEnhetREF;
    }

    public static class Builder {

        private String hendelsesId;
        private String behandlingsId;
        private String applikasjonSak;
        private Applikasjoner hendelsesprodusent;
        private XMLGregorianCalendar hendelsestidspunkt;
        private Sakstemaer sakstema;
        private Aktoer aktør;
        private String ansvarligEnhet;

        private Builder medHendelsesId(String hendelsesId) {
            this.hendelsesId = hendelsesId;
            return this;
        }

        public Builder medBehandlingsId(long behandlingsId) {
            // BehandlingsId i SOB skal være unik i NAV, så vi prefikser med applikasjonsID.
            this.behandlingsId = String.format("%s-%d", Fagsystem.MELOSYS.getKode(), behandlingsId);
            return this;
        }

        public Builder medSaksnummer(String saksnummer) {
            this.applikasjonSak = saksnummer;
            return this;
        }

        private Builder medHendelsesprodusent(String hendelsesprodusent) {
            this.hendelsesprodusent = new Applikasjoner();
            this.hendelsesprodusent.setValue(hendelsesprodusent);
            return this;
        }

        private Builder medHendelsestidspunkt(LocalDateTime hendelsestidspunkt) {
            try {
                this.hendelsestidspunkt = KonverteringsUtils.localDateTimeToXMLGregorianCalendar(hendelsestidspunkt);
            } catch (DatatypeConfigurationException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        public Builder medArkivtema(String sakstema) {
            this.sakstema = new Sakstemaer();
            this.sakstema.setValue(sakstema);
            return this;
        }

        public Builder medAktørID(String aktørID) {
            this.aktør = new Aktoer();
            this.aktør.setAktoerId(aktørID);
            return this;
        }

        private Builder medAnsvarligEnhet(String ansvarligEnhet) {
            this.ansvarligEnhet = ansvarligEnhet;
            return this;
        }

        public BehandlingStatusMapper build() {
            this.medHendelsesId(generateCallId())
                .medHendelsesprodusent(Fagsystem.MELOSYS.getKode())
                .medHendelsestidspunkt(LocalDateTime.now())
                .medAnsvarligEnhet(Integer.toString(MELOSYS_ENHET_ID));

            return new BehandlingStatusMapper(hendelsesId, behandlingsId, applikasjonSak, hendelsesprodusent, hendelsestidspunkt, sakstema, aktør, ansvarligEnhet);
        }
    }

    BehandlingOpprettet tilBehandlingOpprettet() {
        return utførMapping(new BehandlingOpprettet());
    }

    BehandlingAvsluttet tilBehandlingAvsluttet() {
        return utførMapping(new BehandlingAvsluttet());
    }

    private <T extends BehandlingStatus> T utførMapping(T behandlingStatus) {
        behandlingStatus.setHendelsesId(hendelsesId);
        behandlingStatus.setBehandlingsID(behandlingsID);
        behandlingStatus.setApplikasjonSakREF(applikasjonSakREF);
        behandlingStatus.setHendelsesprodusentREF(hendelsesprodusentREF);
        behandlingStatus.setHendelsesTidspunkt(hendelsesTidspunkt);
        behandlingStatus.setSakstema(sakstema);
        behandlingStatus.getAktoerREF().add(aktoerREF);
        behandlingStatus.setAnsvarligEnhetREF(ansvarligEnhetREF);

        return behandlingStatus;
    }
}
