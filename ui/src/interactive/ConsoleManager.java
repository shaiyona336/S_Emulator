package interactive;

import components.instruction.Instruction;
import components.label.Label;
import components.variable.Variable;
import dtos.ExecutionDetails;
import dtos.ProgramDetails;
import dtos.RunHistoryDetails;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class ConsoleManager {
    public enum Command {
        READ_XML_FILE,
        SHOW_PROGRAM,
        EXPAND_PROGRAM,
        RUN_PROGRAM,
        SHOW_STATISTICS,
        SAVE_STATE_TO_FILE,
        LOAD_STATE_FROM_FILE,
        EXIT,
        INVALID_COMMAND,
    }

    private static final Scanner scanner = new Scanner(System.in);

    public static Command getCommandFromUser()
    {
        Command command;

        do {
            showMenu();

            try {
                int commandNumber = Integer.parseInt(scanner.nextLine());

                command = switch (commandNumber) {
                    case 1 -> Command.READ_XML_FILE;
                    case 2 -> Command.SHOW_PROGRAM;
                    case 3 -> Command.EXPAND_PROGRAM;
                    case 4 -> Command.RUN_PROGRAM;
                    case 5 -> Command.SHOW_STATISTICS;
                    case 6 -> Command.SAVE_STATE_TO_FILE;
                    case 7 -> Command.LOAD_STATE_FROM_FILE;
                    case 8 -> Command.EXIT;
                    default -> {
                        System.out.println("Invalid input. Please enter a number from 1 to 8.");
                        yield Command.INVALID_COMMAND;
                    }
                };
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number from 1 to 8.");
                command =  Command.INVALID_COMMAND;
            }
        } while (command ==  Command.INVALID_COMMAND);

        return command;
    }

    private static void showMenu()
    {
        System.out.println("-----------------------------------------------");
        System.out.println("Enter the number of the command you want to run");
        System.out.println("1. Read XML File");
        System.out.println("2. Show Program");
        System.out.println("3. Expand Program");
        System.out.println("4. Run Program");
        System.out.println("5. Show Statistics");
        System.out.println("6. Save State to File");
        System.out.println("7. Load State from File");
        System.out.println("8. Exit Program");
        System.out.println("-----------------------------------------------");
    }

    public static File readXmlFile()
    {
        boolean validFile = false;
        File file;

        do {
            System.out.print("Enter path to your xml file: ");
            String path = scanner.nextLine();
            file = new File(path);

            if (!path.endsWith(".xml")) {
                System.out.println("Invalid path. Please enter a xml file.");
            }
            else if (!file.exists()) {
                System.out.println("File does not exist. Please enter a valid path.");
            }
            else if (!file.isFile()) {
                System.out.println("Invalid path. Please enter a valid path.");
            }
            else {
                validFile = true;
            }
        } while (!validFile);

        return file;
    }

    public static void showProgram(ProgramDetails programDetails) {
        System.out.println("===============================================");
        System.out.println("Program name: " + programDetails.name());

        System.out.print("Variables:");
        for (Variable variable : programDetails.inputVariables())
        {
            System.out.print(" " + variable.getStringVariable());
        }
        System.out.println();

        System.out.print("Labels:");
        for (Label label : programDetails.labels())
        {
            System.out.print(" " + label.getStringLabel());
        }
        System.out.println();

        System.out.println("Instructions:");
        for (Instruction instruction : programDetails.instructions()) {
            StringBuilder instructionLine = new StringBuilder(instruction.getStringInstruction());
            Instruction currentInstruction = instruction;

            while(currentInstruction.hasAncientInstruction()) {
                currentInstruction = currentInstruction.getAncientInstruction();
                instructionLine.append(" >>> ").append(currentInstruction.getStringInstruction());
            }

            System.out.println(instructionLine);
        }
        System.out.println("===============================================");
    }

    public static int getExpansionDegree(int maxDegree) {
        boolean validDegree = false;
        int degree = 0;

        System.out.println("The program max degree is: " + maxDegree);
        while (!validDegree) {
            System.out.print("Enter the desired expansion degree: ");
            try {
                degree = Integer.parseInt(scanner.nextLine());
                if (1 <= degree && degree <= maxDegree) {
                    validDegree = true;
                } else {
                    System.out.println("Invalid input. Please enter a number from 1 to " + maxDegree + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number from 1 to " + maxDegree + ".");
            }
        }

        return degree;
    }

    public static int getRunDegree(int maxRunDegree) {
        boolean validDegree = false;
        int degree = 0;

        System.out.println("The program max degree is: " + maxRunDegree);
        while (!validDegree) {
            System.out.print("Enter the desired running degree 1-" + maxRunDegree + ", or 0 for running default program: ");
            try {
                degree = Integer.parseInt(scanner.nextLine());
                if (0 <= degree && degree <= maxRunDegree) {
                    validDegree = true;
                } else {
                    System.out.println("Invalid input. Please enter a number from 0 to " + maxRunDegree + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number from 1 to " + maxRunDegree + ".");
            }
        }

        return degree;
    }

    public static Long[] getProgramInputs(List<Variable> inputVariables) {
        System.out.print("Input variables:");
        for (Variable variable : inputVariables)
        {
            System.out.print(" " + variable.getStringVariable());
        }

        System.out.println();

        Long[] longInputs;
        boolean validInput;
        do {
            System.out.print("Enter your input: ");
            validInput = true;
            String input = scanner.nextLine();
            if (input.isEmpty()) {
                return new Long[0];
            }

            String[] inputs = input.split(",");
            longInputs = new Long[inputs.length];

            for (int i = 0; i < inputs.length; i++) {
                try {
                    longInputs[i] = Long.parseLong(inputs[i]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid inputs, enter numbers seperated by a comma.");
                    validInput = false;
                    break;
                }
            }
        } while (!validInput);
        return longInputs;
    }

    public static void showExecutionDetails(ExecutionDetails executionDetails) {
        showProgram(executionDetails.programDetails());
        System.out.print("Execution result: ");
        long yValue = executionDetails.variables().getVariableValue(Variable.OUTPUT);
        System.out.print(Variable.OUTPUT.getStringVariable() + " = " + yValue);

        for (Variable xVariable : executionDetails.programDetails().inputVariables()) {
            long xValue = executionDetails.variables().getVariableValue(xVariable);
            System.out.print(", " + xVariable.getStringVariable() + " = " + xValue);
        }

        for (Variable zVariable : executionDetails.programDetails().workVariables()) {
            long zValue = executionDetails.variables().getVariableValue(zVariable);
            System.out.print(", " + zVariable.getStringVariable() + " = " + zValue);
        }
        System.out.println();
        System.out.println("Cycles consumed: " +  executionDetails.cycles());
    }

    public static void showStatistics(List<RunHistoryDetails> runHistoryDetails) {
        for (RunHistoryDetails runHistoryDetail : runHistoryDetails) {
            System.out.println("--------------------");
            System.out.println("Run number: " + runHistoryDetail.runNumber());
            System.out.println("Execution degree: " + runHistoryDetail.expansionDegree());
            System.out.println("Inputs: " + runHistoryDetail.inputs().toString());
            System.out.println("y value: " + runHistoryDetail.yValue());
            System.out.println("Cycles consumed: " + runHistoryDetail.cyclesNumber());
            System.out.println("--------------------");
        }
    }

    public static String getFileName() {
        System.out.print("Enter file name: ");

        return scanner.nextLine();
    }

    public static String getStateFilePath() {
        boolean validFile = false;
        File file;
        String path;

        do {
            System.out.print("Enter path to file: ");
            path = scanner.nextLine();
            file = new File(path);

            if (!file.exists()) {
                System.out.println("File does not exist. Please enter a valid path.");
            }
            else if (!file.isFile()) {
                System.out.println("Invalid path. Please enter a valid path.");
            }
            else {
                validFile = true;
            }
        } while (!validFile);

        return path;
    }
}