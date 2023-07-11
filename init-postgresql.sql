DROP SCHEMA IF EXISTS movies CASCADE;
CREATE SCHEMA movies AUTHORIZATION movieuser;
drop table if exists movies.Movie cascade;
create table movies.Movie (
                                      id integer not null,
                                      title varchar(255),
                                      year integer,
                                      primary key (id)
);

