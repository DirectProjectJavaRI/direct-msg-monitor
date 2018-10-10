package org.nhindirect.monitor.repository;

import java.util.List;

import org.nhindirect.monitor.entity.Aggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AggregationRepository extends JpaRepository<Aggregation, String>
{
	@Query("select p.id from Aggregation p")
	public List<String> findAllKeys();
}
