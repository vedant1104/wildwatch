FROM node:20-alpine AS builder
WORKDIR /app

# Install deps (cacheable)
COPY package.json package-lock.json ./
RUN npm install --production=false

# Copy source and build
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app

# Copy production files
COPY package.json package-lock.json ./
COPY --from=builder /app/.next .next
COPY --from=builder /app/public ./public
COPY --from=builder /app/next.config.js ./next.config.js

# Install production deps only
RUN npm install --omit=dev

EXPOSE 3000
CMD ["npm", "start"]
