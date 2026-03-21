FROM node:20-alpine AS build
WORKDIR /workspace
COPY package.json ./
RUN npm install
COPY . .
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginx:1.26-alpine
ENV API_BACKEND_HOST=localhost:8080
ENV VITE_API_BASE_URL=http://localhost:8080
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
