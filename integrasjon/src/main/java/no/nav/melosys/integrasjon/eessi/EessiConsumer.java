package no.nav.melosys.integrasjon.eessi;

import java.util.List;

import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;

public interface EessiConsumer {

    OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto, byte[] vedlegg, BucType bucType, boolean forsøkSend) throws MelosysException;

    void sendAnmodningUnntakSvar(SedDataDto sedDataDto, String rinaSaksnummer) throws MelosysException;

    List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, String status) throws MelosysException;

    List<Institusjon> hentMottakerinstitusjoner(String bucType) throws MelosysException;

    MelosysEessiMelding hentMelosysEessiMeldingFraJournalpostID(String journalpostID) throws MelosysException;

    void lagreSaksrelasjon(SaksrelasjonDto saksrelasjonDto) throws MelosysException;

    List<SaksrelasjonDto> hentSakForRinasaksnummer(String rinaSaksnummer) throws MelosysException;

    byte[] genererSedForhåndsvisning(SedDataDto sedDataDto, SedType sedType) throws MelosysException;
}
