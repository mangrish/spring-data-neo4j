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

package org.springframework.data.neo4j.repositories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.event.*;
import org.springframework.data.neo4j.events.TestNeo4jEventListener;
import org.springframework.data.neo4j.repositories.context.EventHandlingContext;
import org.springframework.data.neo4j.repositories.domain.Document;
import org.springframework.data.neo4j.repositories.domain.Folder;
import org.springframework.data.neo4j.repositories.repo.DocumentRepo;
import org.springframework.data.neo4j.repositories.repo.FolderRepo;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author vince
 */
@ContextConfiguration(classes = {EventHandlingContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class EventsTest {

    private Folder folder;
    private Document a;


    @Autowired
    private FolderRepo folderRepo;
    @Autowired
    private DocumentRepo documentRepo;

    @Autowired
    private TestNeo4jEventListener<BeforeSaveEvent> preSave;
    @Autowired
    private TestNeo4jEventListener<AfterSaveEvent> postSave;
    @Autowired
    private TestNeo4jEventListener<BeforeDeleteEvent> preDelete;
    @Autowired
    private TestNeo4jEventListener<AfterDeleteEvent> postDelete;

    @Before
    public void initialise() {

        // create model, not yet persisted
        folder = new Folder();
        a = new Document();

        a.setName("a");
        folder.setName("folder");
        folder.getDocuments().add(a);
        a.setFolder(folder);

        clearEvents();
    }

    private void clearEvents() {
        preSave.clear();
        postSave.clear();
        preDelete.clear();
        postDelete.clear();

    }

    @Test
    public void shouldPublishAppropriateEventsOnSave() {

        folderRepo.save(folder);

        assertEquals(2, this.preSave.getEvents().size());
        assertEquals(2, this.postSave.getEvents().size());

        assertEquals(0, this.preDelete.getEvents().size());
        assertEquals(0, this.postDelete.getEvents().size());

        assertTrue(preSave.hasObject(folder));
        assertTrue(preSave.hasObject(a));

        assertTrue(postSave.hasObject(folder));
        assertTrue(postSave.hasObject(a));
    }

    @Test
    public void shouldPublishAppropriateEventsOnDelete() {

        folderRepo.save(folder);

        clearEvents();

        documentRepo.delete(a);

        assertEquals(1, this.preSave.getEvents().size());
        assertEquals(1, this.postSave.getEvents().size());
        assertEquals(1, this.preDelete.getEvents().size());
        assertEquals(1, this.postDelete.getEvents().size());

        assertTrue(preSave.hasObject(folder));
        assertTrue(preDelete.hasObject(a));

        assertTrue(postSave.hasObject(folder));
        assertTrue(postDelete.hasObject(a));

    }

    @Test
    public void shouldPublishAppropriateEventsOnDirectUpdate() {

        folderRepo.save(folder);

        clearEvents();

        folder.setName("new folder");

        folderRepo.save(folder);

        assertEquals(1, this.preSave.getEvents().size());
        assertEquals(1, this.postSave.getEvents().size());
        assertEquals(0, this.preDelete.getEvents().size());
        assertEquals(0, this.postDelete.getEvents().size());

        assertTrue(preSave.hasObject(folder));
        assertTrue(postSave.hasObject(folder));

    }

    @Test
    public void shouldPublishAppropriateEventsOnTransitiveUpdate() {

        folderRepo.save(folder);

        clearEvents();

        a.setName("new document");

        folderRepo.save(folder);

        assertEquals(1, this.preSave.getEvents().size());
        assertEquals(1, this.postSave.getEvents().size());
        assertEquals(0, this.preDelete.getEvents().size());
        assertEquals(0, this.postDelete.getEvents().size());

        assertTrue(preSave.hasObject(a));
        assertTrue(postSave.hasObject(a));

    }

    @Test
    public void shouldPublishAppropriateEventsWhenItemRemovedFromCollection() {

        folderRepo.save(folder);

        clearEvents();

        folder.getDocuments().clear();

        folderRepo.save(folder);

        assertEquals(2, this.preSave.getEvents().size());
        assertEquals(2, this.postSave.getEvents().size());
        assertEquals(0, this.preDelete.getEvents().size());
        assertEquals(0, this.postDelete.getEvents().size());

        assertTrue(preSave.hasObject(folder));
        assertTrue(postSave.hasObject(folder));
        assertTrue(preSave.hasObject(a));
        assertTrue(postSave.hasObject(a));

    }

    @Test
    public void shouldPublishAppropriateEventsWhenItemAddedToCollection() {

        folderRepo.save(folder);

        clearEvents();

        Document b = new Document();
        b.setName("b");
        b.setFolder(folder);

        folder.getDocuments().add(b);

        folderRepo.save(folder);

        assertEquals(2, this.preSave.getEvents().size());
        assertEquals(2, this.postSave.getEvents().size());
        assertEquals(0, this.preDelete.getEvents().size());
        assertEquals(0, this.postDelete.getEvents().size());

        assertTrue(preSave.hasObject(folder));
        assertTrue(postSave.hasObject(folder));
        assertTrue(preSave.hasObject(b));
        assertTrue(postSave.hasObject(b));
    }
}
