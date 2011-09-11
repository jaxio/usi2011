package usi2011;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.jmx.support.MBeanRegistrationSupport.REGISTRATION_IGNORE_EXISTING;
import static usi2011.util.Profiles.HECTOR;

import org.slf4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;

import usi2011.http.HttpServer;
import usi2011.repository.UserRepository;
import usi2011.statemachine.StateMachine;
import usi2011.util.Profiles;

public class Main {
    private static final Logger logger = getLogger(Main.class);
    public static AnnotationConfigApplicationContext context;

    static {
        banner();
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setDefaultProfiles(Profiles.HECTOR);
        context.getEnvironment().setActiveProfiles(Profiles.HECTOR);
        logger.warn("Starting up...1/5: register spring configuration...");
        context.register(DefaultConfiguration.class, HectorConfiguration.class);
        logger.warn("Starting up...2/5: refresh context");
        context.refresh();
        logger.warn("Starting up...3/5: static init done");
    }

    public static void main(String[] args) {
        logger.warn("Starting up...4/5: building server");
        context.getBean(HttpServer.class).build();
        logger.warn("Starting up...5/5: starting phase done");
        StateMachine sm = context.getBean(StateMachine.class);
        logger.warn("StateMachine: " + sm.getCurrentState());
        
        ((UserRepository)context.getBean(UserRepository.class)).loadUsersInMemory();        
    }

    @ImportResource("classpath*:/applicationContext.xml")
    @ComponentScan(basePackageClasses = Main.class)
    public static class DefaultConfiguration {
        @Bean
        public AnnotationMBeanExporter mbeanExporter() {
            AnnotationMBeanExporter exporterThatCatchesExceptions = new AnnotationMBeanExporter();
            exporterThatCatchesExceptions.setRegistrationBehavior(REGISTRATION_IGNORE_EXISTING);
            exporterThatCatchesExceptions.setEnsureUniqueRuntimeObjectNames(false);
            return exporterThatCatchesExceptions;
        }
    }

    @Profile(HECTOR)
    public static class HectorConfiguration {
        // keep it for legacy purposes.
    }

    private static void banner() {
        logger.warn("");
        logger.warn("   CHALLENGE USI 2011");
        logger.warn("");
        logger.warn("   JAXIO & FRIENDS TEAM");
        logger.warn("");
        logger.warn("   Le code sera mis en Open Source en fin de competition");
        logger.warn("   sous une licence restant a definir.");
        logger.warn("");
        logger.warn("   Pour le moment le code source est strictement confidentiel.");
        logger.warn("   Tous droits reserves Jaxio SARL & Friends ");
        logger.warn("");
        logger.warn("   Aucune utilisation, reproduction, diffusion, meme partielle");
        logger.warn("   permise sans accord prealable de Jaxio SARL & Friends");
        logger.warn("");
        logger.warn("     Julien Dubois");
        logger.warn("     Bernard Pons");
        logger.warn("     Florent Ramiere");
        logger.warn("     Nicolas Romanetti");
        logger.warn("");
        logger.warn("   Contact: usi2011team@jaxio.com");
        logger.warn("");
    }
}