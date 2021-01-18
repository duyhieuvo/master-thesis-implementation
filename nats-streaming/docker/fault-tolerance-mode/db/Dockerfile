FROM postgres:9.5

# Expose the PostgreSQL port
EXPOSE 5432

ADD postgres.db.sql /docker-entrypoint-initdb.d/

RUN echo "listen_addresses='*'" >> /etc/postgresql/postgresql.conf &&\
    echo "max_prepared_transactions = 5" >> /etc/postgresql/postgresql.conf

CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]