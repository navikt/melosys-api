# EU Regulation 883/2004 Articles

## Overview

EU Regulation 883/2004 coordinates social security systems across EU/EEA.
Title II (Articles 11-16) contains rules for determining applicable legislation.

## Article 11 - General Rules

### Art. 11.1 - Single Legislation Principle
A person shall be subject to the legislation of a single Member State only.

### Art. 11.3.a - Employed Person
> "A person pursuing an activity as an employed person in a Member State shall be
> subject to the legislation of that Member State"

**Kodeverk:** `FO_883_2004_ART11_3_A`

**Conditions:**
- Person is employed
- Work performed in a single EEA country
- Not posted (art. 12) or multi-state (art. 13)

### Art. 11.3.b - Civil Servant
> "A civil servant shall be subject to the legislation of the Member State
> to which the administration employing them is subject"

**Kodeverk:** `FO_883_2004_ART11_3_B`

**Conditions:**
- Employed as tjenestemann
- In public administration (statsforvaltning)
- The administration is subject to Norwegian legislation

### Art. 11.3.c - Unemployed
> "A person receiving unemployment benefits [...] shall be subject to
> the legislation of the Member State of residence"

**Kodeverk:** `FO_883_2004_ART11_3_C`

### Art. 11.3.d - Military Service
> "A person called up or recalled for service in the armed forces [...]
> shall be subject to the legislation of that Member State"

**Kodeverk:** `FO_883_2004_ART11_3_D`

### Art. 11.3.e - Other Non-Employed
> "Any other person not covered by (a) to (d) shall be subject to
> the legislation of the Member State of residence"

**Kodeverk:** `FO_883_2004_ART11_3_E`

**Use cases:**
- Students
- Ikke-yrkesaktive (non-workers)
- Persons not covered by other articles

### Art. 11.4 - Maritime Workers
> "Work on board a vessel at sea shall be deemed to be work performed in
> the Member State whose flag the vessel is flying"

**Kodeverk:** `FO_883_2004_ART11_4`
**Tilleggsbestemmelse:** `FO_883_2004_ART11_4_1`

**Exception:** If employer established in another country where worker resides,
employer country legislation applies.

### Art. 11.5 - Flight Crew
> "Activity as a flight crew or cabin crew member [...] shall be
> deemed to be activity pursued in the Member State where the home base is located"

**Kodeverk:** `FO_883_2004_ART11_5`

## Article 12 - Posted Workers

### Art. 12.1 - Posted Employee
> "A person who pursues an activity as an employed person in a Member State
> on behalf of an employer which normally carries out its activities there
> and who is posted by that employer to another Member State [...]
> shall continue to be subject to the legislation of the first Member State"

**Kodeverk:** `FO_883_2004_ART12_1`

**Conditions:**
1. Employer has substantial activity in Norway
2. Direct relationship between employer and employee continues
3. Maximum 24 months
4. Person previously subject to Norwegian legislation
5. Not sent to replace another posted person

### Art. 12.2 - Posted Self-Employed
> "A person who normally pursues an activity as a self-employed person
> in a Member State who goes to pursue a similar activity in another
> Member State shall continue to be subject to the legislation of the first State"

**Kodeverk:** `FO_883_2004_ART12_2`

**Conditions:**
1. Self-employed established in Norway
2. Substantial activity in Norway
3. Maximum 24 months
4. Similar activity abroad

## Article 13 - Multi-State Activity

### Art. 13.1 - Multi-State Employee

**Art. 13.1.a** - Substantial part in residence country:
> "If the person pursues a substantial part of their activity in the
> Member State of residence, they shall be subject to the legislation
> of that Member State"

**Art. 13.1.b** - No substantial part in residence:
> "If the person does not pursue a substantial part of their activity
> in the Member State of residence:
> (i) subject to the legislation of the Member State in which the
> registered office or place of business of the employer is situated"

**Kodeverk:** `FO_883_2004_ART13_1`

**Substantial part:** ≥25% of working time or remuneration

### Art. 13.2 - Multi-State Self-Employed
> "A person who normally pursues an activity as a self-employed person
> in two or more Member States shall be subject to:
> (a) the legislation of the Member State of residence if they pursue
> a substantial part of their activity in that Member State; or
> (b) the legislation of the Member State in which the centre of interest
> of their activities is situated"

**Kodeverk:** `FO_883_2004_ART13_2`

### Art. 13.3 - Employee and Self-Employed
> "A person who normally pursues an activity as an employed person and
> an activity as a self-employed person in different Member States
> shall be subject to the legislation of the Member State in which
> they pursue an activity as an employed person"

**Kodeverk:** `FO_883_2004_ART13_3`

### Art. 13.4 - Civil Servant and Other
> "A person who is employed as a civil servant by one Member State and
> who pursues an activity as an employed person [...] in one or more
> other Member States shall be subject to the legislation of the
> Member State to which the administration employing them is subject"

**Kodeverk:** `FO_883_2004_ART13_4`

## Article 14 - Voluntary Insurance

Voluntary insurance provisions - rarely used in Melosys context.

## Article 15 - Contract Agents

> "European Union contract agents may opt to be subject to the legislation
> of the Member State of employment, last insurance, or nationality"

**Kodeverk:** `FO_883_2004_ART15`

## Article 16 - Exceptions

### Art. 16.1 - Agreement on Exception
> "Two or more Member States [...] may provide for exceptions to
> Articles 11 to 15 in the interest of certain persons or categories
> of persons"

**Kodeverk:** `FO_883_2004_ART16_1`

**Process:**
1. Person/employer requests exception
2. Both countries must agree
3. Must be in worker's interest
4. Exception period agreed

**SED Flow:**
- Send A001 (exception request)
- Receive A002 (response)

### Art. 16.2 - Pensioner Exception
> "A person receiving one or more pensions under the legislation of
> one or more Member States and who resides in another Member State
> may at their request be exempted..."

**Kodeverk:** `FO_883_2004_ART16_2`

## Implementation Regulation 987/2009

### Art. 14.11 - Business Outside EEA
When employer has registered office/place of business outside EEA area:
- Employee residence country determines legislation
- Used via `FO_987_2009_ART14_11`

### Art. 18 - Exception Procedure
Specifies the procedure for art. 16 exceptions:
- Application to desired country's authority
- Communication between institutions
- Time limits for response

## Related Confluence Pages

- [BUCer](https://confluence.adeo.no/spaces/TEESSI/pages/320014887)
- [Vilkår for artikkel 11](https://confluence.adeo.no/spaces/TEESSI/pages/292390226)
- [Vilkår for artikkel 12](https://confluence.adeo.no/spaces/TEESSI/pages/265187800)
- [Vilkår for artikkel 13](https://confluence.adeo.no/spaces/TEESSI/pages/261940457)
- [Vilkår for artikkel 16](https://confluence.adeo.no/spaces/TEESSI/pages/313350257)
