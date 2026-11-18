# syntax=docker/dockerfile:1
# Three stages, so the final image holds only what the server needs to run: no source code,
# no build tools and no dev dependencies.

# 1. deps: install node_modules from the lock file. Its own stage so Docker reuses the layer
#    until package.json or package-lock.json changes.
FROM node:26-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# 2. build: compile the app. `output: "standalone"` in next.config.ts makes Next.js write a
#    self-contained server to .next/standalone.
FROM node:26-alpine AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# The build validates the environment, and the two secrets have no defaults. These placeholders
# exist only for this one command: they are not ENV, so they are not in the image, and the real
# values come from the Kubernetes Secret when the container starts.
RUN AUTH_SECRET=build-placeholder-not-a-real-secret-0123456789 \
    AUTH_KEYCLOAK_SECRET=build-placeholder-not-a-real-client-secret \
    npm run build

# 3. run: the standalone server plus the static files it serves.
FROM node:26-alpine AS run
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3001 \
    HOSTNAME=0.0.0.0
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
# Do not run as root inside the container.
USER node
EXPOSE 3001
CMD ["node", "server.js"]
