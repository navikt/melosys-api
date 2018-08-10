package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import java.time.LocalDateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.*;
import no.nav.melosys.integrasjon.KonverteringsUtils;

public class BehandlingStatusMapper {

    // Identifiserer unik hendelse for hendelsesprodusent.
    private String hendelsesId;

    // Identifiserer opprettet behandling.
    private String behandlingsID;

    // Identifiserer i hvilken sak behandlingen inngår, i gjeldende applikasjon (fagsystem) dersom en slik eksisterer.
    private String applikasjonSakREF;

    // Unik identifikator for en hendelsesprodusent.
    private Applikasjoner hendelsesprodusentREF; // http://nav.no/kodeverk/Kodeverk/Applikasjoner

    // Tidspunktet hendelsen inntraff/oppstod (i produsentsystemet).
    private XMLGregorianCalendar hendelsesTidspunkt;

    // Identifiserer hvilken prosess behandlingen følger.
    private Sakstemaer sakstema; // http://nav.no/kodeverk/Kodeverk/Arkivtemaer

    // Identifiserer hvilke(n) aktør(er), person eller organisasjon, som 'eier' saken behandlingen inngår i.
    private Aktoer aktoerREF;

    private String ansvarligEnhetREF;

    public BehandlingStatusMapper(String hendelsesId, String behandlingsID, String saksnummer, Applikasjoner hendelsesprodusentREF, XMLGregorianCalendar hendelsestidspunkt, Sakstemaer sakstema, Aktoer aktoerREF, String ansvarligEnhetREF) {
        this.hendelsesId = hendelsesId;
        this.behandlingsID = behandlingsID;
        this.applikasjonSakREF = saksnummer;
        this.hendelsesprodusentREF = hendelsesprodusentREF;
        this.hendelsesTidspunkt = hendelsestidspunkt;
        this.sakstema = sakstema;
        this.aktoerREF = aktoerREF;
        this.ansvarligEnhetREF = ansvarligEnhetREF;
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

        public Builder medHendelsesId(String hendelsesId) {
            this.hendelsesId = hendelsesId;
            return this;
        }

        public Builder medBehandlingsId(String behandlingsId) {
            this.behandlingsId = behandlingsId;
            return this;
        }

        public Builder medSaksnummer(String saksnummer) {
            this.applikasjonSak = saksnummer;
            return this;
        }

        public Builder medHendelsesprodusent(String hendelsesprodusent) {
            this.hendelsesprodusent = new Applikasjoner();
            this.hendelsesprodusent.setValue(hendelsesprodusent);
            return this;
        }

        public Builder medHendelsestidspunkt(LocalDateTime hendelsestidspunkt) {
            try {
                this.hendelsestidspunkt = KonverteringsUtils.localDateTimeToXMLGregorianCalendar(hendelsestidspunkt);
            } catch (DatatypeConfigurationException e) {
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

        public Builder medAnsvarligEnhet(String ansvarligEnhet) {
            this.ansvarligEnhet = ansvarligEnhet;
            return this;
        }

        public BehandlingStatusMapper build() {
            return new BehandlingStatusMapper(hendelsesId, behandlingsId, applikasjonSak, hendelsesprodusent, hendelsestidspunkt, sakstema, aktør, ansvarligEnhet);
        }
    }

    public BehandlingOpprettet tilBehandlingOpprettet() {
        return utførMapping(new BehandlingOpprettet());
    }

    public BehandlingAvsluttet tilBehandlingAvsluttet() {
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
