# GitOps for Cloudflare Email

GitOps tool for Cloudflare Email Routing.

Allows you to write a file like this:

```yaml
domains:
- domain: example.org
  account: 7448d8798a4380162d4b56f9b452e2f6f9e24e7a
  zone: a3db5c13ff90a36963278c6a39e4ee3c22e2a436
groups:
  all:
  - hello@example.com
  - goodbye@example.com
forwards:
  hello: hello@example.com
catch-all: catch@example.com
```

and a GitHub Workflow like this:

```yaml
name: Sync Emails

on:
  push:
    branches:
      - 'main'

jobs:
  emails:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v2
      - name: Deploy configuration
        if: github.event_name == 'push'
        id: sync-emails
        uses: GayPizzaSpecifications/gitops-cloudflare-email@main
        with:
          token: ${{ secrets.CLOUDFLARE_API_TOKEN }}
          path: emails.yaml
```

And commit changes to your repo, syncing it to Cloudflare Emails!
