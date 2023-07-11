package org.acme.observability;

import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MovieProducer {

    @Inject @Channel("movies-out")
    Emitter<Record<Integer, String>> emitter;

    public void sendMovieToKafka(MovieEntity movieEntity) {
        emitter.send(Record.of(movieEntity.year, movieEntity.title));
    }
}
