# Risk Register

**Reviewed:** 2026-10-10
**Scope:** EchoWave source repository and v1.0.0 Android APK. This is a project risk record, not legal advice or a guarantee of platform decisions.

| Priority | Risk | Possible impact | Current action |
| --- | --- | --- | --- |
| Critical | The app uses undocumented YouTube/YouTube Music InnerTube endpoints and background playback. | Provider access may be blocked or changed. YouTube may take action under applicable terms; Google API credentials or privileges may be affected if relevant API rules apply. | Do not assume this use is authorized. Obtain written permission or replace this integration with permitted sources before further distribution. |
| High | Public client/API identifiers are embedded in the app and visible in source/history. Their ownership/restrictions should not be assumed. | The identifiers can be copied or flagged by automated scanners; if an owned credential is exposed, quota or billing abuse may occur. | Verify their provenance. Restrict or rotate only credentials the project owns, and never add private keys or personal account credentials. |
| Critical | Music, artwork, lyrics, metadata, and stream rights have not been comprehensively cleared. | A rights holder may submit a copyright complaint; GitHub may disable affected content or the release while processing a valid notice. | Confirm licenses and rights for each bundled asset and data source. Do not bundle media without permission. |
| High | Privacy disclosure is not a complete published privacy policy. Local preferences, search history, library data, backup, and third-party requests need review. | Users may lack clear notice/control; app store declarations could be inaccurate. | Complete a data-flow review, decide backup/deletion behavior, publish a real policy/contact channel before store submission. |
| High | Release keystore is locally managed. | Losing it prevents same-identity updates; disclosure may let another party sign malicious updates under that key. | Keep encrypted off-device backups; never upload keystore or signing properties to GitHub. |
| High | Playback and resolver behavior depend on remote services and fragile response formats. | Search/playback can fail after provider-side changes, throttling, or access restrictions. | Monitor issue reports and run the playback regression plan after each release. Do not evade blocks by rotating credentials or identities. |
| Medium | Dependency, transitive license, and vulnerability inventory is incomplete. | Vulnerabilities or notice obligations may go undetected. | Generate and review an SBOM/license report and vulnerability scan for each release. |
| Medium | The v1.0.0 release has not had a full clean-device playback regression. | Device-specific lifecycle, seek, Bluetooth, or background failures may affect users. | Run the physical-device cases in [TEST_PLAN.md](TEST_PLAN.md) before the next release. |

The [YouTube API Services Developer Policies](https://developers.google.com/youtube/terms/developer-policies) state that undocumented APIs must not be used without express permission. EchoWave's current InnerTube integration is not documented as authorized; applicability and the project's rights position have not been resolved. The v1.0.1 release gate therefore remains open.

## What platform action could look like

- **YouTube/Google:** InnerTube endpoints may stop responding, rate-limit requests, or block clients. YouTube's Terms of Service reserve suspension/termination for material or repeated breaches. If YouTube API Services rules apply, the published policy guide describes possible quota reduction, credential revocation, or other action. These are possible outcomes, not a prediction that action will occur.
- **GitHub:** A copyright owner can submit a takedown notice. GitHub may remove or disable repository/release content after its process; its policy also allows account action for repeat infringement. GitHub's Acceptable Use Policies prohibit infringing content. See [GitHub's DMCA policy](https://docs.github.com/en/site-policy/content-removal-policies/dmca-takedown-policy) and [Acceptable Use Policies](https://docs.github.com/en/site-policy/acceptable-use-policies/github-acceptable-use-policies).
- **App stores:** Store review may reject, suspend, or remove an app that violates store rules, provider terms, or rights requirements. EchoWave has not been submitted to Google Play.

An app remaining available, or another project using a similar technique, does not establish permission or predict enforcement. Do not ignore or evade provider restrictions. If contacted, preserve the notice, review the stated issue, and seek qualified legal advice before responding or submitting a counter-notice.

## Owner checklist

- [ ] Resolve the provider authorization question before further public distribution.
- [ ] Review rights and license status for all source code, artwork, lyrics, and bundled assets.
- [ ] Publish an accurate privacy policy and support contact before store submission.
- [ ] Keep release signing keys private and maintain a tested backup.
- [ ] Complete dependency/SBOM/security review and device regression for the next release.
