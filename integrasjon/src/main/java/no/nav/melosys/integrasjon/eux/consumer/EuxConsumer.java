package no.nav.melosys.integrasjon.eux.consumer;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.melosys.eux.model.nav.SED;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.felles.RestConsumer;

public interface EuxConsumer extends RestConsumer {

    void setSakSensitiv(String rinaSaksnummer) throws MelosysException;

    void fjernSakSensitiv(String rinaSaksnummer) throws MelosysException;

    String opprettBucOgSedMedVedlegg(String bucType, String fagSakNummer, String mottakerId, String filType, String korrelasjonsId, SED sed, Object vedlegg) throws MelosysException;

    List<String> hentInstitusjoner(String bucType, String landkode) throws MelosysException;

    String opprettBuC(String bucType) throws MelosysException;

    void oppdaterSed(String rinaSaksnummer, String korrelasjonsId, String dokumentId, SED sed) throws MelosysException;

    void slettSed(String rinaSaksnummer, String dokumentId) throws MelosysException;

    void sendSed(String rinaSaksnummer, String korrelasjonsId, String dokumentId) throws MelosysException;

    void slettBuC(String rinaSaksnummer) throws MelosysException;

    JsonNode hentKodeverk(String kodeverk) throws MelosysException;

    JsonNode hentMuligeAksjoner(String rinaSaksnummer) throws MelosysException;

    String opprettBucOgSed(String bucType, String mottakerId, SED sed) throws MelosysException;

    JsonNode finnRinaSaker(String fnr, String fornavn, String etternavn, String fødselsdato, String rinaSaksnummer, String bucType, String status) throws MelosysException;

    String opprettSed(String rinaSaksnummer, String korrelasjonsId, SED sed) throws MelosysException;

    Object hentVedlegg(String rinaSaksnummer, String dokumentId, String vedleggId) throws MelosysException;

    String leggTilVedlegg(String rinaSaksnummer, String dokumentId, String filType, Object vedlegg) throws MelosysException;

    SED hentSed(String rinaSaksnummer, String dokumentId) throws MelosysException;

    JsonNode hentBuC(String buc) throws MelosysException;

    List<String> hentTilgjengeligeSedTyper(String rinaSaksnummer) throws MelosysException;

    void settMottaker(String rinaSaksnummer, String mottakerId) throws MelosysException;

    List<String> hentDeltagere(String rinaSaksnummer) throws MelosysException;

    List<String> bucTypePerSektor() throws MelosysException;

    void slettVedlegg(String rinaSaksnummer, String dokumentId, String vedleggId) throws MelosysException;
}
