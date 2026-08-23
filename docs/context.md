# Product Context

`fund-helper` is a follow-only watchlist of Turkish TEFAS yatırım fonları. Search funds, follow or unfollow them, and see the latest price and return. Fund detail explains type, risk, and fees in short Turkish, mapped from official TEFAS fields.

It is not a chat consultant, broker, portfolio ledger, or investment adviser. Output is informational. Always treat it as “yatırım tavsiyesi değildir”.

## Durable facts

1. v1 is a follow-only TEFAS watchlist, not a chat consultant.
2. No buy/sell advice; Turkish explanations map official fields only.
3. Local-only; no backend or accounts in v1.
4. Sideload APK on Samsung A23 first; Play Store is later.
5. UI talks only to `FundRepository`; swap `TefasClient` if the phone cannot reach TEFAS.

## Users

- The first user is the developer, sideloading a debug APK onto a Samsung Galaxy A23.
- Later users (Play Store) are people who already look up fonlar on tefas.gov.tr and want a simple follow list on their phone.

## v1 job

- Search by fund code or name (YAT / yatırım fonları first).
- Follow and unfollow. The watchlist is the home screen.
- Show latest price and returns on the watchlist and on fund detail.
- On detail: type, risk, fees, a short Turkish explanation from official fields, and the disclaimer.

## Non-goals (out of v1)

- BES-only focus or BEFAS as the primary universe
- Charts
- Holdings, unit counts, or TL amounts
- Accounts, login, or a backend
- iOS
- LLM / chat consultant
- Buy or sell recommendations
- Play Console listing, privacy policy, and financial-features declaration (after the A23 APK has been used)

## Delivery

- **Now:** debug APK sideloaded on the Samsung A23 over USB.
- **Later:** Play Store. `targetSdk` 36 is already the Gradle target so we do not rebuild only for policy.

## Product rules

- Local-only storage. No login, no server.
- Refresh **only followed funds** (TEFAS is rate-limited; the 2026 site has bot protection).
- Network failure keeps the last cache and the follow list. Never wipe the watchlist because TEFAS failed.
- Copy on screen is Turkish. Explanations are a field mapper, not generated advice.
