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

package org.springframework.ogm.neo4j.template;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.ogm.neo4j.template.context.DataManipulationEventConfiguration;
import org.springframework.ogm.neo4j.Neo4jOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@ContextConfiguration(classes = DataManipulationEventConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@DirtiesContext
public class ExceptionTranslationTest extends MultiDriverTestClass {

    @Autowired
    private Neo4jOperations neo4jTemplate;


    @Before
    public void setUp()  {
        Assert.notNull(neo4jTemplate, "neo4jTemplate not properly wired in");
    }

    @Test(expected= InvalidDataAccessApiUsageException.class)
    public void testTemplateExceptionsAreIntercepted() {
        neo4jTemplate.loadAll(Rating.class, 0);

    }

}
