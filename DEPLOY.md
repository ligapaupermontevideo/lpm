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
| `PORT` or `SERVER_PORT`| no       | Which port the container listens on; most hosts set `PORT` for you |

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
