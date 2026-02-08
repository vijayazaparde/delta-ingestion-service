package com.delta.ingestion.repository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository repo;

    @Test
    void shouldFindByExternalIdIn() {
        Set<String> ids = Set.of("cust_1","cust_2");
        Set<String> result = repo.findByExternalIdIn(ids);
        assertNotNull(result);
    }
}
