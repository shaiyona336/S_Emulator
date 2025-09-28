package interactive;

import components.engine.Engine;
import components.engine.StandardEngine;
import dtos.ExecutionDetails;
import dtos.RunHistoryDetails;

import java.io.*;
import java.util.List;

public class LogicManager implements Runnable {
    private Engine engine = new StandardEngine();

    @Override
    public void run() {
        boolean exit = false;

        while (!exit) {
            ConsoleManager.Command command = ConsoleManager.getCommandFromUser();

            switch (command) {
                case READ_XML_FILE -> {
                    File file = ConsoleManager.readXmlFile();
                    try {
                        engine.loadProgramFromFile(file);
                        System.out.println("File " + file.getName() + " was successfully loaded." + System.lineSeparator());
                    } catch (RuntimeException e) {
                        System.out.println(e.getCause().getMessage() + ", file was not loaded." + System.lineSeparator());
                    }
                }
                case SHOW_PROGRAM -> {
                    if (engine.isProgramLoaded()) {
                        ConsoleManager.showProgram(engine.getProgramDetails());
                        System.out.println();
                    } else {
                        System.out.println("There is no loaded program in the system.");
                    }
                }
                case EXPAND_PROGRAM -> {
                    if (!engine.isProgramLoaded()) {
                        System.out.println("There is no loaded program in the system.");
                    } else if (engine.getProgramMaxDegree() == 0) {
                        System.out.println("This program can't be expanded because it's already composed by basic instructions only.");
                    } else {
                        int expansionDegree = ConsoleManager.getExpansionDegree(engine.getProgramMaxDegree());
                        ConsoleManager.showProgram(engine.expandProgram(expansionDegree));
                        System.out.println();
                    }
                }
                case RUN_PROGRAM -> {
                    if (!engine.isProgramLoaded()) {
                        System.out.println("There is no loaded program in the system.");
                    }
                    else {
                        int runningDegree;
                        if (engine.getProgramMaxDegree() == 0) {
                            runningDegree = 0;
                            System.out.println("This program can only run as default, run degree was set to 0.");
                        }
                        else {
                            runningDegree = ConsoleManager.getRunDegree(engine.getProgramMaxDegree());
                        }
                        Long[] inputs = ConsoleManager.getProgramInputs(engine.getProgramDetails().inputVariables());
                        ExecutionDetails executionDetails = engine.runProgram(runningDegree, inputs);
                        ConsoleManager.showExecutionDetails(executionDetails);
                        System.out.println();
                    }
                }
                case SHOW_STATISTICS -> {
                    if (!engine.isProgramLoaded()) {
                        System.out.println("There is no loaded program in the system.");
                    }
                    else if (!engine.isRunning()) {
                        System.out.println("There is no statistics to show since this program hasn't been executed yet.");
                    }
                    else {
                        List<RunHistoryDetails> runHistoryDetails = engine.getStatistics();
                        ConsoleManager.showStatistics(runHistoryDetails);
                        System.out.println();
                    }
                }
                case SAVE_STATE_TO_FILE -> {
                    if (!engine.isProgramLoaded()) {
                        System.out.println("There is no loaded program in the system.");
                    }
                    else {
                        String fileName = ConsoleManager.getFileName();
                        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
                            out.writeObject(engine);
                            System.out.println("State saved successfully to " + fileName + System.lineSeparator());
                        } catch (IOException e) {
                            System.out.println("Error saving state: " + e.getMessage() + System.lineSeparator());
                        }
                    }
                }
                case LOAD_STATE_FROM_FILE -> {
                    String filePath = ConsoleManager.getStateFilePath();
                    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
                        engine = (Engine) in.readObject();
                        System.out.println("State file " + filePath + " was successfully loaded." + System.lineSeparator());
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("Error: Failed to load state. " + e.getMessage() + System.lineSeparator());
                    }
                }
                case EXIT -> exit = true;
            }
        }
    }
}