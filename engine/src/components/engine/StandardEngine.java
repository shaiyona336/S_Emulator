package components.engine;

import components.executor.Context;
import components.executor.ProgramExecutor;
import components.instruction.Instruction;
import components.instruction.implementations.synthetic.QuoteInstruction;
import components.jaxb.generated.*;
import components.program.JaxbConversion;
import components.program.Program;
import dtos.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import components.architecture.Architecture;
import components.architecture.InstructionGeneration;

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
                    definedFunctions.put(sFunc.getName(), new FunctionData(functionAsProgram, sFunc.getUserString()));
                }
            }
            program = JaxbConversion.SProgramToProgram(sProgram);

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

        List<InstructionDetails> instructionDetailsList = convertInstructions(contextProgram);

        List<LabelDetails> labelDetailsList = new ArrayList<>();
        List<components.label.Label> labels = contextProgram.getLabels(getProgramMap());
        for (components.label.Label label : labels) {
            labelDetailsList.add(new LabelDetails(label.getStringLabel()));
        }

        // Input and work variables are just identifiers - they don't have values until execution
        List<VariableDetails> inputVarDetailsList = new ArrayList<>();
        List<components.variable.Variable> inputVars = contextProgram.getInputVariables(getProgramMap());
        for (components.variable.Variable var : inputVars) {
            inputVarDetailsList.add(new VariableDetails(var.getStringVariable(), 0L));
        }

        List<VariableDetails> workVarDetailsList = new ArrayList<>();
        List<components.variable.Variable> workVars = contextProgram.getWorkVariables(getProgramMap());
        for (components.variable.Variable var : workVars) {
            workVarDetailsList.add(new VariableDetails(var.getStringVariable(), 0L));
        }

        List<BreakpointDetails> breakpointDetailsList = new ArrayList<>();

        return ProgramDetails.createWithArchitectureCounts(
                contextProgram.getName(),
                instructionDetailsList,
                labelDetailsList,
                inputVarDetailsList,
                workVarDetailsList,
                breakpointDetailsList
        );
    }

    @Override
    public ProgramDetails expandProgram(int expansionDegree) {
        Program currentProgram = this.contextProgram;
        for (int i = 0; i < expansionDegree; i++) {
            currentProgram = currentProgram.expand(getProgramMap());
        }

        List<InstructionDetails> instructionDetailsList = convertInstructions(currentProgram);

        List<LabelDetails> labelDetailsList = new ArrayList<>();
        List<components.label.Label> labels = currentProgram.getLabels(getProgramMap());
        for (components.label.Label label : labels) {
            labelDetailsList.add(new LabelDetails(label.getStringLabel()));
        }

        List<VariableDetails> inputVarDetailsList = new ArrayList<>();
        List<components.variable.Variable> inputVars = currentProgram.getInputVariables(getProgramMap());
        for (components.variable.Variable var : inputVars) {
            inputVarDetailsList.add(new VariableDetails(var.getStringVariable(), 0L));
        }

        List<VariableDetails> workVarDetailsList = new ArrayList<>();
        List<components.variable.Variable> workVars = currentProgram.getWorkVariables(getProgramMap());
        for (components.variable.Variable var : workVars) {
            workVarDetailsList.add(new VariableDetails(var.getStringVariable(), 0L));
        }

        List<BreakpointDetails> breakpointDetailsList = new ArrayList<>();

        return ProgramDetails.createWithArchitectureCounts(
                currentProgram.getName(),
                instructionDetailsList,
                labelDetailsList,
                inputVarDetailsList,
                workVarDetailsList,
                breakpointDetailsList
        );
    }

    private List<InstructionDetails> convertInstructions(Program prog) {
        List<InstructionDetails> instructionDetailsList = new ArrayList<>();
        int index = 0;
        int instructionNumber = 1;
        List<Instruction> instructions = prog.getInstructions();
        for (Instruction instruction : instructions) {
            String operand1 = "";
            String operand2 = "";

            // Parse operands from string representation
            try {
                String instStr = instruction.getStringInstruction();
                String[] parts = instStr.split(" ");
                if (parts.length > 1) operand1 = parts[1];
                if (parts.length > 2) operand2 = parts[2];
            } catch (Exception e) {
                // Leave empty if parsing fails
            }

            instructionDetailsList.add(InstructionDetails.fromInstruction(
                    index++,
                    instructionNumber++,
                    instruction.getName(),
                    operand1,
                    operand2
            ));
        }
        return instructionDetailsList;
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

        // Convert Context to VariableDetails
        List<VariableDetails> variableDetailsList = convertContextToVariableDetails(
                programExecutor.getVariablesContext(),
                programToRun
        );

        return new ExecutionDetails(
                getProgramDetailsForProgram(programToRun),
                variableDetailsList,
                programExecutor.getCyclesNumber(),
                y
        );
    }

    private List<VariableDetails> convertContextToVariableDetails(Context context, Program program) {
        List<VariableDetails> variableDetailsList = new ArrayList<>();

        if (context == null) {
            return variableDetailsList;
        }

        try {
            Map<components.variable.Variable, Long> variables = context.getVariables();
            if (variables != null) {
                for (Map.Entry<components.variable.Variable, Long> entry : variables.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        variableDetailsList.add(new VariableDetails(
                                entry.getKey().getStringVariable(),
                                entry.getValue()
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error converting context to variable details: " + e.getMessage());
            e.printStackTrace();
        }

        return variableDetailsList;
    }

    private ProgramDetails getProgramDetailsForProgram(Program prog) {
        List<InstructionDetails> instructionDetailsList = convertInstructions(prog);

        List<LabelDetails> labelDetailsList = new ArrayList<>();
        List<components.label.Label> labels = prog.getLabels(getProgramMap());
        for (components.label.Label label : labels) {
            labelDetailsList.add(new LabelDetails(label.getStringLabel()));
        }

        List<VariableDetails> inputVarDetailsList = new ArrayList<>();
        List<components.variable.Variable> inputVars = prog.getInputVariables(getProgramMap());
        for (components.variable.Variable var : inputVars) {
            inputVarDetailsList.add(new VariableDetails(var.getStringVariable(), 0L));
        }

        List<VariableDetails> workVarDetailsList = new ArrayList<>();
        List<components.variable.Variable> workVars = prog.getWorkVariables(getProgramMap());
        for (components.variable.Variable var : workVars) {
            workVarDetailsList.add(new VariableDetails(var.getStringVariable(), 0L));
        }

        List<BreakpointDetails> breakpointDetailsList = new ArrayList<>();

        return ProgramDetails.createWithArchitectureCounts(
                prog.getName(),
                instructionDetailsList,
                labelDetailsList,
                inputVarDetailsList,
                workVarDetailsList,
                breakpointDetailsList
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

        List<VariableDetails> variableDetailsList = convertContextToVariableDetails(
                this.debugExecutor.getVariablesContext(),
                this.debugProgram
        );

        return new DebugStepDetails(
                variableDetailsList,
                1,
                this.debugExecutor.isFinished(),
                this.debugExecutor.getCyclesNumber(),
                0L
        );
    }

    @Override
    public DebugStepDetails stepOver() {
        if (!isInDebugMode || this.debugExecutor == null) {
            throw new IllegalStateException("Not in a debug session. Cannot step over.");
        }

        int cyclesBefore = this.debugExecutor.getCyclesNumber();
        this.debugExecutor.stepOver();
        int cyclesAfter = this.debugExecutor.getCyclesNumber();
        int cyclesConsumed = cyclesAfter - cyclesBefore;

        List<VariableDetails> variableDetailsList = convertContextToVariableDetails(
                this.debugExecutor.getVariablesContext(),
                this.debugProgram
        );

        // Get Y variable value
        long yValue = 0;
        for (VariableDetails var : variableDetailsList) {
            if (var.getStringVariable().equals("Y")) {
                yValue = var.getValue();
                break;
            }
        }

        return new DebugStepDetails(
                variableDetailsList,
                this.debugExecutor.getNextInstructionNumber(),
                this.debugExecutor.isFinished(),
                cyclesConsumed,
                yValue
        );
    }

    @Override
    public ExecutionDetails resume() {
        if (!isInDebugMode || this.debugExecutor == null) {
            throw new IllegalStateException("Not in a debug session. Cannot resume.");
        }
        Long y = this.debugExecutor.resume();
        runHistoryDetails.add(new RunHistoryDetails(++runNumber, this.debugExpansionDegree, List.of(this.debugExecutor.getInitialInputs()), y, this.debugExecutor.getCyclesNumber()));

        List<VariableDetails> variableDetailsList = convertContextToVariableDetails(
                this.debugExecutor.getVariablesContext(),
                this.debugProgram
        );

        ExecutionDetails finalDetails = new ExecutionDetails(
                getProgramDetailsForProgram(this.debugProgram),
                variableDetailsList,
                this.debugExecutor.getCyclesNumber(),
                y
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

    private Map<String, Program> getProgramMap() {
        Map<String, Program> programMap = new HashMap<>();
        for (Map.Entry<String, FunctionData> entry : definedFunctions.entrySet()) {
            programMap.put(entry.getKey(), entry.getValue().program());
        }
        return programMap;
    }

    @Override
    public boolean validateArchitecture(Architecture architecture) {
        if (contextProgram == null) return false;

        for (components.instruction.Instruction instruction : contextProgram.getInstructions()) {
            if (!architecture.supportsInstruction(instruction.getName())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getArchitectureValidationMessage(Architecture architecture) {
        if (contextProgram == null) return "No program loaded";

        StringBuilder unsupported = new StringBuilder();
        for (components.instruction.Instruction instruction : contextProgram.getInstructions()) {
            if (!architecture.supportsInstruction(instruction.getName())) {
                if (unsupported.length() > 0) unsupported.append(", ");
                unsupported.append(instruction.getName());
            }
        }

        if (unsupported.length() > 0) {
            return "Architecture " + architecture.getDisplayName() +
                    " does not support: " + unsupported.toString();
        }
        return "All instructions supported";
    }

    @Override
    public String getContextProgramName() {
        return contextProgram != null ? contextProgram.getName() : null;
    }
}