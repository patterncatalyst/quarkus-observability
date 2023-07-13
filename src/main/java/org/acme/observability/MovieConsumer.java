package org.acme.observability;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MovieConsumer {

    private final Logger logger = Logger.getLogger(MovieConsumer.class);

    @Inject
    Mutiny.SessionFactory sf;

    @Incoming("movies-in")
    public Uni<Void> receive(Record<Integer, String> record) {

        logger.infof("Got a movie: %d - %s", record.key(), record.value());

        MovieEntity movieEntity = new MovieEntity();
        movieEntity.title = record.value();
        movieEntity.year = record.key();

        return sf.withTransaction((session, tx) -> session.persist(movieEntity));
    }
}
