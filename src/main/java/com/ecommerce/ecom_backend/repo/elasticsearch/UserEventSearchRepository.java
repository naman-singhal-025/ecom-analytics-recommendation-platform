package com.ecommerce.ecom_backend.repo.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.ecom_backend.model.UserEvent;

/**
 * Spring Data repository for {@link UserEvent} documents in Elasticsearch.
 */
@Repository
public interface UserEventSearchRepository extends ElasticsearchRepository<UserEvent, String> {
    long countByEventType(String eventType);
}