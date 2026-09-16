package dit.us.derma.task_performer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaskPerformerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskPerformerApplication.class, args);
	}

}
