package org.nhindirect.monitor.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.nhindirect.monitor.entity.Aggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AggregationRepository extends JpaRepository<Aggregation, String>
{
	@Query("select p.id from Aggregation p")
	public List<String> findAllKeys();
	
	@Override
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Aggregation> findById(String id);
}