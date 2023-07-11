package org.acme.observability;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MovieResource {

    @Inject
    MovieProducer producer;

    @POST
    public Response send(MovieEntity movieEntity) {
        producer.sendMovieToKafka(movieEntity);
        // Return an 202 - Accepted response.
        return Response.accepted().build();
    }

//    @GET
//    @Path("movies")
//    public List<MovieEntity> getMovies() {
//        return MovieEntity.listAll(Sort.by("year"));
//    }
}
