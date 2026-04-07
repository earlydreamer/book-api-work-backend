package dev.earlydreamer.todayus.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

	@Bean(initMethod = "migrate")
	Flyway flyway(DataSource dataSource) {
		return Flyway.configure()
			.locations("classpath:db/migration")
			.dataSource(dataSource)
			.load();
	}

	@Bean
	static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
		return (ConfigurableListableBeanFactory beanFactory) -> {
			if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
				return;
			}

			BeanDefinition beanDefinition = beanFactory.getBeanDefinition("entityManagerFactory");
			String[] dependsOn = beanDefinition.getDependsOn();
			if (dependsOn == null || dependsOn.length == 0) {
				beanDefinition.setDependsOn("flyway");
				return;
			}

			String[] nextDependsOn = new String[dependsOn.length + 1];
			System.arraycopy(dependsOn, 0, nextDependsOn, 0, dependsOn.length);
			nextDependsOn[dependsOn.length] = "flyway";
			beanDefinition.setDependsOn(nextDependsOn);
		};
	}
}
