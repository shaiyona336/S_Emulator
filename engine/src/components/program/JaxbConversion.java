package components.program;

import components.instruction.Instruction;
import components.instruction.InstructionFactory;
import components.jaxb.generated.*;
import components.label.LabelFactory;
import components.variable.VariableFactory;
import components.label.Label;
import components.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public class JaxbConversion {

    public static Program SProgramToProgram(SProgram sProgram) {
        Program program = new StandardProgram(sProgram.getName());
        List<Instruction> instructions = SInstructionsToInstructions(sProgram.getSInstructions());
        for (Instruction inst : instructions) {
            program.addInstruction(inst);
        }
        return program;
    }

    public static Program SFunctionToProgram(SFunction sFunction) {
        List<Instruction> instructions = SInstructionsToInstructions(sFunction.getSInstructions());
        return new StandardProgram(sFunction.getName(), instructions);
    }

    private static List<Instruction> SInstructionsToInstructions(SInstructions sInstructions) {
        List<Instruction> instructionList = new ArrayList<>();
        if (sInstructions == null) {
            return instructionList;
        }

        for (SInstruction sInst : sInstructions.getSInstruction()) {
            String instructionName = sInst.getName();
            Variable variable = VariableFactory.createVariableFromString(sInst.getSVariable());
            Label label = LabelFactory.createLabelFromString(sInst.getSLabel());

            SInstructionArguments sArgs = sInst.getSInstructionArguments();

            Instruction instruction = InstructionFactory.createInstruction(instructionName, label, variable, sArgs);
            instructionList.add(instruction);
        }
        return instructionList;
    }
}