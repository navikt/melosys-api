package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

class VedtaksmetadataFiksAvvist(melding: String) : RuntimeException(melding)

/**
 * Datafiks for MELOSYS-8174: setter inn manglende rader i `vedtak_metadata` for avsluttede
 * behandlinger som blokkerer skattepliktig årsavregning.
 *
 * `ÅrsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning` kaller
 * `hentVedtakMetadata()` ubetinget i filter/sortering, så én rad uten vedtaksmetadata velter hele
 * saken med «vedtakMetadata er påkrevd for Behandlingsresultat» før den blir faglig vurdert.
 *
 * Native SQL, ikke JPA: `registrert_av`/`endret_av` må bli [PATCH_MARKØR] (JPA-auditing ville satt
 * saksbehandler/«MELOSYS»), og `vedtak_dato` skal være `behandlingsresultat.endret_dato` som
 * tilnærming, ikke `Instant.now()` slik `Behandlingsresultat.settVedtakMetadata` gjør.
 */
@Component
class VedtaksmetadataFiksService {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Transactional(readOnly = true)
    fun forhåndsvis(
        saksnummer: List<String>,
        tillatSorteringsendring: List<String> = emptyList(),
    ): VedtaksmetadataFiksResultat {
        val kandidater = hentKandidater(saksnummer)
        val påvirkning = sorteringspåvirkning(saksnummer)
        return VedtaksmetadataFiksResultat(
            skarp = false,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallRaderInnsatt = 0,
            rader = kandidater,
            utenMetadataPerSak = tellUtenMetadata(saksnummer),
            ukjentBehType = kandidater.filterNot { it.behType in KJENTE_BEH_TYPER }.map { it.behandlingsresultatId },
            sorteringspåvirkning = påvirkning,
            saksnummerUtenKandidater = finnSaksnummerUtenKandidater(saksnummer, kandidater),
            godkjenningerUtenTreff = finnGodkjenningerUtenTreff(tillatSorteringsendring, påvirkning),
        )
    }

