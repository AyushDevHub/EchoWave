# Security Policy

EchoWave is a public open-source Android application. Security reports are welcome for the app, its build/release workflow, and repository configuration.

## Supported versions

| Version | Security fixes |
| --- | --- |
| Latest GitHub release | Best effort |
| Older releases | Not guaranteed; upgrade to the latest release when possible |

## Report a vulnerability

Use GitHub's **Report a vulnerability** feature in the repository's Security tab if it is enabled. If private reporting is unavailable, contact the maintainer through the GitHub profile rather than posting exploit details in a public issue. Do not include credentials, personal data, cookies, PO tokens, signed media URLs, or complete request headers in a report.

Please include the affected version/commit, Android version and device when relevant, impact, and safe reproduction steps. The project has no guaranteed response time or bug-bounty program. Please allow time for investigation and a fix before public disclosure.

## Credential handling

Never commit release keystores, signing properties, account cookies, OAuth tokens, private credentials, or personal data. If a credential is exposed, revoke or rotate it with its owner immediately, remove it from active history where appropriate, and report the exposure privately. Rewriting Git history does not invalidate a leaked credential.

The public client identifiers used by the current InnerTube integration are visible in the app and source; they are not user passwords. Their visibility does not imply permission to use the underlying service.
