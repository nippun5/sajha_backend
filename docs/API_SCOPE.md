# Phase 3C contract → Phase 4A endpoint coverage

Source of truth: `sajha_phase3c_api_contract/openapi.yaml` supplied in the prior phase.

| Method | Path | Phase 4A |
|---|---|---|
| GET | `/api/v1/health` | Implemented |
| POST | `/api/v1/auth/register` | Implemented |
| POST | `/api/v1/auth/login` | Implemented |
| POST | `/api/v1/auth/refresh` | Implemented |
| POST | `/api/v1/auth/logout` | Implemented |
| GET | `/api/v1/me` | Implemented |
| POST | `/api/v1/me/phone/challenges` | Implemented; needs Twilio secrets |
| POST | `/api/v1/me/phone/verify` | Implemented; needs Twilio secrets |
| POST | `/api/v1/auth/social/exchange` | **Not implemented — blocked** |
| POST | `/api/v1/auth/password/forgot`, `/reset` | **Not implemented — blocked** |
| Any | `/api/v1/admin/**` | **Not implemented — deliberately denied** |
| Any | `/api/v1/marketplace/**`, `/api/v1/business-requests/**`, `/api/v1/businesses/**`, `/api/v1/reviews/**`, `/api/v1/media` | **Not implemented — deliberately denied** |

Response account conforms to Phase 3C `Account`: `id`, `name`, `email`, `phone`, `phoneVerified`, `verifiedContactPhones`, `roles`. A registered user is **never** granted `ADMIN` by client input. Identifiers are UUID strings, timestamps in UTC. Errors include `{error:{code,message,details,traceId}}` for standard API errors.

Registered phone is mandatory, normalized E.164, but **verification is separate**. Pending posting routes must reject users whose registered phone hasn't been verified; unimplemented posting routes reject everyone already.

## DB migration scope

`app_users`, `user_roles`, `verified_contact_phones`, `refresh_sessions`, `phone_challenges`. These are the identity subset of the proposed Phase 3C relational model. Marketplace/business/review migrations will be added **after** the actual API implementation; no placeholder tables are needed yet.