    @Transactional
    fun utfør(
        saksnummer: List<String>,
        maksAntallRader: Int,
        tillatSorteringsendring: List<String> = emptyList(),
    ): VedtaksmetadataFiksResultat {
        if (saksnummer.isEmpty()) throw VedtaksmetadataFiksAvvist("Skarp kjøring krever eksplisitt saksnummer-liste")

        val kandidater = hentKandidater(saksnummer)
        if (kandidater.size > maksAntallRader) {
            throw VedtaksmetadataFiksAvvist(
                "Fant ${kandidater.size} rader, men maksAntallRader er $maksAntallRader. " +
                    "Kjør preview først, og hev maksAntallRader eksplisitt hvis tallet er riktig."
            )
        }
        // Etter maksAntallRader-kontrollen, så den mer presise meldingen vinner i det vanlige tilfellet.
        if (kandidater.size > MAKS_UTTRYKK_I_IN) {
            throw VedtaksmetadataFiksAvvist(
                "Fant ${kandidater.size} kandidater, men INSERT-en binder dem i en IN-liste, og Oracle " +
                    "tar maks $MAKS_UTTRYKK_I_IN uttrykk (ORA-01795). Del kjøringen opp i flere kall med " +
                    "færre saksnummer om gangen."
            )
        }
        val ukjente = kandidater.filterNot { it.behType in KJENTE_BEH_TYPER }
        if (ukjente.isNotEmpty()) {
            throw VedtaksmetadataFiksAvvist(
                "Kandidater med ukjent beh_type (vedtakstype kan ikke utledes trygt): " +
                    ukjente.joinToString { "${it.behandlingsresultatId}=${it.behType}" }
            )
        }

        // Må måles før INSERT-en — etterpå er patch-radene ekte rader, og «før»-bildet kan ikke gjenskapes.
        val påvirkning = sorteringspåvirkning(saksnummer)
        val blirNyeste = påvirkning.filter { it.trengerGodkjenning }
        val ikkeGodkjent = blirNyeste.filterNot { it.saksnummer in tillatSorteringsendring }
        val godkjenningerUtenTreff = finnGodkjenningerUtenTreff(tillatSorteringsendring, påvirkning)
        if (ikkeGodkjent.isNotEmpty()) {
            throw VedtaksmetadataFiksAvvist(
                "Den tilnærmede vedtaksdatoen ville blitt den nyeste i saken for " +
                    ikkeGodkjent.joinToString { "${it.saksnummer} (${it.nyesteKandidatDato} mot ${it.nyesteSammenlignbareDato})" } +
                    ". Da endres hvilken behandling årsavregningen henter avgiftsgrunnlaget fra. " +
                    "Sett riktig vedtaksdato manuelt, eller legg saksnummeret i tillatSorteringsendring " +
                    "hvis endringen er vurdert og ønsket." +
                    if (godkjenningerUtenTreff.isEmpty()) "" else
                        " Merk: disse oppføringene i tillatSorteringsendring traff ingen sak som trengte godkjenning: $godkjenningerUtenTreff."
            )
        }

        blirNyeste.forEach {
            log.warn {
                "Datafiks $PATCH_MARKØR: sak ${it.saksnummer} patches selv om behandlingsresultat " +
                    "${it.nyesteKandidatId} (${it.nyesteKandidatDato}) blir nyeste i saken foran " +
                    "${it.nyesteSammenlignbareId} (${it.nyesteSammenlignbareDato}) — godkjent i " +
                    "tillatSorteringsendring."
            }
        }

        påvirkning.filter { it.patchenBlirNyesteIHeleSaken && !it.trengerGodkjenning }.forEach {
            log.info {
                "Datafiks $PATCH_MARKØR: sak ${it.saksnummer} — behandlingsresultat ${it.nyesteKandidatId} " +
                    "(${it.nyesteKandidatDato}) blir nyeste rad i saken, men fortrenger ingen dato som " +
                    "kan være ekte (nyeste sammenlignbare: ${it.nyesteSammenlignbareDato ?: "ingen"})."
            }
        }

        log.info {
            "Datafiks $PATCH_MARKØR: setter inn ${kandidater.size} rader i vedtak_metadata for saker $saksnummer " +
                "(behandlingsresultat ${kandidater.map { it.behandlingsresultatId }})"
        }

        // Bundet til ID-ene kontrollene så, ikke til kandidatfilteret på nytt: READ COMMITTED kunne
        // ellers sluppet inn en kandidat som dukket opp etter kontrollene.
        val antallInnsatt = entityManager.createNativeQuery(INSERT_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .setParameter("ider", kandidater.map { it.behandlingsresultatId })
            .setParameter("markoer", PATCH_MARKØR)
            .executeUpdate()

        val avvik = antallInnsatt != kandidater.size
        if (avvik) {
            log.warn {
                "Datafiks $PATCH_MARKØR: satte inn $antallInnsatt rader, men forhåndsvisningen viste " +
                    "${kandidater.size}. Noe har endret dataene under kjøringen — kontroller før du går videre."
            }
        } else {
            log.info { "Datafiks $PATCH_MARKØR: satte inn $antallInnsatt rader" }
        }

        return VedtaksmetadataFiksResultat(
            skarp = true,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallRaderInnsatt = antallInnsatt,
            rader = kandidater,
            utenMetadataPerSak = tellUtenMetadata(saksnummer),
            avvik = avvik,
            sorteringspåvirkning = påvirkning,
            saksnummerUtenKandidater = finnSaksnummerUtenKandidater(saksnummer, kandidater),
            godkjenningerUtenTreff = godkjenningerUtenTreff,
        )
    }

    @Transactional
    fun angre(saksnummer: List<String>, skarp: Boolean, bekreftAlle: Boolean = false): VedtaksmetadataAngreResultat {
        if (skarp && saksnummer.isEmpty() && !bekreftAlle) {
            throw VedtaksmetadataFiksAvvist(
                "Skarp angre uten saksnummer sletter alle rader merket $PATCH_MARKØR, også fra tidligere " +
                    "kjøringer. Angi saksnummer, eller send bekreftAlle=true hvis du faktisk vil rulle tilbake alt."
            )
        }

        val kandidater = hentAngreKandidater(saksnummer)
        val kanIkkeAngres = tellSomIkkeKanAngres(saksnummer)

        if (!skarp) {
            return VedtaksmetadataAngreResultat(
                skarp = false,
                saksnummer = saksnummer,
                antallRaderFunnet = kandidater.size,
                antallSlettet = 0,
                rader = kandidater,
                antallSomIkkeKanAngres = kanIkkeAngres,
            )
        }

        log.info {
            "Datafiks $PATCH_MARKØR: ruller tilbake ${kandidater.size} rader " +
                "(scope=${saksnummer.ifEmpty { listOf("ALLE") }}, urørt fordi de er endret etterpå: $kanIkkeAngres)"
        }

        val sql = if (saksnummer.isEmpty()) ANGRE_SQL else ANGRE_SQL + ANGRE_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)
        val antallSlettet = query.executeUpdate()

        log.info { "Datafiks $PATCH_MARKØR: slettet $antallSlettet rader" }

        return VedtaksmetadataAngreResultat(
            skarp = true,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallSlettet = antallSlettet,
            rader = kandidater,
            antallSomIkkeKanAngres = kanIkkeAngres,
            avvik = antallSlettet != kandidater.size,
        )
    }

