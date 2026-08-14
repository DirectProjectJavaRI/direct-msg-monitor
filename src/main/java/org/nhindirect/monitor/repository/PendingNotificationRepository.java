package org.nhindirect.monitor.repository;

import java.util.Calendar;
import java.util.List;

import org.nhindirect.monitor.entity.PendingNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PendingNotificationRepository extends JpaRepository<PendingNotification, Long>
{
	@Query("select p.address from PendingNotification p where upper(p.dsnMessageId) = :dsnMessageId and upper(p.address) in :addresses")
	public List<String> findByDsnMessageIdIgnoreCaseAndAddressInIgnoreCase(@Param("dsnMessageId") String dsnMessageId, @Param("addresses") List<String> addresses);

	@Query("select p from PendingNotification p where upper(p.dsnMessageId) = :dsnMessageId")
	public List<PendingNotification> findByDsnMessageIdIgnoreCase(@Param("dsnMessageId") String dsnMessageId);

	@Transactional
	public void deleteByDsnMessageIdIgnoreCase(String dsnMessageId);

	public List<PendingNotification> findByCreatedTimeBefore(Calendar thresholdDate);
}
