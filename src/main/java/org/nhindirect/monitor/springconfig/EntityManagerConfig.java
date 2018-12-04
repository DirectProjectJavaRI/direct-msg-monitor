package org.nhindirect.monitor.springconfig;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.util.ClassUtils;

@Configuration
public class EntityManagerConfig
{
	@Autowired
	protected DataSource dataSource;
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory()
	{		   
		  try
		  {
			 final HibernateJpaVendorAdapter jpaAdaptor = new HibernateJpaVendorAdapter();
			 jpaAdaptor.setGenerateDdl(true);
			 
			 final LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
			 entityManagerFactory.setDataSource(dataSource);
			 entityManagerFactory.setPersistenceUnitName("direct-msg-monitor-store");
			 entityManagerFactory.setJpaVendorAdapter(jpaAdaptor);
			 
			 entityManagerFactory.setPackagesToScan(ClassUtils.getPackageName(org.nhindirect.monitor.entity.Aggregation.class));

			 return entityManagerFactory;		
		  }
		  catch (Exception e)
		  {
			  throw new IllegalStateException("Failed to build entity factory manager.", e);
		  }
	}	
}
