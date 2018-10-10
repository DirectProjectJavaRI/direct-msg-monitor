package org.nhindirect.monitor.repository;

import java.util.Calendar;
import java.util.List;

import org.nhindirect.monitor.entity.ReceivedNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReceivedNotificationRepository extends JpaRepository<ReceivedNotification, Long>
{
	@Query("select r.address from ReceivedNotification r where upper(r.messageid) = :messageid and upper(r.address) in :addresses")
	public List<String> findByMessageidIgnoreCaseAndAddressInIgnoreCase(@Param("messageid") String messageId, @Param("addresses") List<String> addresses);
	
	@Transactional
	public void deleteByReceivedTimeBefore(Calendar thresholdDate);
}
