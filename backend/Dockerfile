FROM node:22-alpine
WORKDIR /app
COPY functions/package.json ./
RUN npm install --omit=dev && npm cache clean --force
COPY functions/ ./
USER node
EXPOSE 8080
CMD ["node","server.mjs"]
