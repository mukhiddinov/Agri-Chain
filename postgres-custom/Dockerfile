FROM postgres:16-alpine
COPY ./init-postgres.sh /docker-entrypoint-initdb.d/init-postgres.sh
RUN chmod +x /docker-entrypoint-initdb.d/init-postgres.sh
