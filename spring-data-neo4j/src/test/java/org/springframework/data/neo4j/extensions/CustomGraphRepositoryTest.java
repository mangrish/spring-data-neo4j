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
package org.springframework.data.neo4j.extensions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertTrue;

/**
 * @author: Vince Bickers
 */
@ContextConfiguration(classes = {CustomPersistenceContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class CustomGraphRepositoryTest extends MultiDriverTestClass {

    @Autowired
    private UserRepository repository;

    /**
     * asserts that the correct proxied object is created by Spring
     * and that we can integrate with it.
     */
    @Test
    public void shouldExposeCommonMethodOnExtendedRepository() {
        assertTrue(repository.sharedCustomMethod());
    }

}
