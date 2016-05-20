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

package org.springframework.ogm.neo4j;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * @author Luanne Misquitta
 */
public class Neo4jSystemException extends UncategorizedDataAccessException{

	private static final long serialVersionUID = 347947370839580927L;

	public Neo4jSystemException(RuntimeException ex) {
		super(ex.getMessage(), ex);
	}
}