    private fun finnSaksnummerUtenKandidater(saksnummer: List<String>, kandidater: List<VedtaksmetadataFiksRad>): List<String> {
        val truffet = kandidater.map { it.saksnummer }.toSet()
        return saksnummer.filterNot { it in truffet }
    }

    private fun finnGodkjenningerUtenTreff(
        tillatSorteringsendring: List<String>,
        påvirkning: List<SorteringspåvirkningRad>,
    ): List<String> {
        val trengerGodkjenning = påvirkning.filter { it.trengerGodkjenning }.map { it.saksnummer }.toSet()
        return tillatSorteringsendring.distinct().filterNot { it in trengerGodkjenning }
    }

    @Suppress("UNCHECKED_CAST")
    private fun hentKandidater(saksnummer: List<String>): List<VedtaksmetadataFiksRad> {
        if (saksnummer.isEmpty()) return emptyList()

        val rader = entityManager.createNativeQuery(KANDIDAT_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .resultList as List<Array<Any?>>

        return rader.map { rad ->
            VedtaksmetadataFiksRad(
                saksnummer = rad[0] as String,
                behandlingsresultatId = (rad[1] as Number).toLong(),
                behType = rad[2] as String?,
                resultatType = rad[3] as String?,
                blirVedtakDato = rad[4] as String?,
                blirKlagefrist = rad[5] as String?,
                blirVedtakType = rad[6] as String,
            )
        }
    }

    /**
     * `ÅrsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning` sorterer på `vedtak_dato`
     * og henter avgiftsgrunnlaget fra den siste. `endret_dato` er `@LastModifiedDate` og alltid ≥ den
     * ekte vedtaksdatoen, så patchede rader ser systematisk nyere ut enn de er.
     *
     * Kontrollen sammenligner derfor kun mot datoer som kan være ekte ([Datoopphav.EKTE] og
     * [Datoopphav.PATCHET_ENDRET]); våre egne urørte rader rapporteres, men styrer ingenting.
     *
     * Ingen simulering av avgiftsgrunnlaget: den faktiske utvelgelsen filtrerer også på år og
     * periodeoverlapp, mens dette er global maks mot global maks. `true` er en pålitelig grunn til å
     * stoppe; `false` er ingen garanti. Derfor listes alle datoene.
     */
    private fun sorteringspåvirkning(saksnummer: List<String>): List<SorteringspåvirkningRad> {
        if (saksnummer.isEmpty()) return emptyList()

        val eksisterendePerSak = raderPerSak(EKSISTERENDE_NYESTE_SQL, saksnummer, medMarkoer = true)
        val kandidaterPerSak = raderPerSak(PATCH_NYESTE_SQL, saksnummer, medMarkoer = false)

        return kandidaterPerSak.keys.sorted().map { sak ->
            val eksisterende = eksisterendePerSak[sak].orEmpty()
            val nyesteKandidat = kandidaterPerSak.getValue(sak).first()

            fun datoer(opphav: Datoopphav) =
                eksisterende.filter { it.opphav == opphav }.mapNotNull { it.visning }

            val nyesteSammenlignbare = eksisterende
                .filter { it.opphav != Datoopphav.PATCHET_URØRT && it.sortering != null }
                .maxByOrNull { it.sortering!! }

            SorteringspåvirkningRad(
                saksnummer = sak,
                nyesteKandidatId = nyesteKandidat.behandlingsresultatId,
                nyesteKandidatDato = nyesteKandidat.visning,
                nyesteSammenlignbareId = nyesteSammenlignbare?.behandlingsresultatId,
                nyesteSammenlignbareDato = nyesteSammenlignbare?.visning,
                // >= og ikke >: ved likt tidsstempel er utfallet i den ekte sorteringen vilkårlig, og det skal flagges.
                trengerGodkjenning = nyesteSammenlignbare != null &&
                    requireNotNull(nyesteKandidat.sortering) >= nyesteSammenlignbare.sortering!!,
                patchenBlirNyesteIHeleSaken = eksisterende
                    .mapNotNull { it.sortering }
                    .maxOrNull()
                    ?.let { requireNotNull(nyesteKandidat.sortering) >= it } ?: true,
                ekteDatoer = datoer(Datoopphav.EKTE),
                usikreDatoer = datoer(Datoopphav.PATCHET_ENDRET),
                patchedeDatoer = datoer(Datoopphav.PATCHET_URØRT),
                antallUdaterteRader = eksisterende.count { it.sortering == null },
            )
        }
    }

    /** Nyest først — `.first()` i [sorteringspåvirkning] og datolistene i rapporten avhenger av `ORDER BY` i SQL-ene. */
    @Suppress("UNCHECKED_CAST")
    private fun raderPerSak(sql: String, saksnummer: List<String>, medMarkoer: Boolean): Map<String, List<SortertRad>> {
        val query = entityManager.createNativeQuery(sql)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
        if (medMarkoer) query.setParameter("markoer", PATCH_MARKØR)

        return (query.resultList as List<Array<Any?>>)
            .map { rad ->
                SortertRad(
                    saksnummer = rad[0] as String,
                    behandlingsresultatId = (rad[1] as Number).toLong(),
                    visning = rad[2] as String?,
                    sortering = rad[3] as String?,
                    opphav = Datoopphav.av((rad[4] as Number).toInt()),
                )
            }
            .groupBy { it.saksnummer }
    }

    /**
     * Utledet av auditfeltene: `registrert_av` (`@CreatedBy`) sier pålitelig om fiksen laget raden;
     * `endret_av` (`@LastModifiedBy`) flyttes av *enhver* senere skriving, så [PATCHET_ENDRET] kan,
     * men må ikke, være korrigert til en ekte dato — og regnes derfor konservativt som mulig ekte.
     */
    private enum class Datoopphav {
        EKTE,
        PATCHET_URØRT,
        PATCHET_ENDRET,
        ;

        companion object {
            fun av(kode: Int) = when (kode) {
                0 -> EKTE
                1 -> PATCHET_URØRT
                else -> PATCHET_ENDRET
            }
        }
    }

    /** [visning] er lesbar for operatøren; [sortering] har mikrosekunder og er den som sammenlignes. */
    private data class SortertRad(
        val saksnummer: String,
        val behandlingsresultatId: Long,
        val visning: String?,
        val sortering: String?,
        val opphav: Datoopphav,
    )

    private fun hentAngreKandidater(saksnummer: List<String>): List<VedtaksmetadataAngreRad> {
        val sql = if (saksnummer.isEmpty()) ANGRE_KANDIDAT_SQL else ANGRE_KANDIDAT_SQL + ANGRE_KANDIDAT_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)

        return (query.resultList as List<Array<Any?>>).map { rad ->
            VedtaksmetadataAngreRad(
                saksnummer = rad[0] as String,
                behandlingsresultatId = (rad[1] as Number).toLong(),
                vedtakDato = rad[2] as String?,
                vedtakType = rad[3] as String?,
            )
        }
    }

