# Deploying

The app is a standard Spring Boot jar in a Docker image (see `Dockerfile`).
It keeps no database — reservations and the cart live in memory, so a
redeploy or restart clears them (fine for this site's traffic; worth
knowing before you pick a host that restarts containers often).

## Required environment variables

Set these on whichever host you deploy to — they're never baked into the
image. See `.env.example` for the same list with explanations.

| Variable              | Required | Notes                                                             |
|------------------------|----------|--------------------------------------------------------------------|
| `GOOGLE_CALENDAR_ID`   | yes      | Public Google Calendar ID                                         |
| `GOOGLE_API_KEY`       | yes      | Google API key with Calendar API enabled                          |
| `LIGA_WHATSAPP_NUMBER` | yes      | WhatsApp number orders/reservations are sent to, digits only       |
| `LIGA_ADMIN_TOKEN`     | no       | Shared secret for `/prestamo-mazos/reservas?token=...`; leave unset to disable that page |
| `MOXFIELD_SYNC_TOKEN`  | no*      | Shared secret for `POST /internal/moxfield-decks-sync` — see "Keeping the deck list live" below |
| `PORT` or `SERVER_PORT`| no       | Which port the container listens on; most hosts set `PORT` for you |

\* Required in practice on hosts Cloudflare blocks by IP (confirmed for Render) — otherwise `/mazos` and the deck reservation flow get stuck on the bundled snapshot in `DeckCatalog`, which can show a deck that's since been made private on Moxfield.

## Keeping the deck list live (Moxfield + Cloudflare)

`MoxfieldDeckService` fetches the league's decks straight from Moxfield on
every request, so a deck set private there stops showing up here too. On
most networks that works out of the box (it shells out to `curl`, since
Moxfield's API blocks every plain Java HTTP client's TLS fingerprint — see
that class's Javadoc). On some hosts it still fails, because Cloudflare
separately blocks known hosting-provider IP ranges regardless of client —
confirmed on Render, where `curl` itself gets rejected with exit code 22.

If that's the case for your host too (check its logs for "Moxfield fetch
failed" / "curl exited with"), set it up to receive scheduled pushes
instead of fetching directly:

1. Deploy with `MOXFIELD_SYNC_TOKEN` set to a random secret (e.g.
   `openssl rand -hex 32`).
2. In the GitHub repo, add two Actions secrets (Settings → Secrets and
   variables → Actions): `MOXFIELD_SYNC_URL` set to
   `https://<your-host>/internal/moxfield-decks-sync`, and
   `MOXFIELD_SYNC_TOKEN` set to the same value as step 1.
3. `.github/workflows/sync-moxfield-decks.yml` then pushes a fresh deck
   list from GitHub's own runners every 15 minutes.

GitHub auto-disables a scheduled workflow after 60 days with no repo
activity (easy to hit in the off-season) — a push, or "Enable workflow" on
the Actions tab, turns it back on.

## Building and testing the image locally

```bash
docker build -t lpm-app .
docker run --rm -p 8080:8080 \
  -e GOOGLE_CALENDAR_ID=your-calendar-id@group.calendar.google.com \
  -e GOOGLE_API_KEY=your-api-key \
  -e LIGA_WHATSAPP_NUMBER=59800000000 \
  -e LIGA_ADMIN_TOKEN=some-secret \
  lpm-app
```

Then open http://localhost:8080.

## Free hosting options that run a Dockerfile as-is

**Render** (recommended starting point) — connect the GitHub repo, pick
"Web Service" → "Docker" (it finds the `Dockerfile` automatically), add the
environment variables above in its dashboard, deploy. Free web services
sleep after ~15 minutes idle and take a short while to wake up on the next
request — fine for a low-traffic league site.

Other Docker-friendly free tiers, if Render doesn't suit:
- **Fly.io** — generous free allowance, needs a card on file even for the
  free tier, a bit more CLI-driven (`fly launch`/`fly deploy`).
- **Google Cloud Run** — pay-per-use with a large free monthly quota,
  scales to zero, needs a Google Cloud billing account set up (free tier
  covers a hobby site's usage).
- **Koyeb** — has a small always-on free instance, similar dashboard-driven
  flow to Render.

Free-tier terms change often — check current limits before committing to
one.
