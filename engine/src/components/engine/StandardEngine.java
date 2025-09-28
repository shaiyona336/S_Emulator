package components.engine;

import components.executor.ProgramExecutor;
import components.instruction.Instruction;
import components.instruction.implementations.synthetic.QuoteInstruction;
import components.jaxb.generated.*;
import components.program.JaxbConversion;
import components.program.Program;
import dtos.DebugStepDetails;
import dtos.ExecutionDetails;
import dtos.ProgramDetails;
import dtos.RunHistoryDetails;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class StandardEngine implements Engine {
    final static String JAXB_XML_PACKAGE_NAME = "components.jaxb.generated";

    private record FunctionData(Program program, String userString) {}

    private Program program;
    private boolean programLoaded = false;
    private int runNumber;
    private List<RunHistoryDetails> runHistoryDetails = new ArrayList<>();

    private Map<String, FunctionData> definedFunctions = new HashMap<>();

    private Program contextProgram;

    private ProgramExecutor debugExecutor = null;
    private boolean isInDebugMode = false;
    private Program debugProgram = null;
    private int debugExpansionDegree = 0;


    @Override
    public void loadProgramFromFile(File file) {
        SProgram sProgram = parseXmlFile(file);
        try {
            this.definedFunctions.clear();
            runNumber = 0;
            runHistoryDetails = new ArrayList<>();

            jumpLabelsAreValid(sProgram.getSInstructions());

            if (sProgram.getSFunctions() != null) {
                for (SFunction sFunc : sProgram.getSFunctions().getSFunction()) {
                    jumpLabelsAreValid(sFunc.getSInstructions());
                    Program functionAsProgram = JaxbConversion.SFunctionToProgram(sFunc);
                    // --- MODIFIED: Store the function program AND its user-string ---
                    definedFunctions.put(sFunc.getName(), new FunctionData(functionAsProgram, sFunc.getUserString()));
                }
            }
            program = JaxbConversion.SProgramToProgram(sProgram);

            // --- NEW: Set the initial context to the main program ---
            this.contextProgram = this.program;

            validateFunctionCalls(program, definedFunctions);
            for (FunctionData data : definedFunctions.values()) {
                validateFunctionCalls(data.program(), definedFunctions);
            }

            programLoaded = true;
        } catch (RuntimeException e) {
            programLoaded = false;
            definedFunctions.clear();
            throw e;
        }
    }

    private void validateFunctionCalls(Program progToValidate, Map<String, FunctionData> functions) {
        for (Instruction inst : progToValidate.getInstructions()) {
            if (inst instanceof QuoteInstruction quote) {
                if (!functions.containsKey(quote.getFunctionName())) {
                    throw new RuntimeException("Validation Error: Function '" + quote.getFunctionName() + "' is not defined.");
                }
            }
        }
    }

    @Override
    public boolean isProgramLoaded() {
        return programLoaded;
    }

    private SProgram parseXmlFile(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            JAXBContext jaxbContext = JAXBContext.newInstance(JAXB_XML_PACKAGE_NAME);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (SProgram) unmarshaller.unmarshal(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void jumpLabelsAreValid(SInstructions instructions) {
        Set<String> instructionLabels = new HashSet<>();
        Set<String> jumpLabels = new HashSet<>();
        if (instructions == null) return;
        for (SInstruction instruction : instructions.getSInstruction()) {
            if (instruction.getSLabel() != null) {
                instructionLabels.add(instruction.getSLabel());
            }
            if (instruction.getName().contains("JUMP") || instruction.getName().contains("GOTO")) {
                if (instruction.getSInstructionArguments() != null) {
                    for (SInstructionArgument argument : instruction.getSInstructionArguments().getSInstructionArgument()) {
                        if (argument.getName().contains("Label")) {
                            jumpLabels.add(argument.getValue());
                        }
                    }
                }
            }
        }
        for (String jumpLabel : jumpLabels) {
            if (!instructionLabels.contains(jumpLabel) && !jumpLabel.equals("EXIT")) {
                throw new RuntimeException("Can't jump to label " + jumpLabel);
            }
        }
    }

    @Override
    public ProgramDetails getProgramDetails() {
        if (!programLoaded || contextProgram == null) return null;
        return new ProgramDetails(
                contextProgram.getName(),
                contextProgram.getInputVariables(getProgramMap()),
                contextProgram.getWorkVariables(getProgramMap()),
                contextProgram.getLabels(getProgramMap()),
                contextProgram.getInstructions()
        );
    }

    @Override
    public ProgramDetails expandProgram(int expansionDegree) {
        Program currentProgram = this.contextProgram;
        for (int i = 0; i < expansionDegree; i++) {
            currentProgram = currentProgram.expand(getProgramMap());
        }
        return new ProgramDetails(
                currentProgram.getName(),
                currentProgram.getInputVariables(getProgramMap()),
                currentProgram.getWorkVariables(getProgramMap()),
                currentProgram.getLabels(getProgramMap()),
                currentProgram.getInstructions()
        );
    }

    @Override
    public int getProgramMaxDegree() {
        if (!programLoaded || contextProgram == null) return 0;
        return contextProgram.calculateMaxDegree(getProgramMap());
    }

    @Override
    public ExecutionDetails runProgram(int expansionDegree, Long... input) {
        Program programToRun = this.contextProgram;
        for (int i = 0; i < expansionDegree; i++) {
            programToRun = programToRun.expand(getProgramMap());
        }

        ProgramExecutor programExecutor = new ProgramExecutor(programToRun, getProgramMap());
        Long y = programExecutor.run(input);

        runHistoryDetails.add(new RunHistoryDetails(++runNumber, expansionDegree, List.of(input), y, programExecutor.getCyclesNumber()));

        return new ExecutionDetails(
                new ProgramDetails(programToRun.getName(), programToRun.getInputVariables(getProgramMap()), programToRun.getWorkVariables(getProgramMap()), programToRun.getLabels(getProgramMap()), programToRun.getInstructions()),
                programExecutor.getVariablesContext(),
                programExecutor.getCyclesNumber()
        );
    }

    @Override
    public List<RunHistoryDetails> getStatistics() {
        return runHistoryDetails;
    }

    @Override
    public boolean isRunning() {
        return runNumber > 0;
    }

    @Override
    public DebugStepDetails startDebugging(int degree, Long[] inputs) {
        if (isInDebugMode) {
            stop();
        }
        this.debugExpansionDegree = degree;
        this.debugProgram = this.contextProgram;
        for (int i = 0; i < degree; i++) {
            this.debugProgram = this.debugProgram.expand(getProgramMap());
        }

        this.debugExecutor = new ProgramExecutor(this.debugProgram, getProgramMap());
        this.debugExecutor.initializeDebugSession(inputs);
        isInDebugMode = true;

        return new DebugStepDetails(
                this.debugExecutor.getVariablesContext(),
                1,
                this.debugExecutor.isFinished()
        );
    }

    @Override
    public DebugStepDetails stepOver() {
        if (!isInDebugMode || this.debugExecutor == null) {
            throw new IllegalStateException("Not in a debug session. Cannot step over.");
        }
        this.debugExecutor.stepOver();
        return new DebugStepDetails(
                this.debugExecutor.getVariablesContext(),
                this.debugExecutor.getNextInstructionNumber(),
                this.debugExecutor.isFinished()
        );
    }

    @Override
    public ExecutionDetails resume() {
        if (!isInDebugMode || this.debugExecutor == null) {
            throw new IllegalStateException("Not in a debug session. Cannot resume.");
        }
        Long y = this.debugExecutor.resume();
        runHistoryDetails.add(new RunHistoryDetails(++runNumber, this.debugExpansionDegree, List.of(this.debugExecutor.getInitialInputs()), y, this.debugExecutor.getCyclesNumber()));

        ExecutionDetails finalDetails = new ExecutionDetails(
                new ProgramDetails(this.debugProgram.getName(), this.debugProgram.getInputVariables(getProgramMap()), this.debugProgram.getWorkVariables(getProgramMap()), this.debugProgram.getLabels(getProgramMap()), this.debugProgram.getInstructions()),
                this.debugExecutor.getVariablesContext(),
                this.debugExecutor.getCyclesNumber()
        );
        stop();
        return finalDetails;
    }

    @Override
    public void stop() {
        this.debugExecutor = null;
        this.debugProgram = null;
        this.isInDebugMode = false;
    }

    @Override
    public void addRunToHistory(RunHistoryDetails details) {
        if (this.runHistoryDetails != null) {
            this.runHistoryDetails.add(details);
        }
    }

    // --- NEW: Gets the list of names for the ComboBox ---
    public List<String> getDisplayableProgramNames() {
        if (!programLoaded) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        names.add(program.getName());
        definedFunctions.values().stream()
                .map(FunctionData::userString)
                .sorted()
                .forEach(names::add);
        return names;
    }

    public void setContextProgram(String displayName) {
        if (displayName == null) return;

        if (program != null && program.getName().equals(displayName)) {
            this.contextProgram = this.program;
            return;
        }

        for (FunctionData data : definedFunctions.values()) {
            if (data.userString().equals(displayName)) {
                this.contextProgram = data.program();
                return;
            }
        }
    }

    //to convert the new FunctionData map to the old Program map for legacy method calls
    private Map<String, Program> getProgramMap() {
        Map<String, Program> programMap = new HashMap<>();
        for (Map.Entry<String, FunctionData> entry : definedFunctions.entrySet()) {
            programMap.put(entry.getKey(), entry.getValue().program());
        }
        return programMap;
    }
}