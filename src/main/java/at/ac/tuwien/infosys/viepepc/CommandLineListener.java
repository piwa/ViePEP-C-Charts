package at.ac.tuwien.infosys.viepepc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Scanner;
import java.util.concurrent.Future;

/**
 * Created by philippwaibel on 20/06/16. edited by gerta sheganaku
 */
@Component
@Slf4j
@Profile("!test")
public class CommandLineListener implements CommandLineRunner {
    @Autowired
    private DataLoader dataLoader;


    public void run(String... args) {
        dataLoader.createGraph();
        System.exit(0);
    }

}

