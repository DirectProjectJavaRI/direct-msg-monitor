package org.nhindirect.monitor.repository;

import java.util.List;

import org.nhindirect.monitor.entity.AggregationCompleted;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AggregationCompletedRepository extends JpaRepository<AggregationCompleted, String>
{
	@Query("select p.id from AggregationCompleted p")
	public List<String> findAllKeys();
}
