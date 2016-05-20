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

package org.springframework.data.neo4j.repository;

import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ogm.neo4j.Neo4jOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
@Repository
public class GraphRepositoryImpl<T> implements GraphRepository<T> {

    private static final int DEFAULT_QUERY_DEPTH = 1;

    private final Class<T> clazz;
    private final Neo4jOperations neo4jOperations;

    public GraphRepositoryImpl(Class<T> clazz, Neo4jOperations neo4jOperations) {
        this.clazz = clazz;
        this.neo4jOperations = neo4jOperations;
    }

    @Override
    public <S extends T> S save(S entity) {
        neo4jOperations.save(entity);
        return entity;
    }

    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        for (S entity : entities) {
            neo4jOperations.save(entity);
        }
        return entities;
    }

    @Override
    public T findOne(Long id) {
        return neo4jOperations.load(clazz, id);
    }

    @Override
    public boolean exists(Long id) {
        return findOne(id) != null;
    }

    @Override
    public long count() {
        return neo4jOperations.count(clazz);
    }

    @Override
    public void delete(Long id) {
        Object o = findOne(id);
        if (o != null) {
            neo4jOperations.delete(o);
        }
    }

    @Override
    public void delete(T t) {
        neo4jOperations.delete(t);
    }

    @Override
    public void delete(Iterable<? extends T> ts) {
        for (T t : ts) {
            neo4jOperations.delete(t);
        }
    }

    @Override
    public void deleteAll() {
        neo4jOperations.deleteAll(clazz);
    }

    @Override
    public <S extends T> S save(S s, int depth) {
        neo4jOperations.save(s, depth);
        return s;
    }

    @Override
    public <S extends T> Iterable<S> save(Iterable<S> ses, int depth) {
        neo4jOperations.save(ses, depth);
        return ses;
    }

    @Override
    public T findOne(Long id, int depth) {
        return neo4jOperations.load(clazz, id, depth);
    }

    // findAll and variants
    @Override
    public Iterable<T> findAll() {
        return findAll(DEFAULT_QUERY_DEPTH);
    }

    @Override
    public Iterable<T> findAll(int depth) {
        return neo4jOperations.loadAll(clazz, depth);
    }

    @Override
    public Iterable<T> findAll(Iterable<Long> longs) {
        return findAll(longs, DEFAULT_QUERY_DEPTH);
    }

    @Override
    public Iterable<T> findAll(Iterable<Long> ids, int depth) {
        return neo4jOperations.loadAll(clazz, (Collection<Long>) ids, depth);
    }

    @Override
    public Iterable<T> findAll(Sort sort) {
        return findAll(sort, DEFAULT_QUERY_DEPTH);
    }

    @Override
    public Iterable<T> findAll(Sort sort, int depth) {
        return neo4jOperations.loadAll(clazz, convert(sort), depth);
    }

    @Override
    public Iterable<T> findAll(Iterable<Long> ids, Sort sort) {
        return findAll(ids, sort, DEFAULT_QUERY_DEPTH);
    }

    @Override
    public Iterable<T> findAll(Iterable<Long> ids, Sort sort, int depth) {
        return neo4jOperations.loadAll(clazz, (Collection<Long>) ids, convert(sort), depth);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return findAll(pageable, DEFAULT_QUERY_DEPTH);
    }

    @Override
    public Page<T> findAll(Pageable pageable, int depth) {
        Collection<T> data = neo4jOperations.loadAll(clazz, convert(pageable.getSort()), new Pagination(pageable.getPageNumber(), pageable.getPageSize()), depth);
        return updatePage(pageable, new ArrayList<T>(data));
    }

    /*
     * Converts a Spring Data Sort object to an OGM SortOrder
     */
    private SortOrder convert(Sort sort) {

        SortOrder sortOrder = new SortOrder();

        if (sort != null) {
            for (Sort.Order order : sort) {
                if (order.isAscending()) {
                    sortOrder.add(order.getProperty());
                } else {
                    sortOrder.add(SortOrder.Direction.DESC, order.getProperty());
                }
            }
        }
        return sortOrder;
    }

    /*
     * This is a cheap trick to estimate the total number of objects without actually knowing the real value.
     * Essentially, if the result size is the same as the page size, we assume more data can be fetched, so
     * we set the expected total to the current total retrieved so far + the current page size. As soon as the
     * result size is less than the page size, we know there are no more, so we set the total to the number
     * retrieved so far. This will ensure that page.next() returns false.
     */
    private Page<T> updatePage(Pageable pageable, List<T> results) {
        int pageSize = pageable.getPageSize();
        int pageOffset = pageable.getOffset();
        int total = pageOffset + results.size() + (results.size() == pageSize ? pageSize : 0);

        return new PageImpl<T>(results, pageable, total);
    }
}
