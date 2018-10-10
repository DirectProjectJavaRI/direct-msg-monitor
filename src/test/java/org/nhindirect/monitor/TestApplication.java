package org.nhindirect.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan( basePackages= {"org.nhindirect.monitor.springconfig", "org.nhindirect.monitor.resources", 
		"org.nhindirect.monitor.streams", "org.nhindirect.monitor.entity"})
public class TestApplication
{	
    public static void main(String[] args) 
    {
        SpringApplication.run(TestApplication.class, args);
    }  

}