    private fun tellSomIkkeKanAngres(saksnummer: List<String>): Int {
        val sql = if (saksnummer.isEmpty()) KAN_IKKE_ANGRES_SQL else KAN_IKKE_ANGRES_SQL + ANGRE_KANDIDAT_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)
        return (query.singleResult as Number).toInt()
    }

    @Suppress("UNCHECKED_CAST")
    private fun tellUtenMetadata(saksnummer: List<String>): Map<String, Int> {
        if (saksnummer.isEmpty()) return emptyMap()

        val rader = entityManager.createNativeQuery(ETTERKONTROLL_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .resultList as List<Array<Any?>>

        return rader.associate { rad -> (rad[0] as String) to (rad[1] as Number).toInt() }
    }

    companion object {
        /** Står i rader som allerede er satt inn i prod — verdien kan ikke endres. */
        const val PATCH_MARKØR = "MELOSYS-8174-PATCH"

        /**
         * Saker fag har bekreftet har trygdeavgift til Nav for 2024. MEL-409394 er bevisst utelatt:
         * avklart separat (år-løs årsavregning, ikke manglende vedtaksmetadata).
         */
        val STANDARD_SAKER = listOf("MEL-448193", "MEL-545776", "MEL-632908")

        const val MAKS_ANTALL_SAKER = 25

        /** Oracles tak i en IN-liste (ORA-01795). `IN (:ider)` i INSERT_SQL er like lang som antall kandidatrader, ikke saker. */
        const val MAKS_UTTRYKK_I_IN = 1000

        const val STANDARD_MAKS_ANTALL_RADER = 10

        /**
         * Behandlingstyper vi trygt kan utlede vedtakstype for: FØRSTEGANG gir FØRSTEGANGSVEDTAK,
         * NY_VURDERING og ENDRET_PERIODE gir ENDRINGSVEDTAK (samme mønster som Flyway-patchen V7.6_04).
         * Andre typer — KLAGE, ANKE, SATSENDRING … — har egne vedtakstyper i kodeverket og avvises.
         */
        val KJENTE_BEH_TYPER = listOf("FØRSTEGANG", "NY_VURDERING", "ENDRET_PERIODE")

        /** Resultattypene ÅrsavregningService slår opp på — det er kun disse som kan velte en sak. */
        private val RESULTATTYPER = listOf("FASTSATT_TRYGDEAVGIFT", "FASTSATT_LOVVALGSLAND", "MEDLEM_I_FOLKETRYGDEN")

        private const val KANDIDAT_WHERE = """
            WHERE b.status = 'AVSLUTTET'
              AND br.resultat_type IN (:resultattyper)
              AND NOT EXISTS (SELECT 1 FROM vedtak_metadata vm WHERE vm.behandlingsresultat_id = br.behandling_id)
              AND b.saksnummer IN (:saksnummer)
        """

        /**
         * Samme utledning som Flyway-patchen V7.6_04. beh_type bestemmer strengt tatt ikke vedtakstypen
         * (den sendes separat i FattVedtakRequest), men feltet leses ikke av ÅrsavregningService, og NULL
         * er verre: A1TypeUtstedelse.av() switcher på enumen uten null-gren.
         */
        private const val VEDTAK_TYPE_CASE =
            "CASE WHEN b.beh_type = 'FØRSTEGANG' THEN 'FØRSTEGANGSVEDTAK' ELSE 'ENDRINGSVEDTAK' END"

        /** Klagefrist +42 dager følger Flyway-patchen V7.6_04. Datoene formateres i SQL for å slippe JDBC-typemapping. */
        private const val KANDIDAT_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   b.beh_type,
                   br.resultat_type,
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(TRUNC(CAST(br.endret_dato AS DATE)) + 42, 'YYYY-MM-DD'),
                   $VEDTAK_TYPE_CASE
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            ORDER BY b.saksnummer, br.endret_dato
        """

        private const val INSERT_SQL = """
            INSERT INTO vedtak_metadata
                (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type,
                 registrert_dato, endret_dato, registrert_av, endret_av)
            SELECT br.behandling_id,
                   br.endret_dato,
                   TRUNC(CAST(br.endret_dato AS DATE)) + 42,
                   $VEDTAK_TYPE_CASE,
                   SYSTIMESTAMP, SYSTIMESTAMP, :markoer, :markoer
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
              AND br.behandling_id IN (:ider)
        """

        /**
         * Siste kolonne er koden [Datoopphav.av] leser. Mikrosekund-kolonnen trengs fordi ÅrsavregningService
         * sammenligner TIMESTAMP i full oppløsning. `NULLS LAST` fordi Oracle ellers legger NULL først i DESC.
         */
        private const val EKSISTERENDE_NYESTE_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS.FF6'),
                   CASE
                       WHEN vm.registrert_av IS NULL OR vm.registrert_av <> :markoer THEN 0
                       WHEN vm.endret_av = :markoer THEN 1
                       ELSE 2
                   END
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
            WHERE b.status = 'AVSLUTTET'
              AND br.resultat_type IN (:resultattyper)
              AND b.saksnummer IN (:saksnummer)
            ORDER BY b.saksnummer, vm.vedtak_dato DESC NULLS LAST, br.behandling_id DESC
        """

        private const val PATCH_NYESTE_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS.FF6'),
                   0
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            ORDER BY b.saksnummer, br.endret_dato DESC, br.behandling_id DESC
        """

        private const val ETTERKONTROLL_SQL = """
            SELECT b.saksnummer, COUNT(*)
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            GROUP BY b.saksnummer
        """

        private const val ANGRE_KANDIDAT_SQL = """
            SELECT b.saksnummer,
                   vm.behandlingsresultat_id,
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   vm.vedtak_type
            FROM vedtak_metadata vm
            JOIN behandling b ON b.id = vm.behandlingsresultat_id
            WHERE vm.registrert_av = :markoer
              AND vm.endret_av = :markoer
        """
        private const val ANGRE_KANDIDAT_SAKSFILTER = " AND b.saksnummer IN (:saksnummer)"

        /**
         * `IS NULL`-grenen er nødvendig: både `=` og `<>` er UNKNOWN mot NULL i Oracle, så en rad med tømt
         * endret_av ville ellers falt ut av både angre-kandidatene og denne tellingen.
         */
        private const val KAN_IKKE_ANGRES_SQL = """
            SELECT COUNT(*)
            FROM vedtak_metadata vm
            JOIN behandling b ON b.id = vm.behandlingsresultat_id
            WHERE vm.registrert_av = :markoer
              AND (vm.endret_av IS NULL OR vm.endret_av <> :markoer)
        """

        private const val ANGRE_SQL = """
            DELETE FROM vedtak_metadata
            WHERE registrert_av = :markoer
              AND endret_av = :markoer
        """
        private const val ANGRE_SAKSFILTER = """
              AND behandlingsresultat_id IN (SELECT b.id FROM behandling b WHERE b.saksnummer IN (:saksnummer))
        """
    }
}

data class VedtaksmetadataFiksRad(
    val saksnummer: String,
    val behandlingsresultatId: Long,
    val behType: String?,
    val resultatType: String?,
    val blirVedtakDato: String?,
    val blirKlagefrist: String?,
    val blirVedtakType: String,
)

/**
 * Hva patchen gjør med vedtaksdato-sorteringen i én sak. [trengerGodkjenning] er det eneste feltet
 * som blokkerer skarp kjøring; resten er rapport. Datolistene dekker de radene ÅrsavregningService
 * faktisk ser (avsluttede behandlinger med resultattype i RESULTATTYPER).
 */
data class SorteringspåvirkningRad(
    val saksnummer: String,
    /** Nyeste kandidat i saken, med datoen patchen gir den. */
    val nyesteKandidatId: Long,
    val nyesteKandidatDato: String?,
    /** Nyeste rad som *kan* være et ekte vedtak ([ekteDatoer] + [usikreDatoer]). Null = ingenting å fortrenge. */
    val nyesteSammenlignbareId: Long?,
    val nyesteSammenlignbareDato: String?,
    /**
     * Patchen legger seg på eller over [nyesteSammenlignbareDato] og kan fortrenge et ekte vedtak.
     * Blokkerer skarp kjøring med mindre saksnummeret står i `tillatSorteringsendring`.
     */
    val trengerGodkjenning: Boolean,
    /** Patchen blir nyeste rad mot ALLE rader, også våre egne tidligere. Blokkerer ikke, men logges. */
    val patchenBlirNyesteIHeleSaken: Boolean,
    /**
     * Rader fiksen ikke har laget. Ingen garanti for ekthet: Flyway-patchen V7.6_04 skrev samme
     * tilnærmede dato uten markør, så rader derfra havner her. Å telle dem med er den konservative retningen.
     */
    val ekteDatoer: List<String> = emptyList(),
    /** Rader fiksen laget som noen har skrevet til siden — kan være korrigert, vi vet ikke. Regnes som mulig ekte. */
    val usikreDatoer: List<String> = emptyList(),
    /** Rader fiksen laget og ingen har rørt. Styrer ingenting. */
    val patchedeDatoer: List<String> = emptyList(),
    /** Rader uten `vedtak_dato`. Skiller «saken har udaterte rader» fra «saken har ingen vedtaksmetadata». */
    val antallUdaterteRader: Int = 0,
)

data class VedtaksmetadataFiksResultat(
    val skarp: Boolean,
    val saksnummer: List<String>,
    val antallRaderFunnet: Int,
    val antallRaderInnsatt: Int,
    /** Kandidatene før innsetting. Ved `avvik = true` er dette ikke det som ble skrevet. */
    val rader: List<VedtaksmetadataFiksRad>,
    /** Rader uten vedtaksmetadata per sak, etter kjøringen. Tom = ingen igjen. */
    val utenMetadataPerSak: Map<String, Int>,
    /** Blokkerer skarp kjøring. */
    val ukjentBehType: List<Long> = emptyList(),
    /** Skrivefeil, eller sak uten defekte rader. Blokkerer ikke. */
    val saksnummerUtenKandidater: List<String> = emptyList(),
    /** Oppføringer i `tillatSorteringsendring` uten sak som trengte godkjenning — typisk skrivefeil. Blokkerer ikke. */
    val godkjenningerUtenTreff: List<String> = emptyList(),
    /** Antall innsatte rader stemmer ikke med forhåndsvisningen. */
    val avvik: Boolean = false,
    val sorteringspåvirkning: List<SorteringspåvirkningRad> = emptyList(),
    val markoer: String = VedtaksmetadataFiksService.PATCH_MARKØR,
    val angreEndepunkt: String = "POST /admin/aarsavregninger/saker/skattepliktige/vedtaksmetadata-fiks/angre",
)

data class VedtaksmetadataAngreRad(
    val saksnummer: String,
    val behandlingsresultatId: Long,
    val vedtakDato: String?,
    val vedtakType: String?,
)

data class VedtaksmetadataAngreResultat(
    val skarp: Boolean,
    /** Tom liste = alle markerte rader, uansett sak. */
    val saksnummer: List<String>,
    val antallRaderFunnet: Int,
    val antallSlettet: Int,
    val rader: List<VedtaksmetadataAngreRad>,
    /** Patch-rader som er endret etterpå — røres aldri. */
    val antallSomIkkeKanAngres: Int,
    val avvik: Boolean = false,
    val markoer: String = VedtaksmetadataFiksService.PATCH_MARKØR,
)
