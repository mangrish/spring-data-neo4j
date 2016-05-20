/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.examples.movies.repo;

import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

/**
 * @author Luanne Misquitta
 */
public interface RatingRepository extends Neo4jRepository<Rating> {

	List<Rating> findByStars(int stars);

	List<Rating> findByStarsAndRatingTimestamp(int stars, long ratingTimestamp);

	List<Rating> findByStarsOrRatingTimestamp(int stars, long ratingTimestamp);

	List<Rating> findByStarsAndRatingTimestampLessThan(int stars, long ratingTimestamp);

	List<Rating> findByStarsOrRatingTimestampGreaterThan(int stars, long ratingTimestamp);

	List<Rating> findByUserName(String name);

	List<Rating> findByMovieName(String name);

	List<Rating> findByUserNameAndMovieName(String userName, String movieName);

	List<Rating> findByUserNameAndStars(String name, int stars);

	List<Rating> findByStarsAndMovieName(int stars, String name);

	List<Rating> findByUserNameAndMovieNameAndStars(String userName, String movieName, int stars);

	List<Rating> findByStarsOrUserName(int stars, String username);

	List<Rating> findByUserNameAndUserMiddleName(String username, String middleName);

}
