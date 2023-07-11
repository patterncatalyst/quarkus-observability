package org.acme.observability;
import jakarta.persistence.*;
//import io.quarkus.hibernate.orm.panache.PanacheEntity;
//import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
@Table(name = "movie")
@NamedQuery(name = "Movies.findAll", query = "SELECT m FROM MovieEntity m ORDER BY m.title")
public class MovieEntity {

    @Id
    @SequenceGenerator(name = "moviesSequence", sequenceName = "known_movies_id_seq", allocationSize = 1, initialValue = 10)
    @GeneratedValue(generator = "moviesSequence")
    public Integer id;

    public String title;
    public Integer year;


}
