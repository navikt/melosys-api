package no.nav.melosys.integrasjon.eux.consumer;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.felles.RestConsumer;

import java.util.List;

interface EuxConsumer extends RestConsumer {

    List<String> getInstitusjoner(String bucType, String landkode) throws MelosysException;

    String opprettBucOgSed(String bucType, String fagSakNummer, String mottakerId, String filType, String korrelasjonsId) throws MelosysException;

    String opprettRinaSak(String bucType) throws MelosysException;

    void oppdaterSed(String rinaSaksnummer, String korrelasjonsId, String sedType, String dokumentId, Object sed) throws MelosysException;

    void sendSed(String korrelasjonsId, String dokumentId) throws MelosysException;

    void opprettSed(String rinaSaksnummer, String euFormat, String korrelasjonsId, Object SED) throws MelosysException;

    String leggTilVedlegg(String rinaSaksnummer, String dokumentId, String filType) throws MelosysException;

    String hentSed(String rinaSaksnummer, String dokumentId) throws MelosysException;

    String hentBuc(String buc) throws MelosysException;

    List<String> hentTilgjengeligeSedTyper(String rinaSaksnummer) throws MelosysException;

    List<String> bucTypePerSektor() throws MelosysException;

}
