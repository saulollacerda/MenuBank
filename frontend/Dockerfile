# frontend/Dockerfile
FROM node:20-alpine AS build

# Declare build-time variables so Railway passes them during `npm run build`.
# These are baked into the Vite bundle at build time.
ARG VITE_SUPABASE_URL
ARG VITE_SUPABASE_ANON_KEY
ARG VITE_AUTH_PROVIDER=supabase
ARG VITE_API_BASE_URL=/api

ENV VITE_SUPABASE_URL=$VITE_SUPABASE_URL
ENV VITE_SUPABASE_ANON_KEY=$VITE_SUPABASE_ANON_KEY
ENV VITE_AUTH_PROVIDER=$VITE_AUTH_PROVIDER
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

WORKDIR /app
COPY package*.json .
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
ENV PORT=80
COPY --from=build /app/dist /usr/share/nginx/html
RUN chmod -R 755 /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf.template
EXPOSE 80
CMD ["/bin/sh", "-c", "envsubst '${BACKEND_URL} ${PORT}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]